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

import org.junit.Assert;

import org.junit.jupiter.api.Test;
import org.rutebanken.irkalla.domain.CrudAction;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.GraphqlGeometry;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.Name;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.Quay;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.StopPlace;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.TopographicPlace;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.rutebanken.irkalla.routes.tiamat.StopPlaceChange.MULTI_MODAL_TYPE;
import static org.rutebanken.irkalla.routes.tiamat.StopPlaceChange.PARENT_STOP_PLACE_TYPE;

public class StopPlaceChangeTest {

    @Test
    public void createDoesNotGiveChanges() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.CREATE, current, null);
        Assert.assertNull(change.getUpdateType());
        Assert.assertNull(change.getOldValue());
        Assert.assertNull(change.getNewValue());
    }

    @Test
    public void removeDoesNotGiveChanges() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("stopName", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.REMOVE, current, prev);
        Assert.assertNull(change.getUpdateType());
        Assert.assertNull(change.getOldValue());
        Assert.assertNull(change.getNewValue());
    }

    @Test
    public void onlyMinorChanges() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("stopName", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.MINOR, change.getUpdateType());
        Assert.assertNull(change.getOldValue());
        Assert.assertNull(change.getNewValue());
    }

    @Test
    public void onlyNameChanged() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("me", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.NAME, change.getUpdateType());
        Assert.assertEquals(prev.getNameAsString(), change.getOldValue());
        Assert.assertEquals(current.getNameAsString(), change.getNewValue());
    }

    @Test
    public void onlyTypeChanged() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        current.stopPlaceType = "onstreetBus";
        StopPlace prev = stopPlace("stopName", 4, 2, "quay1");
        prev.stopPlaceType = "onstreetTram";

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.TYPE, change.getUpdateType());
        Assert.assertEquals(prev.stopPlaceType, change.getOldValue());
        Assert.assertEquals(current.stopPlaceType, change.getNewValue());
    }

    @Test
    public void onlyCoordinateChanged() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("stopName", 4, 5, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.COORDINATES, change.getUpdateType());
        Assert.assertEquals("(4.0,5.0)", change.getOldValue());
        Assert.assertEquals("(4.0,2.0)", change.getNewValue());
    }

    @Test
    public void onlyNewQuaysAdded() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1", "quay2");
        StopPlace prev = stopPlace("stopName", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.NEW_QUAY, change.getUpdateType());
        Assert.assertEquals("[]", change.getOldValue());
        Assert.assertEquals("[quay2]", change.getNewValue());
    }

    @Test
    public void onlyNewQuaysReplaced() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay2");
        StopPlace prev = stopPlace("stopName", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.NEW_QUAY, change.getUpdateType());
        Assert.assertEquals("[quay1]", change.getOldValue());
        Assert.assertEquals("[quay2]", change.getNewValue());
    }

    @Test
    public void onlyQuaysRemoved() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("stopName", 4, 2, "quay1", "quay2");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.REMOVED_QUAY, change.getUpdateType());
        Assert.assertEquals("[quay2]", change.getOldValue());
        Assert.assertNull(change.getNewValue());
    }


    @Test
    public void majorChanges() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("oldName", 4, 2, "quay1", "quay2");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.MAJOR, change.getUpdateType());
        Assert.assertTrue(change.getOldValue().contains("quay2"));
        Assert.assertTrue(change.getOldValue().contains(prev.getNameAsString()));
        Assert.assertTrue(change.getNewValue().contains(current.getNameAsString()));
    }


    @Test
    public void missingValidBetweenGivesChangeTimeAsNow() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        Instant beforeTest = Instant.now();
        StopPlaceChange change = new StopPlaceChange(CrudAction.CREATE, current, null);
        Assert.assertFalse(change.getChangeTime().isBefore(beforeTest));
    }

    @Test
    public void getLocationFormatsTopToBottom() {
        StopPlace current = new StopPlace();
        current.topographicPlace = topographicPlace("parent", topographicPlace("grandParent", topographicPlace("greatGrandParent", null)));
        StopPlaceChange change = new StopPlaceChange(CrudAction.CREATE, current, null);
        Assert.assertEquals("greatGrandParent, grandParent, parent", change.getLocation());
    }

    @Test
    public void getEntityClassifierReturnsMultiModalIfTypeNotSetAndStopIsParent() {
        StopPlace current = new StopPlace();
        current.__typename = PARENT_STOP_PLACE_TYPE;
        StopPlaceChange change = new StopPlaceChange(CrudAction.CREATE, current, null);
        Assert.assertEquals(MULTI_MODAL_TYPE, change.getEntityClassifier());
    }

    @Test
    public void getEntityClassifierReturnsNullIfTypeNotSetAndStopIsNotParent() {
        StopPlace current = new StopPlace();
        StopPlaceChange change = new StopPlaceChange(CrudAction.CREATE, current, null);
        Assert.assertNull(change.getEntityClassifier());
    }


    private TopographicPlace topographicPlace(String name, TopographicPlace parent) {
        TopographicPlace topographicPlace = new TopographicPlace();
        topographicPlace.parentTopographicPlace = parent;
        topographicPlace.name = new Name(name);
        return topographicPlace;
    }

    private StopPlace stopPlace(String name, double x, double y, String... quayIds) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.name = new Name(name);
        stopPlace.geometry = new GraphqlGeometry("Point", Arrays.asList(Arrays.asList(x, y)));

        if (quayIds != null) {
            stopPlace.quays = Arrays.stream(quayIds).map(id -> new Quay(id, new Name(id), stopPlace.geometry)).collect(Collectors.toList());
        }

        return stopPlace;
    }
}
