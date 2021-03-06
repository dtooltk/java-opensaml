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

package org.opensaml.saml.saml2.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opensaml.core.xml.AbstractXMLObject;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectChildrenList;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.core.RequesterID;
import org.opensaml.saml.saml2.core.Scoping;

/**
 * Concrete implementation of {@link org.opensaml.saml.saml2.core.Scoping}.
 */
public class ScopingImpl extends AbstractXMLObject implements Scoping {

    /** IDPList child element. */
    private IDPList idpList;

    /** List of RequesterID child elements. */
    private final XMLObjectChildrenList<RequesterID> requesterIDs;

    /** ProxyCount attribute. */
    private Integer proxyCount;

    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected ScopingImpl(final String namespaceURI, final String elementLocalName, final String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        requesterIDs = new XMLObjectChildrenList<>(this);
    }

    /** {@inheritDoc} */
    public Integer getProxyCount() {
        return this.proxyCount;
    }

    /** {@inheritDoc} */
    public void setProxyCount(final Integer newProxyCount) {
        this.proxyCount = prepareForAssignment(this.proxyCount, newProxyCount);
    }

    /** {@inheritDoc} */
    public IDPList getIDPList() {
        return idpList;
    }

    /** {@inheritDoc} */
    public void setIDPList(final IDPList newIDPList) {
        this.idpList = prepareForAssignment(this.idpList, newIDPList);

    }

    /** {@inheritDoc} */
    public List<RequesterID> getRequesterIDs() {
        return requesterIDs;
    }

    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        final ArrayList<XMLObject> children = new ArrayList<>();

        if (idpList != null) {
            children.add(idpList);
        }

        children.addAll(requesterIDs);

        if (children.size() > 0) {
            return Collections.unmodifiableList(children);
        } else {
            return null;
        }
    }
}