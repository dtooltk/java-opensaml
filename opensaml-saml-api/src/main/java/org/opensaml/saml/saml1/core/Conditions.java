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

package org.opensaml.saml.saml1.core;

import java.time.Instant;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;

/**
 * This interface defines how the object representing a SAML1 <code> Conditions</code> element behaves.
 */
public interface Conditions extends SAMLObject {

    /** Element name, no namespace. */
    static final String DEFAULT_ELEMENT_LOCAL_NAME = "Conditions";
    
    /** Default element name. */
    static final QName DEFAULT_ELEMENT_NAME =
            new QName(SAMLConstants.SAML1_NS, DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML1_PREFIX);
    
    /** Local name of the XSI type. */
    static final String TYPE_LOCAL_NAME = "ConditionsType"; 
        
    /** QName of the XSI type. */
    static final QName TYPE_NAME =
            new QName(SAMLConstants.SAML1_NS, TYPE_LOCAL_NAME, SAMLConstants.SAML1_PREFIX);

    /** Name for the NotBefore attribute. */
    static final String NOTBEFORE_ATTRIB_NAME = "NotBefore";

    /** QName for the NotBefore attribute. */
    static final QName NOTBEFORE_ATTRIB_QNAME = new QName(null, "NotBefore", XMLConstants.DEFAULT_NS_PREFIX);

    /** Name for the NotOnOrAfter attribute. */
    static final String NOTONORAFTER_ATTRIB_NAME = "NotOnOrAfter";

    /** QName for the NotOnOrAfter attribute. */
    static final QName NOTONORAFTER_ATTRIB_QNAME =
            new QName(null, "NotOnOrAfter", XMLConstants.DEFAULT_NS_PREFIX);

    /**
     * Get the "not before" condition.
     * 
     * @return the "not before" condition 
     */
    Instant getNotBefore();

    /**
     * Set the "not before" condition.
     * 
     * @param notBefore the "not before" condition 
     */
    void setNotBefore(Instant notBefore);

    /**
     * Get the "not on or after" condition.
     * 
     * @return the "not on or after" condition 
     */
    Instant getNotOnOrAfter();

    /**
     * Set the "not on or after" condition.
     * 
     * @param notOnOrAfter the "not on or after" condition 
     */
    void setNotOnOrAfter(Instant notOnOrAfter);
    
    /**
     * Get the conditions.
     * 
     * @return the conditions
     */
    List<Condition> getConditions();
    
    /**
     * Get the conditions with the given schema type or element name.
     * 
     * @param typeOrName the schema type or element name
     * 
     * @return the matching conditions
     */
    List<Condition> getConditions(QName typeOrName);

    /**
     * Get the audience restriction conditions.
     * 
     * @return the audience restriction conditions
     */
    List<AudienceRestrictionCondition> getAudienceRestrictionConditions();

    /**
     * Get the "do not cache" conditions.
     * 
     * @return the "do not cache" conditions
     */
    List<DoNotCacheCondition> getDoNotCacheConditions();
}