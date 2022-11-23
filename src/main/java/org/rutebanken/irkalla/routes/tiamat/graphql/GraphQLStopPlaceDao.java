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

package org.rutebanken.irkalla.routes.tiamat.graphql;

import org.rutebanken.irkalla.domain.CrudAction;
import org.rutebanken.irkalla.routes.tiamat.StopPlaceChange;
import org.rutebanken.irkalla.routes.tiamat.StopPlaceDao;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.StopPlace;
import org.rutebanken.irkalla.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.rutebanken.irkalla.Constants.ET_CLIENT_ID_HEADER;
import static org.rutebanken.irkalla.Constants.ET_CLIENT_NAME_HEADER;


@Service("stopPlaceDao")
public class GraphQLStopPlaceDao implements StopPlaceDao {

    @Value("${http.client.name:irkalla}")
    private String clientName;

    @Value("${HOSTNAME:irkalla}")
    private String clientId;

    @Value("${tiamat.url}")
    private String tiamatUrl;

    @Value("${tiamat.graphql.path:/services/stop_places/graphql}")
    private String tiamatGraphQLPath;

    @Autowired
    private TokenService tokenService;

    @Override
    public StopPlaceChange getStopPlaceChange(CrudAction crudAction, String id, Long version) {
        RestTemplate restTemplate = new RestTemplate();
        StopPlaceResponse rsp =
                restTemplate.exchange(tiamatUrl + tiamatGraphQLPath, HttpMethod.POST, createQueryHttpEntity(id, version), StopPlaceResponse.class).getBody();

        return toStopPlaceChange(crudAction, id, version, rsp);
    }

    private StopPlaceChange toStopPlaceChange(CrudAction crudAction, String id, Long version, StopPlaceResponse rsp) {
        StopPlace current = rsp.getCurrent();

        if (current == null || !id.equals(current.id)) {
            return null;
        }

        // Tiamat returns version 1 if queried for v 0. Verify that version is actually previous
        StopPlace previous = null;
        if (rsp.getPreviousVersion() != null && id.equals(rsp.getPreviousVersion().id) && rsp.getPreviousVersion().version == (version - 1)) {
            previous = rsp.getPreviousVersion();
        }

        return new StopPlaceChange(crudAction, current, previous);
    }

    private HttpEntity<String> createQueryHttpEntity(String id, Long version) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set(ET_CLIENT_NAME_HEADER, clientName);
        headers.set(ET_CLIENT_ID_HEADER, clientId);
        headers.set("Authorization", "Bearer " + tokenService.getToken());
        return new HttpEntity<>(new StopPlaceQuery(id, version).toString(), headers);
    }


}
