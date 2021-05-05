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

package org.rutebanken.irkalla.routes.tiamat.mapper;

import org.rutebanken.irkalla.domain.CrudEvent;
import org.rutebanken.irkalla.routes.tiamat.StopPlaceChange;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.GraphqlGeometry;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.StopPlace;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Geometry;
import org.wololo.geojson.Point;

import java.util.ArrayList;
import java.util.List;

@Component
public class StopPlaceChangedToEvent {

    public CrudEvent toEvent(StopPlaceChange stopPlaceChange) {
        StopPlace currentVersion = stopPlaceChange.getCurrent();

        CrudEvent.Builder event = CrudEvent.builder()
                                          .type(CrudEvent.EntityType.StopPlace)
                                          .entityClassifier(stopPlaceChange.getEntityClassifier())
                                          .action(CrudEvent.Action.valueOf(stopPlaceChange.getCrudAction().name()))
                                          .changeType(stopPlaceChange.getUpdateType())
                                          .oldValue(stopPlaceChange.getOldValue())
                                          .newValue(stopPlaceChange.getNewValue())
                                          .externalId(currentVersion.id)
                                          .version(currentVersion.version)
                                          .name(currentVersion.getNameAsString())
                                          .comment(currentVersion.versionComment)
                                          .username(currentVersion.changedBy)
                                          .geometry(toGeometry(currentVersion.geometry))
                                          .location(stopPlaceChange.getLocation())
                                          .eventTime(stopPlaceChange.getChangeTime());

        return event.build();
    }


    private Geometry toGeometry(GraphqlGeometry graphqlGeometry) {

        if (graphqlGeometry != null && "Point".equals(graphqlGeometry.type)) {
            List<Double> coordinates = new ArrayList<>();
            graphqlGeometry.coordinates.forEach(coordList -> coordinates.addAll(coordList));

            return new Point(coordinates.stream().mapToDouble(c -> c).toArray());
        }
        return null;
    }

}
