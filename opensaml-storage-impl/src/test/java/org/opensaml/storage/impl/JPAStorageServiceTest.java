/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.storage.impl;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.persistence.EntityManagerFactory;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.opensaml.storage.StorageServiceTest;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test of {@link JPAStorageService} implementation.
 */
public class JPAStorageServiceTest extends StorageServiceTest {

    /** Storage service. */
    private JPAStorageService storageService;

    /** Contexts used for testing. */
    private Object[][] contexts;

    public JPAStorageServiceTest() {
        final SecureRandom random1 = new SecureRandom();
        contexts = new Object[10][1];
        for (int i = 0; i < 10; i++) {
            contexts[i] = new Object[] {Long.toString(random1.nextLong()), };
        }
    }

    /**
     * Creates the shared instance of the entity manager factory.
     */
    @BeforeClass public void setUp() throws ComponentInitializationException {
        storageService = new JPAStorageService(createEntityManagerFactory());
        storageService.setId("test");
        storageService.setCleanupInterval(Duration.ofSeconds(5));
        storageService.setTransactionRetry(2);
        super.setUp();
    }

    /**
     * Creates an entity manager factory instance.
     */
    private EntityManagerFactory createEntityManagerFactory() throws ComponentInitializationException
    {
        final Resource resource = new ClassPathResource("/org/opensaml/storage/impl/jpa-spring-context.xml");
        final GenericApplicationContext context =
                new ApplicationContextBuilder()
                    .setName("JPAStorageService")
                    .setServiceConfiguration(resource)
                    .build();
        final FactoryBean<EntityManagerFactory> factoryBean =
                context.getBean(org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.class);
        try {
            return factoryBean.getObject();
        } catch (Exception e) {
            throw new ComponentInitializationException(e);
        }
    }

    @AfterClass
    protected void tearDown() {
        try {
            List<String> contexts1 = storageService.readContexts();
            for (String ctx : contexts1) {
                storageService.deleteContext(ctx);
            }
            List<?> recs = storageService.readAll();
            Assert.assertEquals(recs.size(), 0);
        } catch (IOException e){ 
            throw new RuntimeException(e);
        }
        super.tearDown();
    }

    @Nonnull protected StorageService getStorageService() {
        return storageService;
    }

    @Test
    public void cleanup() throws ComponentInitializationException, IOException {
        String context = Long.toString(random.nextLong());
        for (int i = 1; i <= 100; i++) {
            storageService.create(context, Integer.toString(i), Integer.toString(i + 1), System.currentTimeMillis() + 100);
        }
        try {
            Thread.sleep(7500);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        List<?> recs = storageService.readAll(context);
        Assert.assertEquals(recs.size(), 0);
    }

    @DataProvider(name = "contexts")
    public Object[][] contexts() throws Exception {
        return contexts;
    }

    @Test(dataProvider = "contexts", singleThreaded = false, threadPoolSize = 25, invocationCount = 100)
    public void multithread(final String context) throws IOException {
        shared.create(context, "mt", "bar", System.currentTimeMillis() + 300000);
        StorageRecord<?> rec = shared.read(context, "mt");
        Assert.assertNotNull(rec);
        shared.update(context, "mt", "baz", System.currentTimeMillis() + 300000);
        rec = shared.read(context, "mt");
        Assert.assertNotNull(rec);
        boolean result = shared.create(context, "mt", "qux", null);
        Assert.assertFalse(result, "createString should have failed");
    }

    @Test(singleThreaded = false, threadPoolSize = 25, invocationCount = 100)
    public void multithreadCaseSensitiveKey() throws IOException {
        shared.create("unit_test", "foo", "bar", null);
        shared.create("unit_test", "FOO", "bar", null);
        StorageRecord<?> rec1 = shared.read("unit_test", "foo");
        StorageRecord<?> rec2 = shared.read("unit_test", "FOO");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);
    }

    @Test
    public void keyCollision() throws IOException {
        shared.create("unit_test", "dlo1", "value", null);
        shared.create("unit_test", "dn11", "value", null);
        StorageRecord<?> rec1 = shared.read("unit_test", "dlo1");
        StorageRecord<?> rec2 = shared.read("unit_test", "dn11");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);

