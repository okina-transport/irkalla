
package org.rutebanken.irkalla.routes.chouette;

import org.apache.camel.Message;

import java.util.Comparator;

import static org.rutebanken.irkalla.Constants.HEADER_NEXT_BATCH_URL;
import static org.rutebanken.irkalla.Constants.HEADER_SYNC_OPERATION;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST;

public class SyncMsgComparator implements Comparator<Message> {

    @Override
    public int compare(Message o1, Message o2) {

        Object o1Opr = o1.getHeader(HEADER_SYNC_OPERATION);
        Object o2Opr = o2.getHeader(HEADER_SYNC_OPERATION);

        if (SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST.equals(o1Opr)) {
            if (!SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST.equals(o2Opr)) {
                return -1;
            }
            return 0;
        } else if (SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST.equals(o2Opr)) {
            return 1;
        }

        if (SYNC_OPERATION_FULL.equals(o1Opr)) {
            if (!SYNC_OPERATION_FULL.equals(o2Opr)) {
                return -1;
            }
        } else if (SYNC_OPERATION_FULL.equals(o2Opr)) {
            return 1;
        }

        Object o1NextUrl = o1.getHeader(HEADER_NEXT_BATCH_URL);
        Object o2NextUrl = o2.getHeader(HEADER_NEXT_BATCH_URL);

        if (o1NextUrl != null) {
            if (o2NextUrl == null) {
                return -1;
            }
        } else if (o2NextUrl != null) {
            return 1;
        }

        return 0;

    }
}
