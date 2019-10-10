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

package org.opensaml.saml.saml2.binding.encoding.impl;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.binding.impl.SAMLOutboundDestinationHandler;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.codec.Base64Support;

/**
 * Test case for {@link HTTPPostEncoder}.
 */
public class HTTPPostSimpleSignEncoderTest extends XMLObjectBaseTestCase {

    /** Velocity template engine. */
    private VelocityEngine velocityEngine;

    @BeforeMethod
    public void setUp() throws Exception {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();
    }

    /**
     * Tests encoding a SAML message to an servlet response.
     * 
     * @throws Exception
     */
    @Test
    public void testResponseEncoding() throws Exception {
        SAMLObjectBuilder<StatusCode> statusCodeBuilder =
                (SAMLObjectBuilder<StatusCode>) builderFactory.<StatusCode>getBuilderOrThrow(
                        StatusCode.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(StatusCode.SUCCESS);

        SAMLObjectBuilder<Status> statusBuilder =
                (SAMLObjectBuilder<Status>) builderFactory.<Status>getBuilderOrThrow(Status.DEFAULT_ELEMENT_NAME);
        Status responseStatus = statusBuilder.buildObject();
        responseStatus.setStatusCode(statusCode);

        SAMLObjectBuilder<Response> responseBuilder =
                (SAMLObjectBuilder<Response>) builderFactory.<Response>getBuilderOrThrow(Response.DEFAULT_ELEMENT_NAME);
        Response samlMessage = responseBuilder.buildObject();
        samlMessage.setID("foo");
        samlMessage.setVersion(SAMLVersion.VERSION_20);
        samlMessage.setIssueInstant(Instant.ofEpochMilli(0));
        samlMessage.setStatus(responseStatus);

        SAMLObjectBuilder<AssertionConsumerService> endpointBuilder =
                (SAMLObjectBuilder<AssertionConsumerService>) builderFactory.<AssertionConsumerService>getBuilderOrThrow(
                        AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        Endpoint samlEndpoint = endpointBuilder.buildObject();
        samlEndpoint.setLocation("http://example.org");
        samlEndpoint.setResponseLocation("http://example.org/response");
        
        MessageContext messageContext = new MessageContext();
        messageContext.setMessage(samlMessage);
        SAMLBindingSupport.setRelayState(messageContext, "relay");
        messageContext.getSubcontext(SAMLPeerEntityContext.class, true)
            .getSubcontext(SAMLEndpointContext.class, true).setEndpoint(samlEndpoint);
        
        SAMLOutboundDestinationHandler handler = new SAMLOutboundDestinationHandler();
        handler.invoke(messageContext);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        HTTPPostSimpleSignEncoder encoder = new HTTPPostSimpleSignEncoder();
        encoder.setMessageContext(messageContext);
        encoder.setHttpServletResponse(response);
        
        encoder.setVelocityEngine(velocityEngine);
        
        encoder.initialize();
        encoder.prepareContext();
        encoder.encode();

        Assert.assertEquals(response.getContentType(), "text/html", "Unexpected content type");
        Assert.assertEquals("UTF-8", response.getCharacterEncoding(), "Unexpected character encoding");
        Assert.assertEquals(response.getHeader("Cache-control"), "no-cache, no-store", "Unexpected cache controls");
        
        Document webDoc = Jsoup.parse(response.getContentAsString());
        
        boolean sawDocType = false;
        List<Node>nods = webDoc.childNodes();
        for (Node node : nods) {
           if (node instanceof DocumentType) {
               sawDocType = true;
               DocumentType documentType = (DocumentType)node;
               Assert.assertEquals(documentType.attr("name"), "html");
               Assert.assertEquals(documentType.attr("publicId"), "");
               Assert.assertEquals(documentType.attr("systemId"), "");
           }
        }
        Assert.assertTrue(sawDocType);
        
        Element head = webDoc.selectFirst("html > head");
        Assert.assertNotNull(head);
        Element metaCharSet = head.selectFirst("meta[charset]");
        Assert.assertNotNull(metaCharSet);
        Assert.assertEquals(metaCharSet.attr("charset").toLowerCase(), "utf-8");
        
        Element body = webDoc.selectFirst("html > body");
        Assert.assertNotNull(body);
        Assert.assertEquals(body.attr("onload"), "document.forms[0].submit()");
        
        Element form = body.selectFirst("form");
        Assert.assertNotNull(form);
        Assert.assertEquals(form.attr("method").toLowerCase(), "post");
        Assert.assertEquals(form.attr("action"), "http://example.org/response");
        
        Element relayState = form.selectFirst("input[name=RelayState]");
        Assert.assertNotNull(relayState);
        Assert.assertEquals(relayState.val(), "relay");
        
        Element noscriptMsg = body.selectFirst("noscript > p");
        Assert.assertNotNull(noscriptMsg);
        Assert.assertTrue(noscriptMsg.text().contains("Since your browser does not support JavaScript"));
        
        Element samlResponse = form.selectFirst("input[name=SAMLResponse]");
        Assert.assertNotNull(samlResponse);
        Assert.assertNotNull(samlResponse.val());
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Support.decode(samlResponse.val()))) {
            XMLObject xmlObject = XMLObjectSupport.unmarshallFromInputStream(parserPool, inputStream);
            Assert.assertTrue(xmlObject instanceof Response);
            assertXMLEquals(xmlObject.getDOM().getOwnerDocument(), samlMessage);
        }
        
        Assert.assertNull(form.selectFirst("input[name=SigAlg]"));
        Assert.assertNull(form.selectFirst("input[name=Signature]"));
        Assert.assertNull(form.selectFirst("input[name=KeyInfo]"));
        
        Element submit = body.selectFirst("noscript > div > input[type=submit]");
        Assert.assertNotNull(submit);
        Assert.assertEquals(submit.val(), "Continue");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRequestEncoding() throws Exception {
        SAMLObjectBuilder<AuthnRequest> responseBuilder = (SAMLObjectBuilder<AuthnRequest>) builderFactory
                .getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
        AuthnRequest samlMessage = responseBuilder.buildObject();
        samlMessage.setID("foo");
        samlMessage.setVersion(SAMLVersion.VERSION_20);
        samlMessage.setIssueInstant(Instant.ofEpochMilli(0));

        SAMLObjectBuilder<Endpoint> endpointBuilder = (SAMLObjectBuilder<Endpoint>) builderFactory
                .getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        Endpoint samlEndpoint = endpointBuilder.buildObject();
        samlEndpoint.setLocation("http://example.org");
        samlEndpoint.setResponseLocation("http://example.org/response");
        
        MessageContext messageContext = new MessageContext();
        messageContext.setMessage(samlMessage);
        SAMLBindingSupport.setRelayState(messageContext, "relay");
        messageContext.getSubcontext(SAMLPeerEntityContext.class, true)
            .getSubcontext(SAMLEndpointContext.class, true).setEndpoint(samlEndpoint);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        HTTPPostSimpleSignEncoder encoder = new HTTPPostSimpleSignEncoder();
        encoder.setMessageContext(messageContext);
        encoder.setHttpServletResponse(response);
        
        encoder.setVelocityEngine(velocityEngine);
        
        encoder.initialize();
        encoder.prepareContext();
        encoder.encode();

        Assert.assertEquals(response.getContentType(), "text/html", "Unexpected content type");
        Assert.assertEquals("UTF-8", response.getCharacterEncoding(), "Unexpected character encoding");
        Assert.assertEquals(response.getHeader("Cache-control"), "no-cache, no-store", "Unexpected cache controls");
        
        Document webDoc = Jsoup.parse(response.getContentAsString());
        
        boolean sawDocType = false;
        List<Node>nods = webDoc.childNodes();
        for (Node node : nods) {
           if (node instanceof DocumentType) {
               sawDocType = true;
               DocumentType documentType = (DocumentType)node;
               Assert.assertEquals(documentType.attr("name"), "html");
               Assert.assertEquals(documentType.attr("publicId"), "");
               Assert.assertEquals(documentType.attr("systemId"), "");
           }
        }
        Assert.assertTrue(sawDocType);
        
        Element head = webDoc.selectFirst("html > head");
        Assert.assertNotNull(head);
        Element metaCharSet = head.selectFirst("meta[charset]");
        Assert.assertNotNull(metaCharSet);
        Assert.assertEquals(metaCharSet.attr("charset").toLowerCase(), "utf-8");
        
        Element body = webDoc.selectFirst("html > body");
        Assert.assertNotNull(body);
        Assert.assertEquals(body.attr("onload"), "document.forms[0].submit()");
        
        Element form = body.selectFirst("form");
        Assert.assertNotNull(form);
        Assert.assertEquals(form.attr("method").toLowerCase(), "post");
        Assert.assertEquals(form.attr("action"), "http://example.org");
        
        Element relayState = form.selectFirst("input[name=RelayState]");
        Assert.assertNotNull(relayState);
        Assert.assertEquals(relayState.val(), "relay");
        
        Element noscriptMsg = body.selectFirst("noscript > p");
        Assert.assertNotNull(noscriptMsg);
        Assert.assertTrue(noscriptMsg.text().contains("Since your browser does not support JavaScript"));
        
        Element samlResponse = form.selectFirst("input[name=SAMLRequest]");
        Assert.assertNotNull(samlResponse);
        Assert.assertNotNull(samlResponse.val());
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Support.decode(samlResponse.val()))) {
            XMLObject xmlObject = XMLObjectSupport.unmarshallFromInputStream(parserPool, inputStream);
            Assert.assertTrue(xmlObject instanceof AuthnRequest);
            assertXMLEquals(xmlObject.getDOM().getOwnerDocument(), samlMessage);
        }
        
