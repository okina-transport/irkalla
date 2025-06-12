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

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Ignore;
import org.junit.Test;
import org.rutebanken.irkalla.Constants;
import org.rutebanken.irkalla.routes.RouteBuilderIntegrationTestBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;

import static org.rutebanken.irkalla.util.Http4URL.toHttp4Url;

@Ignore
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChouetteStopPlaceDeleteRouteBuilderTest extends RouteBuilderIntegrationTestBase {


    @Produce(uri = "activemq:queue:ChouetteStopPlaceDeleteQueue")
    protected ProducerTemplate deleteStopPlaces;

    @Value("${chouette.url}")
    private String chouetteUrl;

    @EndpointInject(uri = "mock:chouetteDeleteStopPlace")
    protected MockEndpoint chouetteDeleteStopPlace;

    @Test
    public void testDeleteStopPlace() throws Exception {

        String stopPlaceId = "NSR:StopPlace:33";

        context.getRouteDefinition("chouette-delete-stop-place").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(toHttp4Url(chouetteUrl) + "/chouette_iev/stop_place/NSR:StopPlace:33")
                        .skipSendToOriginalEndpoint().to("mock:chouetteDeleteStopPlace");
            }
        });


        context.start();
        chouetteDeleteStopPlace.expectedMessageCount(1);

        deleteStopPlaces.sendBodyAndHeader(null, Constants.HEADER_ENTITY_ID, stopPlaceId);

        chouetteDeleteStopPlace.assertIsSatisfied();
    }
}
