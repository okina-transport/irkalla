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
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.http.common.HttpMethods;
import org.rutebanken.irkalla.Constants;
import org.rutebanken.irkalla.routes.BaseRouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class ChouetteStopPlaceDeleteRouteBuilder extends BaseRouteBuilder {
    @Value("${chouette.url}")
    private String chouetteUrl;

    @Value("${chouette.sync.stop.place.retry.delay:15000}")
    private int retryDelay;

    @Override
    public void configure() throws Exception {
        super.configure();

        singletonFrom("activemq:queue:ChouetteStopPlaceDeleteQueue?transacted=true")
                .transacted()

                .log(LoggingLevel.INFO, "Delete stop place ${header." + Constants.HEADER_ENTITY_ID + "} in Chouette")
                .setBody(constant(null))
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .doTry()
                .toD(chouetteUrl + "/chouette_iev/stop_place/${header." + Constants.HEADER_ENTITY_ID + "}")
                .log(LoggingLevel.INFO, "Finished deleting stop place ${header." + Constants.HEADER_ENTITY_ID + "} in Chouette")

                .doCatch(HttpOperationFailedException.class).onWhen(exchange -> {
                    HttpOperationFailedException ex = exchange.getException(HttpOperationFailedException.class);
                     return (ex.getStatusCode() == 423);
                })
                .log(LoggingLevel.INFO, "Unable to delete stop place because Chouette is busy, retry in " + retryDelay + " ms")
                .setHeader(ScheduledMessage.AMQ_SCHEDULED_DELAY, constant(retryDelay))
                .setBody(constant(null))
                .to("activemq:queue:ChouetteStopPlaceSyncQueue")
                .end()
                .routeId("chouette-delete-stop-place");


    }
}
