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

import org.apache.activemq.command.ActiveMQMessage;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.rutebanken.irkalla.Constants.HEADER_NEXT_BATCH_URL;
import static org.rutebanken.irkalla.Constants.HEADER_SYNC_OPERATION;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_DELTA;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST;

public class SyncMsgComparatorTest {


    @Test
    public void testComparator() throws Exception {
        List<ActiveMQMessage> sortedList = Arrays.asList(msg(SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST, null),
                msg(SYNC_OPERATION_FULL, "url"), msg(SYNC_OPERATION_FULL, null),
                msg(SYNC_OPERATION_DELTA, "url"), msg(SYNC_OPERATION_DELTA, null));


        List<ActiveMQMessage> toBeSortedList = new ArrayList<>(sortedList);
        Collections.sort(toBeSortedList, new SyncMsgComparator());

        Assert.assertEquals(sortedList, toBeSortedList);


        List<ActiveMQMessage> toBeSortedListReversed = new ArrayList<>(sortedList);
        Collections.reverse(toBeSortedListReversed);
        Collections.sort(toBeSortedListReversed, new SyncMsgComparator());
        Assert.assertEquals(sortedList, toBeSortedListReversed);
    }


    private ActiveMQMessage msg(String syncOperation, String nextBatchUrl) throws Exception {
        ActiveMQMessage msg = new ActiveMQMessage();
        msg.setProperty(HEADER_NEXT_BATCH_URL, nextBatchUrl);
        msg.setProperty(HEADER_SYNC_OPERATION, syncOperation);
        return msg;
    }
}
