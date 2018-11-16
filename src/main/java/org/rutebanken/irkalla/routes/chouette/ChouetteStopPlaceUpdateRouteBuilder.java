/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.irkalla.routes.chouette;

import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.rutebanken.irkalla.Constants;
import org.rutebanken.irkalla.IrkallaException;
import org.rutebanken.irkalla.routes.BaseRouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.apache.camel.management.mbean.Statistic.UpdateMode.DELTA;
import static org.rutebanken.irkalla.Constants.*;
import static org.rutebanken.irkalla.util.Http4URL.toHttp4Url;

@Component
public class ChouetteStopPlaceUpdateRouteBuilder extends BaseRouteBuilder {
    @Value("${chouette.url}")
    private String chouetteUrl;


    @Value("${chouette.sync.stop.place.cron:0 0/5 * * * ?}")
    private String deltaSyncCronSchedule;

    @Value("${chouette.sync.stop.place.full.cron:0 0 2 * * ?}")
    private String fullSyncCronSchedule;


    @Value("${chouette.sync.stop.place.retry.delay:15000}")
    private int retryDelay;

    @Value("${chouette.sync.stop.place.grace.ms:30000}")
    private int graceMilliseconds;


    @Override
    public void configure() throws Exception {
        super.configure();

        from("quartz2://irkalla/stopPlaceDeltaSync?cron=" + deltaSyncCronSchedule + "&trigger.timeZone=Europe/Oslo")
                .autoStartup("{{chouette.sync.stop.place.autoStartup:true}}")
                .log(LoggingLevel.DEBUG, "Quartz triggers delta sync of changed stop places.")
                .setHeader(HEADER_SYNC_OPERATION, constant(DELTA))
                .inOnly("activemq:queue:ChouetteStopPlaceSyncQueue")
                .routeId("chouette-synchronize-stop-places-delta-quartz");

        from("quartz2://irkalla/stopPlaceSync?cron=" + fullSyncCronSchedule + "&trigger.timeZone=Europe/Oslo")
                .autoStartup("{{chouette.sync.stop.place.autoStartup:true}}")
                .log(LoggingLevel.DEBUG, "Quartz triggers full sync of changed stop places.")
                .setHeader(HEADER_SYNC_OPERATION, constant(SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST))
                .inOnly("activemq:queue:ChouetteStopPlaceSyncQueue")
                .routeId("chouette-synchronize-stop-places-full-quartz");

        singletonFrom("activemq:queue:ChouetteStopPlaceSyncQueue?transacted=true&messageListenerContainerFactoryRef=batchListenerContainerFactory")
                .transacted()
                .process(e -> mergeActiveMQMessages(e))
                .choice()
                .when(simple("${header." + HEADER_SYNC_OPERATION + "} == '" + SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST + "'"))
                .to("direct:deleteUnusedStopPlaces")
                .otherwise()
                .to("direct:synchronizeStopPlaces")
                .end()
                .routeId("chouette-synchronize-stop-places-control-route");


        from("direct:synchronizeStopPlaces")
                .setHeader(Constants.HEADER_PROCESS_TARGET, constant("direct:synchronizeStopPlaceBatch"))
                .choice()
                .when(header(HEADER_NEXT_BATCH_URL).isNull()) // New sync, init
                .to("direct:initNewSynchronization")
                .otherwise()
                .log(LoggingLevel.INFO, "${header." + HEADER_SYNC_OPERATION + "} synchronization of stop places in Chouette resumed.")
                .end()

                .setBody(constant(null))
                .to("direct:processChangedStopPlacesAsNetex")
                .choice()
                .when(header(HEADER_NEXT_BATCH_URL).isNotNull())
                .to("activemq:ChouetteStopPlaceSyncQueue")  // Prepare new iteration
                .otherwise()
                .to("direct:completeSynchronization") // Completed

                .end()

                .routeId("chouette-synchronize-stop-places");


        from("direct:initNewSynchronization")
                .log(LoggingLevel.INFO, "${header." + HEADER_SYNC_OPERATION + "} synchronization of stop places in Chouette started.")
                .choice()
                .when(simple("${header." + HEADER_SYNC_OPERATION + "} == '" + SYNC_OPERATION_DELTA + "'"))
                .setBody(constant(null))
                .to("direct:getSyncStatusUntilTime")
                .setHeader(Constants.HEADER_SYNC_STATUS_FROM, simple("${body}"))
                .end()

                .process(e -> e.getIn().setHeader(Constants.HEADER_SYNC_STATUS_TO, Instant.now().toEpochMilli()))
                .routeId("chouette-synchronize-stop-places-init");

        from("direct:completeSynchronization")
                .choice()
                .when(header(Constants.HEADER_SYNC_STATUS_TO).isNotNull())
                // Adjust sync status back in time to be sure to catch any historic changes not yet committed in stop place registry
                .process(e -> e.getIn().setBody(e.getIn().getHeader(HEADER_SYNC_STATUS_TO, Instant.class).minusMillis(graceMilliseconds)))
                .to("direct:setSyncStatusUntilTime")
                .log(LoggingLevel.INFO, "${header." + HEADER_SYNC_OPERATION + "} synchronization of stop places in Chouette completed.")
                .otherwise()
                .log(LoggingLevel.INFO, "${header." + HEADER_SYNC_OPERATION + "} synchronization of stop places in Chouette completed, unable to update etcd.")
                .end()

                .routeId("chouette-synchronize-stop-places-complete");


        from("direct:deleteUnusedStopPlaces")
                .log(LoggingLevel.INFO, "Full synchronization of stop places in Chouette, deleting unused stops first")
                .removeHeaders("CamelHttp*")
                .setBody(constant(null))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .doTry()
                .toD(toHttp4Url(chouetteUrl) + "/chouette_iev/stop_place/unused")
                .setHeader(HEADER_SYNC_OPERATION, constant(SYNC_OPERATION_FULL))
                .log(LoggingLevel.INFO, "Deleting unused stop places in Chouette completed.")
                .to("activemq:queue:ChouetteStopPlaceSyncQueue")
                .doCatch(HttpOperationFailedException.class).onWhen(exchange -> {
            HttpOperationFailedException ex = exchange.getException(HttpOperationFailedException.class);
            return (ex.getStatusCode() == 423);
        })
                .log(LoggingLevel.INFO, "Unable to delete unused stop places because Chouette is busy, retry in " + retryDelay + " ms")
                .setHeader(ScheduledMessage.AMQ_SCHEDULED_DELAY, constant(retryDelay))
                .setBody(constant(null))
                .to("activemq:queue:ChouetteStopPlaceSyncQueue")
                .stop()
                .routeId("chouette-synchronize-stop-places-delete-unused");


        from("direct:synchronizeStopPlaceBatch")
                .convertBodyTo(String.class)
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .doTry()
                .toD(toHttp4Url(chouetteUrl) + "/chouette_iev/stop_place")
                .doCatch(HttpOperationFailedException.class).onWhen(exchange -> {
            HttpOperationFailedException ex = exchange.getException(HttpOperationFailedException.class);
            return (ex.getStatusCode() == 423);
        })
                .log(LoggingLevel.INFO, "Unable to sync stop places because Chouette is busy, retry in " + retryDelay + " ms")
                .setHeader(ScheduledMessage.AMQ_SCHEDULED_DELAY, constant(retryDelay))
                .setBody(constant(null))
                .to("activemq:queue:ChouetteStopPlaceSyncQueue")
                .stop()
                .routeId("chouette-synchronize-stop-place-batch");

    }

