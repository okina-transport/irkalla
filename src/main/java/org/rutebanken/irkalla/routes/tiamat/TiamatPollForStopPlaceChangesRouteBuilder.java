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

package org.rutebanken.irkalla.routes.tiamat;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.http.client.utils.URIBuilder;
import org.rutebanken.irkalla.Constants;
import org.rutebanken.irkalla.routes.BaseRouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.rutebanken.irkalla.Constants.HEADER_NEXT_BATCH_URL;
import static org.rutebanken.irkalla.util.Http4URL.toHttp4Url;

@Component
public class TiamatPollForStopPlaceChangesRouteBuilder extends BaseRouteBuilder {

    @Value("${HOSTNAME:irkalla}")
    private String clientId;

    @Value("${tiamat.url}")
    private String tiamatUrl;

    @Value("${tiamat.publication.delivery.path:/services/stop_places/netex/changed_in_period}")
    private String publicationDeliveryPath;

    @Value("${sync.stop.place.batch.size:1000}")
    private int batchSize;


    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXX";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private static final ZoneId TIME_ZONE_ID = ZoneId.of("UTC");

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:processChangedStopPlacesAsNetex")
                .choice()
                .when(header(HEADER_NEXT_BATCH_URL).isNull())
                .process(e -> setPollForChangesURL(e))
                .end()
                .to("direct:processBatchOfChangedStopPlacesAsNetex")
                .routeId("tiamat-get-changed-stop-places-as-netex");

        from("direct:processBatchOfChangedStopPlacesAsNetex")
                .log(LoggingLevel.INFO, "Fetching batch of changed stop places: ${header." + HEADER_NEXT_BATCH_URL + "}")
                .removeHeader("Link")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
                .setBody(constant(null))
                .toD("${header." + HEADER_NEXT_BATCH_URL + "}")
                .removeHeader(HEADER_NEXT_BATCH_URL)
                .choice()
                .when(simple("${header." + Exchange.HTTP_RESPONSE_CODE + "} == 200"))
                .toD("${header." + Constants.HEADER_PROCESS_TARGET + "}")
                .choice()
                .when(simple("${header.Link}"))
                .process(e -> setURLToNextBatch(e))
                .end()
                .routeId("tiamat-get-batch-of-changed-stop-places-as-netex");

    }

    private void setPollForChangesURL(Exchange e) throws URISyntaxException {
        Long fromAsEpocMillis = e.getIn().getHeader(Constants.HEADER_SYNC_STATUS_FROM, Long.class);
        Long toAsEpocMillis = e.getIn().getHeader(Constants.HEADER_SYNC_STATUS_TO, Long.class);

        URIBuilder uriBuilder = new URIBuilder(toHttp4Url(tiamatUrl) + publicationDeliveryPath);

        uriBuilder.addParameter("topographicPlaceExportMode", "NONE");
        uriBuilder.addParameter("tariffZoneExportMode", "NONE");

        if (fromAsEpocMillis != null) {
            Instant from = Instant.ofEpochMilli(fromAsEpocMillis);
            uriBuilder.addParameter("from", from.atZone(TIME_ZONE_ID).format(FORMATTER));
        }
        if (toAsEpocMillis != null) {
            Instant to = Instant.ofEpochMilli(toAsEpocMillis);
            uriBuilder.addParameter("to", to.atZone(TIME_ZONE_ID).format(FORMATTER));
        }
        if (batchSize > 0) {
            uriBuilder.addParameter("per_page", String.valueOf(batchSize));
        }

        e.getIn().setHeader(HEADER_NEXT_BATCH_URL, uriBuilder.toString());
    }


    /**
     * URL to next page of result set is encoded as Link header (rel="next")
     */
    private void setURLToNextBatch(Exchange e) {
        e.getIn().setHeader(HEADER_NEXT_BATCH_URL, toHttp4Url(e.getIn().getHeader("Link", String.class)
                .replaceFirst("\\<", "")
                .replaceFirst("\\>; rel=\"next\"", "")));
    }

}
