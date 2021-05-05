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
import org.rutebanken.irkalla.IrkallaException;

import java.io.IOException;
import java.util.Comparator;

import static org.rutebanken.irkalla.Constants.HEADER_NEXT_BATCH_URL;
import static org.rutebanken.irkalla.Constants.HEADER_SYNC_OPERATION;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL;
import static org.rutebanken.irkalla.Constants.SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST;

public class SyncMsgComparator implements Comparator<ActiveMQMessage> {

    @Override
    public int compare(ActiveMQMessage o1, ActiveMQMessage o2) {
        try {
            Object o1Opr = o1.getProperty(HEADER_SYNC_OPERATION);
            Object o2Opr = o2.getProperty(HEADER_SYNC_OPERATION);

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

            Object o1NextUrl = o1.getProperty(HEADER_NEXT_BATCH_URL);
            Object o2NextUrl = o2.getProperty(HEADER_NEXT_BATCH_URL);

            if (o1NextUrl != null) {
                if (o2NextUrl == null) {
                    return -1;
                }
            } else if (o2NextUrl != null) {
                return 1;
            }

            return 0;
        } catch (IOException ioE) {
            throw new IrkallaException("Unable to get sync operation header as property from ActiveMQMessage: " + ioE.getMessage(), ioE);
        }
    }
}

