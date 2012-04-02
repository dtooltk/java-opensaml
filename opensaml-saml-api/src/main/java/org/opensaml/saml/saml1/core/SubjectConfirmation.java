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

/**
 * 
 */

package org.opensaml.saml.saml1.core;

import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.xmlsec.signature.KeyInfo;

/**
 * Interface to define how a SubjectConfirmation element behaves
 */
public interface SubjectConfirmation extends SAMLObject {

    /** Element name, no namespace. */
    public final static String DEFAULT_ELEMENT_LOCAL_NAME = "SubjectConfirmation";
    
    /** Default element name */
    public final static QName DEFAULT_ELEMENT_NAME = new QName(SAMLConstants.SAML1_NS, DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML1_PREFIX);
    
    /** Local name of the XSI type */
    public final static String TYPE_LOCAL_NAME = "SubjectConfirmationType"; 
        
    /** QName of the XSI type */
    public final static QName TYPE_NAME = new QName(SAMLConstants.SAML1_NS, TYPE_LOCAL_NAME, SAMLConstants.SAML1_PREFIX);

    /** Get the list with all the ConfirmationMethods.  This suitable for calls to add() */
    public List<ConfirmationMethod> getConfirmationMethods();

    /** Set the SubjectConfirmationData */
    public void setSubjectConfirmationData(XMLObject subjectConfirmationData) throws IllegalArgumentException;

    /** Return the SubjectConfirmationData */
    public XMLObject getSubjectConfirmationData();
    
    /**
     * Gets the key information for the subject.
     * 
     * @return the key information for the subject
     */
    public KeyInfo getKeyInfo();

    /**
     * Sets the key information for the subject.
     * 
     * @param keyInfo the key information for the subject
     */
    public void setKeyInfo(KeyInfo keyInfo);
}