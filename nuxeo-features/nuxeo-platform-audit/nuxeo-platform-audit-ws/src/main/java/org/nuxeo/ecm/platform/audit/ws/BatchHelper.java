/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatchHelper {

    private static final Map<String, BatchInfo> pageInfo = new ConcurrentHashMap<String, BatchInfo>();

    // Utility class.
    private BatchHelper() {
    }

    public static BatchInfo getBatchInfo(String sessionId, String dateRange) {

        BatchInfo bInfo = pageInfo.get(sessionId);
        if (bInfo != null) {
            if (bInfo.getPageDateRange().equals(dateRange)) {
                return bInfo;
            }
        }
        pageInfo.put(sessionId, new BatchInfo(dateRange));
        return pageInfo.get(sessionId);
    }

    public static void resetBatchInfo(String sessionId) {
        if (pageInfo.containsKey(sessionId)) {
            pageInfo.remove(sessionId);
        }
    }

}
