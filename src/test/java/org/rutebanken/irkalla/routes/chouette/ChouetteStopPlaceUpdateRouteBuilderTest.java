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
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.rutebanken.irkalla.routes.RouteBuilderIntegrationTestBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;

import static org.rutebanken.irkalla.util.Http4URL.toHttp4Url;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChouetteStopPlaceUpdateRouteBuilderTest extends RouteBuilderIntegrationTestBase {


    @Produce(uri = "activemq:queue:ChouetteStopPlaceSyncQueue")
    protected ProducerTemplate updateStopPlaces;

    @Value("${chouette.url}")
    private String chouetteUrl;

    @Value("${etcd.url}")
    private String etcdUrl;

    @Value("${tiamat.url}")
    private String tiamatUrl;

    @Value("${tiamat.publication.delivery.path:/services/stop_places/netex/changed_in_period}")
    private String publicationDeliveryPath;

    @EndpointInject(uri = "mock:chouetteUpdateStopPlaces")
    protected MockEndpoint chouetteUpdateStopPlaces;

    @EndpointInject(uri = "mock:tiamatExportChanges")
    protected MockEndpoint tiamatExportChanges;

    @EndpointInject(uri = "mock:etcd")
    protected MockEndpoint etcd;

    @EndpointInject(uri = "mock:chouetteStopPlaceSyncQueue")
    protected MockEndpoint chouetteStopPlaceSyncQueueMock;

    @Test
    @Ignore
    public void testUpdateStopPlaces() throws Exception {
        String exportPath = toHttp4Url(tiamatUrl) + publicationDeliveryPath + "*";

        context.getRouteDefinition("tiamat-get-batch-of-changed-stop-places-as-netex").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(exportPath)
                        .skipSendToOriginalEndpoint().to("mock:tiamatExportChanges");
            }
        });

        context.getRouteDefinition("chouette-synchronize-stop-place-batch").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(toHttp4Url(chouetteUrl) + "/chouette_iev/stop_place/*")
                        .skipSendToOriginalEndpoint().to("mock:chouetteUpdateStopPlaces");
            }
        });


        context.getRouteDefinition("chouette-synchronize-stop-places-init").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:getSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd");
            }
        });

        context.getRouteDefinition("chouette-synchronize-stop-places-complete").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:setSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd");
            }
        });


        context.start();
        tiamatExportChanges.expectedMessageCount(2);

        // Two batches waiting
        tiamatExportChanges.whenExchangeReceived(1, e -> {
            e.getIn().setHeader("Link", exportPath);
            e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200");
        });
        tiamatExportChanges.whenExchangeReceived(2, e -> e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200"));


        chouetteUpdateStopPlaces.expectedMessageCount(2);

        updateStopPlaces.sendBody(null);

        tiamatExportChanges.assertIsSatisfied();
        chouetteUpdateStopPlaces.assertIsSatisfied();
    }


    @Test
    public void testUpdateStopPlacesNoChanges() throws Exception {
        String exportPath = toHttp4Url(tiamatUrl) + publicationDeliveryPath + "*";

        context.getRouteDefinition("tiamat-get-batch-of-changed-stop-places-as-netex").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(exportPath)
                        .skipSendToOriginalEndpoint().to("mock:tiamatExportChanges");
            }
        });

        context.getRouteDefinition("chouette-synchronize-stop-places-init").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:getSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd");
            }
        });

        context.getRouteDefinition("chouette-synchronize-stop-places-complete").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:setSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd");
            }
        });
        context.start();

        etcd.expectedMessageCount(2);
        tiamatExportChanges.expectedMessageCount(1);
        tiamatExportChanges.whenExchangeReceived(1, e -> e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "204"));


        updateStopPlaces.sendBody(null);

        etcd.assertIsSatisfied();
        tiamatExportChanges.assertIsSatisfied();
    }

    @Test
    @Ignore
    public void testUpdateStopPlacesRetryWhenChouetteIsBusy() throws Exception {
        String exportPath = toHttp4Url(tiamatUrl) + publicationDeliveryPath + "*";

        context.getRouteDefinition("tiamat-get-batch-of-changed-stop-places-as-netex").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(exportPath)
                        .skipSendToOriginalEndpoint().to("mock:tiamatExportChanges");
            }
        });

        context.getRouteDefinition("chouette-synchronize-stop-place-batch").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(toHttp4Url(chouetteUrl) + "/chouette_iev/stop_place/*")
                        .skipSendToOriginalEndpoint().to("mock:chouetteUpdateStopPlaces");
                interceptSendToEndpoint("activemq:queue:ChouetteStopPlaceSyncQueue")
                        .skipSendToOriginalEndpoint().to("mock:chouetteStopPlaceSyncQueue");
            }
        });

        context.getRouteDefinition("chouette-synchronize-stop-places-init").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:getSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd");
            }
        });

        context.start();
        tiamatExportChanges.expectedMessageCount(1);
        chouetteUpdateStopPlaces.expectedMessageCount(1);
        chouetteStopPlaceSyncQueueMock.expectedMessageCount(1);

        // One batch waiting
        tiamatExportChanges.whenExchangeReceived(1, e -> {
            e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200");
        });

        // Chouette is busy, returning 423 - "locked"
        chouetteUpdateStopPlaces.whenExchangeReceived(1, e -> {
            throw new HttpOperationFailedException(null, 423, null, null, null, null);
        });

        updateStopPlaces.sendBody(null);

        tiamatExportChanges.assertIsSatisfied();
        chouetteUpdateStopPlaces.assertIsSatisfied();
        chouetteStopPlaceSyncQueueMock.assertIsSatisfied();

        Assert.assertNotNull(chouetteStopPlaceSyncQueueMock.getExchanges().get(0).getIn().getHeader(ScheduledMessage.AMQ_SCHEDULED_DELAY));
    }
}
