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

package org.rutebanken.irkalla;

public class Constants {
    public static final String HEADER_PROCESS_TARGET= "RutebankenProcessTarget";
    public static final String HEADER_CRUD_ACTION = "RutebankenCrudAction";
    public static final String HEADER_ENTITY_ID = "RutebankenEntityId";
    public static final String HEADER_ENTITY_VERSION = "RutebankenEntityVersion";
    public static final String HEADER_SYNC_STATUS_FROM = "RutebankenSyncStatusFrom";
    public static final String HEADER_SYNC_STATUS_TO = "RutebankenSyncStatusTo";
    public static final String HEADER_SYNC_OPERATION = "RutebankenSyncOperation";
    public static final String HEADER_NEXT_BATCH_URL = "RutebankenNextBatchURL";
    public static final String SINGLETON_ROUTE_DEFINITION_GROUP_NAME = "IrkallaSingletonRouteDefinitionGroup";
    public static final String SYNC_OPERATION_DELTA="DELTA";
    public static final String SYNC_OPERATION_FULL="FULL";
    public static final String SYNC_OPERATION_FULL_WITH_DELETE_UNUSED_FIRST="DELETE_UNUSED";

    public static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";
    public static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";
    public static final String AUTHORIZATION_HEADER = "Authorization";


}
