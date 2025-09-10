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


import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;

import org.apache.camel.component.mock.MockEndpoint;

import org.apache.camel.http.base.HttpOperationFailedException;


import org.apache.camel.support.DefaultMessage;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.Ignore;

import org.junit.jupiter.api.Test;
import org.rutebanken.irkalla.routes.RouteBuilderIntegrationTestBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;


import java.util.HashMap;
import java.util.Map;

import static org.rutebanken.irkalla.Constants.*;


@SpringBootTest
@CamelSpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChouetteStopPlaceUpdateRouteBuilderTest extends RouteBuilderIntegrationTestBase {


    @Produce(value = "activemq:queue:ChouetteStopPlaceSyncQueue")
    protected ProducerTemplate updateStopPlaces;

    @Value("${chouette.url}")
    private String chouetteUrl;

    @Value("${etcd.url}")
    private String etcdUrl;

    @Value("${tiamat.url}")
    private String tiamatUrl;

    @Value("${tiamat.publication.delivery.path:/services/stop_places/netex/changed_in_period}")
    private String publicationDeliveryPath;

    @EndpointInject(value = "mock:chouetteUpdateStopPlaces")
    protected MockEndpoint chouetteUpdateStopPlaces;

    @EndpointInject(value = "mock:tiamatExportChanges")
    protected MockEndpoint tiamatExportChanges;

    @EndpointInject(value = "mock:etcd")
    protected MockEndpoint etcd;

    @EndpointInject(value = "mock:chouetteStopPlaceSyncQueue")
    protected MockEndpoint chouetteStopPlaceSyncQueueMock;

    @Test
    @Ignore
    public void testUpdateStopPlaces() throws Exception {
        String exportPath = tiamatUrl + publicationDeliveryPath + "*";

        AdviceWith.adviceWith(context, "tiamat-get-batch-of-changed-stop-places-as-netex",
                a -> a.interceptSendToEndpoint(exportPath)
                        .skipSendToOriginalEndpoint().to("mock:tiamatExportChanges"));

        AdviceWith.adviceWith(context,"chouette-synchronize-stop-place-batch", a -> a.weaveByToUri(chouetteUrl + "/chouette_iev/stop_place*")
                .replace().to("mock:chouetteUpdateStopPlaces"));

        AdviceWith.adviceWith(context,"chouette-synchronize-stop-places-init",
                a -> a.interceptSendToEndpoint("direct:getSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd"));

        AdviceWith.adviceWith(context,"chouette-synchronize-stop-places-complete",
                a -> a.interceptSendToEndpoint("direct:setSyncStatusUntilTime")
                        .skipSendToOriginalEndpoint().to("mock:etcd"));


        context.start();
        tiamatExportChanges.expectedMessageCount(2);

        // Two batches waiting

        tiamatExportChanges.whenExchangeReceived(1, e -> {
            e.getIn().setHeader("Link", exportPath);
            e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200");
        });
        tiamatExportChanges.whenExchangeReceived(2, e -> e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200"));


        chouetteUpdateStopPlaces.expectedMessageCount(2);

        Message msg = new DefaultMessage(context) {
            private Map<String,Object> headers = new HashMap<>();

            public void setHeader(String key, Object value) {
                headers.put(key, value);
            }

            public Object getHeader(String key) {
                return headers.get(key);
            }

        };



        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_NEXT_BATCH_URL, exportPath);
        headers.put(HEADER_SYNC_OPERATION, tiamatUrl + publicationDeliveryPath + "/test");

        updateStopPlaces.sendBodyAndHeaders(msg,headers);

        tiamatExportChanges.assertIsSatisfied();
        chouetteUpdateStopPlaces.assertIsSatisfied();
    }


    @Test
    public void testUpdateStopPlacesNoChanges() throws Exception {
        String exportPath = tiamatUrl + publicationDeliveryPath + "*";

        context.start();

        AdviceWith.adviceWith(context, "tiamat-get-batch-of-changed-stop-places-as-netex",
                a -> a.interceptSendToEndpoint(exportPath)
                        .skipSendToOriginalEndpoint().to("mock:tiamatExportChanges"));




        tiamatExportChanges.expectedMessageCount(1);
        tiamatExportChanges.whenExchangeReceived(1, e -> e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "204"));


        Message msg = new DefaultMessage(context) {
            private Map<String,Object> headers = new HashMap<>();

            public void setHeader(String key, Object value) {
                headers.put(key, value);
            }

            public Object getHeader(String key) {
                return headers.get(key);
            }

        };



        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_NEXT_BATCH_URL, exportPath);
        headers.put(HEADER_SYNC_OPERATION, tiamatUrl + publicationDeliveryPath + "/test");

        updateStopPlaces.sendBodyAndHeaders(msg,headers);


        tiamatExportChanges.assertIsSatisfied();
    }

    @Test
    public void testUpdateStopPlacesRetryWhenChouetteIsBusy() throws Exception {
        String exportPath = tiamatUrl + publicationDeliveryPath + "*";

        AdviceWith.adviceWith(context, "tiamat-get-batch-of-changed-stop-places-as-netex",
                a -> a.interceptSendToEndpoint(exportPath).skipSendToOriginalEndpoint().to("mock:tiamatExportChanges"));


        AdviceWith.adviceWith(context, "chouette-synchronize-stop-place-batch",
                a -> {
                    a.weaveByToUri(chouetteUrl + "/chouette_iev/stop_place*")
                            .replace().to("mock:chouetteUpdateStopPlaces");

                    a.weaveByToUri("activemq:queue:ChouetteStopPlaceSyncQueue").replace().to("mock:chouetteStopPlaceSyncQueue");
                }
        );


        tiamatExportChanges.expectedMessageCount(1);
        chouetteUpdateStopPlaces.expectedMessageCount(1);
        chouetteStopPlaceSyncQueueMock.expectedMessageCount(1);

        // One batch waiting
        tiamatExportChanges.whenExchangeReceived(1, e -> e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "200"));

        // Chouette is busy, returning 423 - "locked"
        chouetteUpdateStopPlaces.whenExchangeReceived(1, e -> {
            throw new HttpOperationFailedException(null, 423, null, null, null, null);
        });

        context.start();


        Message msg = new DefaultMessage(context) {
            private Map<String,Object> headers = new HashMap<>();

            public void setHeader(String key, Object value) {
                headers.put(key, value);
            }

            public Object getHeader(String key) {
                return headers.get(key);
            }

        };



        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_NEXT_BATCH_URL, exportPath);
        headers.put(HEADER_SYNC_OPERATION, tiamatUrl + publicationDeliveryPath + "/test");

        updateStopPlaces.sendBodyAndHeaders(msg,headers);

        tiamatExportChanges.assertIsSatisfied();
        chouetteUpdateStopPlaces.assertIsSatisfied();
        chouetteStopPlaceSyncQueueMock.assertIsSatisfied(20000);
    }
}
