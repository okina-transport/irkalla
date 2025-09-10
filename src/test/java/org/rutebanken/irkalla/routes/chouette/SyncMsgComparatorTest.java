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
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.rutebanken.irkalla.Constants.HEADER_NEXT_BATCH_URL;
import static org.rutebanken.irkalla.Constants.HEADER_SYNC_OPERATION;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_DELTA;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST;

public class SyncMsgComparatorTest {


    @Test
    public void testComparator() throws Exception {
        List<Message> sortedList = Arrays.asList(msg(SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST, null),
                msg(SYNC_OPERATION_FULL, "url"), msg(SYNC_OPERATION_FULL, null),
                msg(SYNC_OPERATION_DELTA, "url"), msg(SYNC_OPERATION_DELTA, null));


        List<Message> toBeSortedList = new ArrayList<>(sortedList);
        toBeSortedList.sort(new SyncMsgComparator());

        Assertions.assertEquals(sortedList, toBeSortedList);


        List<Message> toBeSortedListReversed = new ArrayList<>(sortedList);
        Collections.reverse(toBeSortedListReversed);
        toBeSortedListReversed.sort(new SyncMsgComparator());
        Assertions.assertEquals(sortedList, toBeSortedListReversed);
    }


    private Message msg(String syncOperation, String nextBatchUrl) throws Exception {
        Message msg = new DefaultMessage(new DefaultCamelContext()) {
            private Map<String,Object> headers = new HashMap<>();

            public void setHeader(String key, Object value) {
                headers.put(key, value);
            }

            public Object getHeader(String key) {
                return headers.get(key);
            }

        };
        msg.setHeader(HEADER_NEXT_BATCH_URL, nextBatchUrl);
        msg.setHeader(HEADER_SYNC_OPERATION, syncOperation);
        return msg;
    }
}