        shared.update("unit_test", "dlo1", "value2", null);
        shared.update("unit_test", "dn11", "value2", null);
        rec1 = shared.read("unit_test", "dlo1");
        rec2 = shared.read("unit_test", "dn11");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);

        Assert.assertEquals(2, storageService.readAll().size());
        Assert.assertEquals(2, storageService.readAll("unit_test").size());

        shared.delete("unit_test", "dlo1");
        rec1 = shared.read("unit_test", "dlo1");
        rec2 = shared.read("unit_test", "dn11");
        Assert.assertNull(rec1);
        Assert.assertNotNull(rec2);
        shared.delete("unit_test", "dn11");
        rec1 = shared.read("unit_test", "dlo1");
        rec2 = shared.read("unit_test", "dn11");
        Assert.assertNull(rec1);
        Assert.assertNull(rec2);
    }

    @Test
    public void caseSensitiveContext() throws IOException {
        shared.create("foo", "bar", "value", null);
        shared.create("FOO", "bar", "value", null);
        StorageRecord<?> rec1 = shared.read("foo", "bar");
        StorageRecord<?> rec2 = shared.read("FOO", "bar");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);

        shared.update("foo", "bar", "value2", null);
        shared.update("FOO", "bar", "value2", null);
        rec1 = shared.read("foo", "bar");
        rec2 = shared.read("FOO", "bar");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);

        Assert.assertEquals(2, storageService.readAll().size());
        Assert.assertEquals(1, storageService.readAll("foo").size());
        Assert.assertEquals(1, storageService.readAll("FOO").size());

        shared.delete("foo", "bar");
        rec1 = shared.read("foo", "bar");
        rec2 = shared.read("FOO", "bar");
        Assert.assertNull(rec1);
        Assert.assertNotNull(rec2);
        shared.delete("FOO", "bar");
        rec1 = shared.read("foo", "bar");
        rec2 = shared.read("FOO", "bar");
        Assert.assertNull(rec1);
        Assert.assertNull(rec2);
    }

    @Test
    public void caseSensitiveKey() throws IOException {
        shared.create("unit_test", "foo", "value", null);
        shared.create("unit_test", "FOO", "value", null);
        StorageRecord<?> rec1 = shared.read("unit_test", "foo");
        StorageRecord<?> rec2 = shared.read("unit_test", "FOO");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);

        shared.update("unit_test", "foo", "value2", null);
        shared.update("unit_test", "FOO", "value2", null);
        rec1 = shared.read("unit_test", "foo");
        rec2 = shared.read("unit_test", "FOO");
        Assert.assertNotNull(rec1);
        Assert.assertNotNull(rec2);
        Assert.assertNotEquals(rec1, rec2);

        Assert.assertEquals(2, storageService.readAll().size());
        Assert.assertEquals(2, storageService.readAll("unit_test").size());

        shared.delete("unit_test", "foo");
        rec1 = shared.read("unit_test", "foo");
        rec2 = shared.read("unit_test", "FOO");
        Assert.assertNull(rec1);
        Assert.assertNotNull(rec2);
        shared.delete("unit_test", "FOO");
        rec1 = shared.read("unit_test", "foo");
        rec2 = shared.read("unit_test", "FOO");
        Assert.assertNull(rec1);
        Assert.assertNull(rec2);
    }

    @Test(enabled = false)
    public void largeValue() throws IOException {
        // hsqldb defaults LOB length to 255 chars; disabled for now
        StringBuilder sb = new StringBuilder(1000 * 36);
        for (int i = 0; i < 1000; i++) {
            sb.append(UUID.randomUUID());
        }
        shared.create("unit_test", "large", sb.toString(), System.currentTimeMillis() + 300000);
        StorageRecord<?> rec = shared.read("unit_test", "large");
        Assert.assertNotNull(rec);
        Assert.assertEquals(sb.toString(), rec.getValue());
    }
}
