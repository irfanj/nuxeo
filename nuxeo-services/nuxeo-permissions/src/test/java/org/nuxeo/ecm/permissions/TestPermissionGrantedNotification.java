/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.security.UpdateACEStatusListener;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.1
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.permissions" })
@LocalDeploy({ "org.nuxeo.ecm.permissions:test-listeners-contrib.xml" })
public class TestPermissionGrantedNotification {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Before
    public void before() {
        DummyPermissionGrantedNotificationListener.processedACEs.clear();
    }

    protected DocumentModel createTestDocument() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        return session.createDocument(doc);
    }

    @Test
    public void shouldTriggerPermissionNotificationListener() {
        DocumentModel doc = createTestDocument();

        ACE fryACE = ACE.builder("fry", WRITE).build();
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, fryACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        assertEquals(0, DummyPermissionGrantedNotificationListener.processedACEs.size());
        TransactionHelper.startTransaction();

        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(Constants.NOTIFY_KEY, true);
        ACE leelaACE = ACE.builder("leela", WRITE).contextData(contextData).build();
        acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        assertEquals(1, DummyPermissionGrantedNotificationListener.processedACEs.size());
    }

    @Test
    public void shouldTriggerOnlyOnceForAnACE() throws InterruptedException {
        DocumentModel doc = createTestDocument();

        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(Constants.NOTIFY_KEY, true);
        ACE leelaACE = ACE.builder("leela", WRITE).contextData(contextData).build();
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);

        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(Instant.now().plus(5, ChronoUnit.SECONDS).toEpochMilli());
        ACE fryACE = ACE.builder("fry", WRITE).begin(begin).contextData(contextData).build();
        acp.addACE(ACL.LOCAL_ACL, fryACE);
        ACE benderACE = ACE.builder("bender", WRITE).begin(begin).contextData(contextData).build();
        acp.addACE(ACL.LOCAL_ACL, benderACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        // leela ACE which is permanent
        assertEquals(1, DummyPermissionGrantedNotificationListener.processedACEs.size());
        assertEquals("leela", DummyPermissionGrantedNotificationListener.processedACEs.get(0).getUsername());

        Thread.sleep(10000);

        eventService.fireEvent(UpdateACEStatusListener.UPDATE_ACE_STATUS_EVENT, new EventContextImpl());
        eventService.waitForAsyncCompletion();
        DummyPermissionGrantedNotificationListener.processedACEs.sort((o1, o2) -> o1.getUsername().compareTo(
                o2.getUsername()));
        assertEquals(3, DummyPermissionGrantedNotificationListener.processedACEs.size());
        assertEquals("bender", DummyPermissionGrantedNotificationListener.processedACEs.get(0).getUsername());
        assertEquals("fry", DummyPermissionGrantedNotificationListener.processedACEs.get(1).getUsername());
        assertEquals("leela", DummyPermissionGrantedNotificationListener.processedACEs.get(2).getUsername());
    }
}