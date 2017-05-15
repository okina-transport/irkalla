package org.rutebanken.irkalla.routes.tiamat;

import org.junit.Assert;
import org.junit.Test;
import org.rutebanken.irkalla.domain.CrudAction;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.GraphqlGeometry;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.Name;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.Quay;
import org.rutebanken.irkalla.routes.tiamat.graphql.model.StopPlace;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StopPlaceChangeTest {

    @Test
    public void createDoesNotGiveChanges(){
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.CREATE, current, null);
        Assert.assertNull(change.getUpdateType());
        Assert.assertNull(change.getOldValue());
        Assert.assertNull(change.getNewValue());
    }

    @Test
    public void removeDoesNotGiveChanges(){
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
    public void onlyCoordinateChanged() {
        StopPlace current = stopPlace("stopName", 4, 2, "quay1");
        StopPlace prev = stopPlace("stopName", 4, 5, "quay1");

        StopPlaceChange change = new StopPlaceChange(CrudAction.UPDATE, current, prev);
        Assert.assertEquals(StopPlaceChange.StopPlaceUpdateType.COORDINATES, change.getUpdateType());
        Assert.assertEquals(prev.geometry.toString(), change.getOldValue());
        Assert.assertEquals(current.geometry.toString(), change.getNewValue());
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


    private StopPlace stopPlace(String name, double x, double y, String... quayIds) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.name = new Name(name);
        stopPlace.geometry = new GraphqlGeometry("point", Arrays.asList(Arrays.asList(x, y)));

        if (quayIds != null) {
            stopPlace.quays = Arrays.stream(quayIds).map(id -> new Quay(id, new Name(id), stopPlace.geometry)).collect(Collectors.toList());
        }

        return stopPlace;
    }
}