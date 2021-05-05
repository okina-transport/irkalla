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

package org.rutebanken.irkalla.routes.etcd;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.rutebanken.irkalla.routes.BaseRouteBuilder;
import org.rutebanken.irkalla.routes.etcd.json.EtcdResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.rutebanken.irkalla.util.Http4URL.toHttp4Url;

/**
 * Get/ set stop place synced until date in etcd. Not using camel-etcd because timeout does not work (hangs indefinitely) with underlying etcd4j lib.
 */
@Component
@ConditionalOnProperty(name = "sync.status.in.memory", havingValue = "false", matchIfMissing = true)
public class EtcdRouteBuilder extends BaseRouteBuilder {

    @Value("${etcd.url}")
    private String etcdUrl;

    @Value("${etcd.sync.status.key:/v2/keys/prod/dynamic/irkalla/stop_place/sync}")
    private String etcdSyncStatusKey;

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXX";

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @Override
    public void configure() throws Exception {

        from("direct:getSyncStatusUntilTime")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
                .doTry()
                .to(toHttp4Url(etcdUrl) + etcdSyncStatusKey)
                .unmarshal().json(JsonLibrary.Jackson, EtcdResponse.class)
                .process(e ->
                                 e.getIn().setBody(Instant.from(FORMATTER.parse(e.getIn().getBody(EtcdResponse.class).node.value)).toEpochMilli()))
                .doCatch(HttpOperationFailedException.class).onWhen(exchange -> {
            HttpOperationFailedException ex = exchange.getException(HttpOperationFailedException.class);
            return (ex.getStatusCode() == 404);
        })
                .log(LoggingLevel.INFO, "No synced until date found in etcd. Using null")
                .setBody(constant(null))
                .end()

                .routeId("get-sync-status-until");

        from("direct:setSyncStatusUntilTime")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
                .process(e -> e.getIn().setBody(Instant.ofEpochMilli(e.getIn().getBody(Long.class)).atZone(ZoneId.of("UTC")).format(FORMATTER)))
                .toD(toHttp4Url(etcdUrl) + etcdSyncStatusKey + "?value=${body}")
                .routeId("set-sync-status-until");
    }
}
