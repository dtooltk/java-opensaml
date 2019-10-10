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

package org.opensaml.saml.saml2.core;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import java.security.KeyPair;
import java.time.Instant;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLTestSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedAssertionTest extends XMLObjectBaseTestCase {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SignedAssertionTest.class);
    
    /** Credential used for signing. */
    private BasicCredential goodCredential;

    /** Builder of Assertions. */
    private SAMLObjectBuilder<Assertion> assertionBuilder;
    
    /** Builder of Issuers. */
    private SAMLObjectBuilder<Issuer> issuerBuilder;
    
    /** Builder of AuthnStatements. */
    private SAMLObjectBuilder<AuthnStatement> authnStatementBuilder;
    
    /** Builder of Signatures. */
    private XMLObjectBuilder<Signature> signatureBuilder;
    
    /** Generator of element IDs. */
    private RandomIdentifierGenerationStrategy idGenerator;

    @BeforeMethod
    protected void setUp() throws Exception {
        KeyPair keyPair = KeySupport.generateKeyPair("RSA", 1024, null);
        goodCredential = CredentialSupport.getSimpleCredential(keyPair.getPublic(), keyPair.getPrivate());
        
        keyPair = KeySupport.generateKeyPair("RSA", 1024, null);
        CredentialSupport.getSimpleCredential(keyPair.getPublic(), null);
        
        assertionBuilder = (SAMLObjectBuilder<Assertion>) builderFactory.<Assertion>getBuilderOrThrow(Assertion.DEFAULT_ELEMENT_NAME);
        issuerBuilder = (SAMLObjectBuilder<Issuer>) builderFactory.<Issuer>getBuilderOrThrow(Issuer.DEFAULT_ELEMENT_NAME);
        authnStatementBuilder = (SAMLObjectBuilder<AuthnStatement>) builderFactory.<AuthnStatement>getBuilderOrThrow(
                AuthnStatement.DEFAULT_ELEMENT_NAME);
        signatureBuilder = builderFactory.getBuilderOrThrow(Signature.DEFAULT_ELEMENT_NAME);
        
        idGenerator = new RandomIdentifierGenerationStrategy();
    }
    
    /**
     * Creates a simple Assertion, signs it and then verifies the signature.
     * 
     * @throws MarshallingException thrown if the Assertion can not be marshalled into a DOM
     * @throws ValidationException thrown if the Signature does not validate
     * @throws SignatureException 
     * @throws UnmarshallingException 
     * @throws SecurityException 
     */
    @Test
    public void testAssertionSignature() 
        throws MarshallingException, SignatureException, UnmarshallingException, SecurityException{
        Instant now = Instant.now();
        
        Assertion assertion = assertionBuilder.buildObject();
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setID(idGenerator.generateIdentifier());
        assertion.setIssueInstant(now);
        
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue("urn:example.org:issuer");
        assertion.setIssuer(issuer);
        
        AuthnStatement authnStmt = authnStatementBuilder.buildObject();
        authnStmt.setAuthnInstant(now);
        assertion.getAuthnStatements().add(authnStmt);
        
        Signature signature = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(goodCredential);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA);
        assertion.setSignature(signature);
        
        Marshaller marshaller = marshallerFactory.getMarshaller(assertion);
        marshaller.marshall(assertion);
        Signer.signObject(signature);
        
        if (log.isDebugEnabled()) {
            log.debug("Marshalled signed assertion: \n" + SerializeSupport.nodeToString(assertion.getDOM()));
        }
        
        // Unmarshall new tree around DOM to avoid side effects and Apache xmlsec bug.
        Assertion signedAssertion = 
            (Assertion) unmarshallerFactory.getUnmarshaller(assertion.getDOM()).unmarshall(assertion.getDOM());
        
        StaticCredentialResolver credResolver = new StaticCredentialResolver(goodCredential);
        KeyInfoCredentialResolver kiResolver = SAMLTestSupport.buildBasicInlineKeyInfoResolver();
        ExplicitKeySignatureTrustEngine trustEngine = new ExplicitKeySignatureTrustEngine(credResolver, kiResolver);
        
        CriteriaSet criteriaSet = new CriteriaSet( new EntityIdCriterion("urn:example.org:issuer") );
        Assert.assertTrue(trustEngine.validate(signedAssertion.getSignature(), criteriaSet),
                "Assertion signature was not valid");
    }
}