    /**
     * Merge status from all msg read in batch into current exchange.
     * <p>
     * Priority:
     * - Delete and start new, full sync if at least one message signals that
     * - Full sync, if no delete and at least one message signals that, using first msg with url set (indicating ongoing job) if any
     * - Delta sync in other cases, using first msg with url set (indicating ongoing job) if any
     */
    private void mergeActiveMQMessages(Exchange e) {
        List<ActiveMQMessage> msgList = e.getIn().getBody(List.class);
        Collections.sort(msgList, new SyncMsgComparator());

        ActiveMQMessage topPriMsg = msgList.get(0);
        try {
            Object syncOperation = topPriMsg.getProperty(HEADER_SYNC_OPERATION);
            if (syncOperation == null) {
                e.getIn().setHeader(HEADER_SYNC_OPERATION, SYNC_OPERATION_DELTA);
            } else {
                e.getIn().setHeader(HEADER_SYNC_OPERATION, syncOperation);
                e.getIn().setHeader(HEADER_SYNC_STATUS_TO, topPriMsg.getProperty(HEADER_SYNC_STATUS_TO));
                e.getIn().setHeader(HEADER_NEXT_BATCH_URL, topPriMsg.getProperty(HEADER_NEXT_BATCH_URL));
            }
        } catch (IOException ioE) {
            throw new IrkallaException("Unable to get sync operation header as property from ActiveMQMessage: " + ioE.getMessage(), ioE);
        }
    }

}
