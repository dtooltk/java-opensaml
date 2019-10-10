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

package org.opensaml.saml.common.messaging.context;

import java.time.Instant;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.TestContext;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the {@link SAMLMessageInfoContext}.
 * 
 * Note: this test appears in the -impl module rather than the -api module because
 * we need to build actual SAMLObject instances. Can't have circular dependencies in Maven.
 */
public class SAMLMessageInfoContextTest extends XMLObjectBaseTestCase {
    
    private String id;
    
    private Instant issueInstant;
    
    @BeforeClass
    public void setUp() {
        id = "abc123";
        issueInstant = Instant.now();
    }
    
    @Test
    public void testNoParent() {
        SAMLMessageInfoContext infoContext = new SAMLMessageInfoContext();
        Assert.assertNull(infoContext.getMessageId());
        Assert.assertNull(infoContext.getMessageIssueInstant());
    }
    
    @Test
    public void testNonMessageContextParent() {
        TestContext testContext = new TestContext();
        SAMLMessageInfoContext infoContext =  testContext.getSubcontext(SAMLMessageInfoContext.class, true);
        Assert.assertNull(infoContext.getMessageId());
        Assert.assertNull(infoContext.getMessageIssueInstant());
        
    }
    
    @Test
    public void testNonSAMLMessage() {
        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(new Object());
        final SAMLMessageInfoContext infoContext =  messageContext.getSubcontext(SAMLMessageInfoContext.class, true);
        Assert.assertNull(infoContext.getMessageId());
        Assert.assertNull(infoContext.getMessageIssueInstant());
    }
    
    @Test
    public void testSAML2Request() {
        final MessageContext messageContext = new MessageContext();
        final AuthnRequest message = buildXMLObject(AuthnRequest.DEFAULT_ELEMENT_NAME);
        message.setID(id);
        message.setIssueInstant(issueInstant);
        messageContext.setMessage(message);
        final SAMLMessageInfoContext infoContext =  messageContext.getSubcontext(SAMLMessageInfoContext.class, true);
        Assert.assertEquals(infoContext.getMessageId(), id);
        Assert.assertEquals(infoContext.getMessageIssueInstant(), issueInstant);
    }

    @Test
    public void testSAML2Response() {
        final MessageContext messageContext = new MessageContext();
        final Response message = buildXMLObject(Response.DEFAULT_ELEMENT_NAME);
        message.setID(id);
        message.setIssueInstant(issueInstant);
        messageContext.setMessage(message);
        final SAMLMessageInfoContext infoContext =  messageContext.getSubcontext(SAMLMessageInfoContext.class, true);
        Assert.assertEquals(infoContext.getMessageId(), id);
        Assert.assertEquals(infoContext.getMessageIssueInstant(), issueInstant);
    }
    
    @Test
    public void testSAML1Request() {
        final MessageContext messageContext = new MessageContext();
        final Request message = buildXMLObject(Request.DEFAULT_ELEMENT_NAME);
        message.setID(id);
        message.setIssueInstant(issueInstant);
        messageContext.setMessage(message);
        final SAMLMessageInfoContext infoContext =  messageContext.getSubcontext(SAMLMessageInfoContext.class, true);
        Assert.assertEquals(infoContext.getMessageId(), id);
        Assert.assertEquals(infoContext.getMessageIssueInstant(), issueInstant);
    }
    
    @Test
    public void testSAML1Response() {
        final MessageContext messageContext = new MessageContext();
        final org.opensaml.saml.saml1.core.Response message = buildXMLObject(org.opensaml.saml.saml1.core.Response.DEFAULT_ELEMENT_NAME);
        message.setID(id);
        message.setIssueInstant(issueInstant);
        messageContext.setMessage(message);
        final SAMLMessageInfoContext infoContext =  messageContext.getSubcontext(SAMLMessageInfoContext.class, true);
        Assert.assertEquals(infoContext.getMessageId(), id);
        Assert.assertEquals(infoContext.getMessageIssueInstant(), issueInstant);
    }

}