        Assert.assertNull(form.selectFirst("input[name=SigAlg]"));
        Assert.assertNull(form.selectFirst("input[name=Signature]"));
        Assert.assertNull(form.selectFirst("input[name=KeyInfo]"));
        
        Element submit = body.selectFirst("noscript > div > input[type=submit]");
        Assert.assertNotNull(submit);
        Assert.assertEquals(submit.val(), "Continue");
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testRequestEncodingWithSimpleSign() throws Exception {
        SAMLObjectBuilder<AuthnRequest> responseBuilder = (SAMLObjectBuilder<AuthnRequest>) builderFactory
                .getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
        AuthnRequest samlMessage = responseBuilder.buildObject();
        samlMessage.setID("foo");
        samlMessage.setVersion(SAMLVersion.VERSION_20);
        samlMessage.setIssueInstant(Instant.ofEpochMilli(0));

        SAMLObjectBuilder<Endpoint> endpointBuilder = (SAMLObjectBuilder<Endpoint>) builderFactory
                .getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        Endpoint samlEndpoint = endpointBuilder.buildObject();
        samlEndpoint.setLocation("http://example.org");
        samlEndpoint.setResponseLocation("http://example.org/response");
        
        MessageContext messageContext = new MessageContext();
        messageContext.setMessage(samlMessage);
        SAMLBindingSupport.setRelayState(messageContext, "relay");
        messageContext.getSubcontext(SAMLPeerEntityContext.class, true)
            .getSubcontext(SAMLEndpointContext.class, true).setEndpoint(samlEndpoint);
        
        KeyPair kp = KeySupport.generateKeyPair("RSA", 1024, null);
        SignatureSigningParameters signingParameters = new SignatureSigningParameters();
        signingParameters.setSigningCredential(CredentialSupport.getSimpleCredential(kp.getPublic(), kp.getPrivate()));
        signingParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        NamedKeyInfoGeneratorManager kiManager = DefaultSecurityConfigurationBootstrap.buildBasicKeyInfoGeneratorManager();
        signingParameters.setKeyInfoGenerator(KeyInfoSupport.getKeyInfoGenerator(signingParameters.getSigningCredential(), kiManager, null));
        messageContext.getSubcontext(SecurityParametersContext.class, true).setSignatureSigningParameters(signingParameters);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        HTTPPostSimpleSignEncoder encoder = new HTTPPostSimpleSignEncoder();
        encoder.setMessageContext(messageContext);
        encoder.setHttpServletResponse(response);
        
        encoder.setVelocityEngine(velocityEngine);
        
        encoder.initialize();
        encoder.prepareContext();
        encoder.encode();
        
        Document webDoc = Jsoup.parse(response.getContentAsString());
        
        boolean sawDocType = false;
        List<Node>nods = webDoc.childNodes();
        for (Node node : nods) {
           if (node instanceof DocumentType) {
               sawDocType = true;
               DocumentType documentType = (DocumentType)node;
               Assert.assertEquals(documentType.attr("name"), "html");
               Assert.assertEquals(documentType.attr("publicId"), "");
               Assert.assertEquals(documentType.attr("systemId"), "");
           }
        }
        Assert.assertTrue(sawDocType);
        
        Element head = webDoc.selectFirst("html > head");
        Assert.assertNotNull(head);
        Element metaCharSet = head.selectFirst("meta[charset]");
        Assert.assertNotNull(metaCharSet);
        Assert.assertEquals(metaCharSet.attr("charset").toLowerCase(), "utf-8");
        
        Element body = webDoc.selectFirst("html > body");
        Assert.assertNotNull(body);
        Assert.assertEquals(body.attr("onload"), "document.forms[0].submit()");
        
        Element form = body.selectFirst("form");
        Assert.assertNotNull(form);
        Assert.assertEquals(form.attr("method").toLowerCase(), "post");
        Assert.assertEquals(form.attr("action"), "http://example.org");
        
        Element relayState = form.selectFirst("input[name=RelayState]");
        Assert.assertNotNull(relayState);
        Assert.assertEquals(relayState.val(), "relay");
        
        Element noscriptMsg = body.selectFirst("noscript > p");
        Assert.assertNotNull(noscriptMsg);
        Assert.assertTrue(noscriptMsg.text().contains("Since your browser does not support JavaScript"));
        
        Element samlResponse = form.selectFirst("input[name=SAMLRequest]");
        Assert.assertNotNull(samlResponse);
        Assert.assertNotNull(samlResponse.val());
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Support.decode(samlResponse.val()))) {
            XMLObject xmlObject = XMLObjectSupport.unmarshallFromInputStream(parserPool, inputStream);
            Assert.assertTrue(xmlObject instanceof AuthnRequest);
            assertXMLEquals(xmlObject.getDOM().getOwnerDocument(), samlMessage);
        }
        
        Assert.assertNotNull(form.selectFirst("input[name=SigAlg]"));
        Assert.assertEquals(form.selectFirst("input[name=SigAlg]").val(), SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        Assert.assertNotNull(form.selectFirst("input[name=Signature]"));
        Assert.assertNotNull(form.selectFirst("input[name=Signature]").val());
        Assert.assertNotNull(form.selectFirst("input[name=KeyInfo]"));
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Support.decode(form.selectFirst("input[name=KeyInfo]").val()))) {
            XMLObject xmlObject = XMLObjectSupport.unmarshallFromInputStream(parserPool, inputStream);
            Assert.assertTrue(xmlObject instanceof KeyInfo);
            assertXMLEquals(xmlObject.getDOM().getOwnerDocument(), 
                    signingParameters.getKeyInfoGenerator().generate(signingParameters.getSigningCredential()));
        }
        
        Element submit = body.selectFirst("noscript > div > input[type=submit]");
        Assert.assertNotNull(submit);
        Assert.assertEquals(submit.val(), "Continue");
        
        // Note: to test that actual signature is cryptographically correct, really need a known good test vector.
        // Need to verify that we're signing over the right data in the right byte[] encoded form.
    }
    
}