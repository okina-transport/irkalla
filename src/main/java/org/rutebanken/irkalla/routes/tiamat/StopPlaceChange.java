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

import com.google.common.base.Joiner;
import org.rutebanken.irkalla.domain.CrudAction;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.GraphqlGeometry;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.StopPlace;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.TopographicPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Representation of a change for a stop place.
 */
public class StopPlaceChange {
    public enum StopPlaceUpdateType {NAME, COORDINATES, TYPE, NEW_QUAY, REMOVED_QUAY, MINOR, MAJOR}

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceChange.class);

    public static final String MULTI_MODAL_TYPE = "multiModal";

    public static final String PARENT_STOP_PLACE_TYPE = "ParentStopPlace";

    private CrudAction crudAction;

    private StopPlaceUpdateType updateType;

    private StopPlace current;

    private StopPlace previousVersion;

    private List<String> oldValue = new ArrayList<>();

    private List<String> newValue = new ArrayList<>();

    public StopPlaceChange(CrudAction crudAction, StopPlace current, StopPlace previousVersion) {
        this.crudAction = crudAction;
        this.current = current;
        this.previousVersion = previousVersion;
        detectUpdateType();
    }


    public Instant getChangeTime() {
        Instant changeTime = null;
        if (current.validBetween != null) {
            if (CrudAction.REMOVE.equals(crudAction)) {
                changeTime = current.validBetween.toDate;
            } else {
                changeTime = current.validBetween.fromDate;
            }
        }

        if (changeTime == null) {
            return Instant.now();
        }
        return changeTime;
    }


    public CrudAction getCrudAction() {
        return crudAction;
    }

    public StopPlace getCurrent() {
        return current;
    }

    public StopPlace getPreviousVersion() {
        return previousVersion;
    }

    public String getEntityClassifier() {
        if (current.stopPlaceType != null) {
            return current.stopPlaceType;
        }
        if (PARENT_STOP_PLACE_TYPE.equals(current.__typename)) {
            return MULTI_MODAL_TYPE;
        }
        return null;
    }

    public StopPlaceUpdateType getUpdateType() {
        return updateType;
    }

    public String getOldValue() {
        return oldValue.isEmpty() ? null : Joiner.on("\n").join(oldValue);
    }

    public String getNewValue() {
        return newValue.isEmpty() ? null : Joiner.on("\n").join(newValue);
    }

    public String getLocation() {

        List<String> locationNames = new ArrayList<>();
        TopographicPlace topographicPlace = current.topographicPlace;

        while ((topographicPlace != null)) {
            if (topographicPlace.name != null && topographicPlace.name.value != null) {
                locationNames.add(topographicPlace.name.value);
            }
            topographicPlace = topographicPlace.parentTopographicPlace;
        }

        // Present top to bottom
        Collections.reverse(locationNames);
        if (locationNames != null) {
            return Joiner.on(", ").join(locationNames);
        }

        return null;
    }

    private void detectUpdateType() {
        if (!CrudAction.UPDATE.equals(crudAction)) {
            return;
        }

        // Defaulting to minor if no substantial changes are found
        updateType = StopPlaceUpdateType.MINOR;

        if (current == null || previousVersion == null) {
            logger.warn("Unable to detect update type for unknown current and/or previous version of stops. Current: " + current + ", previous: " + previousVersion);
            return;
        }


        checkForChanges(current.getNameAsString(), previousVersion.getNameAsString(), StopPlaceUpdateType.NAME);
        checkForChanges(current.stopPlaceType, previousVersion.stopPlaceType, StopPlaceUpdateType.TYPE);

        // TODO do we need to verify magnitude of coord change? Seems to be small changes due to rounding.
        checkForChanges(current.geometry, previousVersion.geometry, StopPlaceUpdateType.COORDINATES);

        List<String> currentQuayIds = current.safeGetQuays().stream().map(q -> q.id).collect(Collectors.toList());
        List<String> previousVersionQuayIds = previousVersion.safeGetQuays().stream().map(q -> q.id).collect(Collectors.toList());


        List<String> newQuays = currentQuayIds.stream().filter(q -> !previousVersionQuayIds.contains(q)).collect(Collectors.toList());
        List<String> removedQuays = previousVersionQuayIds.stream().filter(q -> !currentQuayIds.contains(q)).collect(Collectors.toList());

        if (!newQuays.isEmpty()) {
            registerUpdate(StopPlaceUpdateType.NEW_QUAY);
            newValue.add(newQuays.toString());
            oldValue.add(removedQuays.toString());
        } else if (!removedQuays.isEmpty()) {
            registerUpdate(StopPlaceUpdateType.REMOVED_QUAY);
            oldValue.add(removedQuays.toString());
        }

    }

    private void checkForChanges(Object curr, Object pv, StopPlaceUpdateType updateType) {
        if (!Objects.equals(curr, pv)) {
            registerUpdate(updateType);
            oldValue.add(formatValue(pv));
            newValue.add(formatValue(curr));
        }
    }


    private String formatValue(Object value) {
        if (value instanceof GraphqlGeometry) {
            return formatGeometry((GraphqlGeometry) value);
        }
        return ObjectUtils.nullSafeToString(value);
    }

    private String formatGeometry(GraphqlGeometry geometry) {
        if ("Point".equals(geometry.type)) {
            if (!CollectionUtils.isEmpty(geometry.coordinates)) {
                List<Double> coordinates = geometry.coordinates.get(0);
                if (coordinates != null && coordinates.size() > 1) {
                    Double x = coordinates.get(0);
                    Double y = coordinates.get(1);
                    return "(" + x + "," + y + ")";
                }
            }
        }
        return null;
    }

    private void registerUpdate(StopPlaceUpdateType updateType) {
        if (StopPlaceUpdateType.MINOR.equals(this.updateType)) {
            this.updateType = updateType;
        } else {
            // Set as MAJOR if multiple substantial changes are detected
            this.updateType = StopPlaceUpdateType.MAJOR;
        }
    }

}
