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

package org.opensaml.saml.security;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * A validator for instances of {@link Signature}, which validates that the signature meets security-related
 * requirements indicated by the SAML profile of XML Signature.
 */
public class SAMLSignatureProfileValidator {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAMLSignatureProfileValidator.class);

    /** {@inheritDoc} */
    public void validate(Signature signature) throws SignatureException {

        if (!(signature instanceof SignatureImpl)) {
            log.info("Signature was not an instance of SignatureImpl, was {} validation not supported", signature
                    .getClass().getName());
            return;
        }

        validateSignatureImpl((SignatureImpl) signature);
    }

    /**
     * Validate an instance of {@link SignatureImpl}, which is in turn based on underlying Apache XML Security
     * <code>XMLSignature</code> instance.
     * 
     * @param sigImpl the signature implementation object to validate
     * @throws SignatureException thrown if the signature is not valid with respect to the profile
     */
    protected void validateSignatureImpl(SignatureImpl sigImpl) throws SignatureException {

        if (sigImpl.getXMLSignature() == null) {
            log.error("SignatureImpl did not contain the an Apache XMLSignature child");
            throw new SignatureException("Apache XMLSignature does not exist on SignatureImpl");
        }
        XMLSignature apacheSig = sigImpl.getXMLSignature();

        if (!(sigImpl.getParent() instanceof SignableSAMLObject)) {
            log.error("Signature is not an immedidate child of a SignableSAMLObject");
            throw new SignatureException("Signature is not an immediate child of a SignableSAMLObject.");
        }
        SignableSAMLObject signableObject = (SignableSAMLObject) sigImpl.getParent();

        Reference ref = validateReference(apacheSig);

        String uri = ref.getURI();
        String id = signableObject.getSignatureReferenceID();

        validateReferenceURI(uri, id);

        validateTransforms(ref);
    }

    /**
     * Validate the Signature's SignedInfo Reference.
     * 
     * The SignedInfo must contain exactly 1 Reference.
     * 
     * @param apacheSig the Apache XML Signature instance
     * @return the valid Reference contained within the SignedInfo
     * @throws SignatureException thrown if the Signature does not contain exactly 1 Reference, or if there is an error
     *             obtaining the Reference instance
     */
    protected Reference validateReference(XMLSignature apacheSig) throws SignatureException {
        int numReferences = apacheSig.getSignedInfo().getLength();
        if (numReferences != 1) {
            log.error("Signature SignedInfo had invalid number of References: " + numReferences);
            throw new SignatureException("Signature SignedInfo must have exactly 1 Reference element");
        }

        Reference ref = null;
        try {
            ref = apacheSig.getSignedInfo().item(0);
        } catch (XMLSecurityException e) {
            log.error("Apache XML Security exception obtaining Reference", e);
            throw new SignatureException("Could not obtain Reference from Signature/SignedInfo", e);
        }
        if (ref == null) {
            log.error("Signature Reference was null");
            throw new SignatureException("Signature Reference was null");
        }
        return ref;
    }

    /**
     * Validate the Reference URI and parent ID attribute values.
     * 
     * The URI must either be null or empty (indicating that the entire enclosing document was signed), or else it must
     * be a local document fragment reference and point to the SAMLObject parent via the latter's ID attribute value.
     * 
     * @param uri the Signature Reference URI attribute value
     * @param id the Signature parents ID attribute value
     * @throws SignatureException thrown if the URI or ID attribute values are invalid
     */
    protected void validateReferenceURI(String uri, String id) throws SignatureException {
        if (!Strings.isNullOrEmpty(uri)) {
            if (!uri.startsWith("#")) {
                log.error("Signature Reference URI was not a document fragment reference: " + uri);
                throw new SignatureException("Signature Reference URI was not a document fragment reference");
            } else if (Strings.isNullOrEmpty(id)) {
                log.error("SignableSAMLObject did not contain an ID attribute");
                throw new SignatureException("SignableSAMLObject did not contain an ID attribute");
            } else if (uri.length() < 2 || !id.equals(uri.substring(1))) {
                log
                        .error(String.format("Reference URI '%s' did not point to SignableSAMLObject with ID '%s'",
                                uri, id));
                throw new SignatureException("Reference URI did not point to parent ID");
            }
        }
    }

    /**
     * Validate the transforms included in the Signature Reference.
     * 
     * The Reference may contain at most 2 transforms. One of them must be the Enveloped signature transform. An
     * Exclusive Canonicalization transform (with or without comments) may also be present. No other transforms are
     * allowed.
     * 
     * @param reference the Signature reference containing the transforms to evaluate
     * @throws SignatureException thrown if the set of transforms is invalid
     */
    protected void validateTransforms(Reference reference) throws SignatureException {
        Transforms transforms = null;
        try {
            transforms = reference.getTransforms();
        } catch (XMLSecurityException e) {
            log.error("Apache XML Security error obtaining Transforms instance", e);
            throw new SignatureException("Apache XML Security error obtaining Transforms instance", e);
        }

        if (transforms == null) {
            log.error("Error obtaining Transforms instance, null was returned");
            throw new SignatureException("Transforms instance was null");
        }

        int numTransforms = transforms.getLength();
        if (numTransforms > 2) {
            log.error("Invalid number of Transforms was present: " + numTransforms);
            throw new SignatureException("Invalid number of transforms");
        }

        boolean sawEnveloped = false;
        for (int i = 0; i < numTransforms; i++) {
            Transform transform = null;
            try {
                transform = transforms.item(i);
            } catch (TransformationException e) {
                log.error("Error obtaining transform instance", e);
                throw new SignatureException("Error obtaining transform instance", e);
            }
            String uri = transform.getURI();
            if (Transforms.TRANSFORM_ENVELOPED_SIGNATURE.equals(uri)) {
                log.debug("Saw Enveloped signature transform");
                sawEnveloped = true;
            } else if (Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS.equals(uri)
                    || Transforms.TRANSFORM_C14N_EXCL_WITH_COMMENTS.equals(uri)) {
                log.debug("Saw Exclusive C14N signature transform");
            } else {
                log.error("Saw invalid signature transform: " + uri);
                throw new SignatureException("Signature contained an invalid transform");
            }
        }

        if (!sawEnveloped) {
            log.error("Signature was missing the required Enveloped signature transform");
            throw new SignatureException("Transforms did not contain the required enveloped transform");
        }
    }

}