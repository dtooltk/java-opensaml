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

package org.opensaml.saml.saml2.profile.impl;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.RequestContextBuilder;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.profile.SAML2ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** {@link AddStatusToResponse} unit test. */
public class AddStatusToResponseTest extends OpenSAMLInitBaseTestCase {
    
    private ProfileRequestContext<Object,Response> prc;
    
    private AddStatusToResponse action;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException, ProfileException {
        prc = new RequestContextBuilder().setOutboundMessage(
                SAML2ActionTestingSupport.buildResponse()).buildProfileRequestContext();
        action = new AddStatusToResponse();
        action.setId("test");
    }

    @Test public void testMinimal() throws ProfileException, ComponentInitializationException {
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        
        final Response response = prc.getOutboundMessageContext().getMessage();

        final Status status = response.getStatus();
        Assert.assertNotNull(status);
        
        Assert.assertNotNull(status.getStatusCode());
        Assert.assertEquals(status.getStatusCode().getValue(), StatusCode.RESPONDER_URI);
        Assert.assertNull(status.getStatusCode().getStatusCode());
        
        Assert.assertNull(status.getStatusMessage());
    }

    @Test public void testMultiStatus() throws ProfileException, ComponentInitializationException {
        action.setStatusCodes(Lists.newArrayList(StatusCode.REQUESTER_URI, StatusCode.REQUEST_VERSION_DEPRECATED_URI));
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        
        final Response response = prc.getOutboundMessageContext().getMessage();

        final Status status = response.getStatus();
        Assert.assertNotNull(status);
        
        Assert.assertNotNull(status.getStatusCode());
        Assert.assertEquals(status.getStatusCode().getValue(), StatusCode.REQUESTER_URI);
        Assert.assertNotNull(status.getStatusCode().getStatusCode());
        Assert.assertEquals(status.getStatusCode().getStatusCode().getValue(), StatusCode.REQUEST_VERSION_DEPRECATED_URI);
        Assert.assertNull(status.getStatusCode().getStatusCode().getStatusCode());
        
        Assert.assertNull(status.getStatusMessage());
    }

    @Test public void testFixedMessage() throws ProfileException, ComponentInitializationException {
        action.setStatusMessage("Foo");
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        
        final Response response = prc.getOutboundMessageContext().getMessage();

        final Status status = response.getStatus();
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatusMessage().getMessage(), "Foo");
    }
    
 }