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

package org.opensaml.saml.metadata.resolver.impl;

import java.util.Iterator;
import java.util.UUID;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * Base class for metadata providers.
 */
public abstract class BaseMetadataResolver extends AbstractDestructableIdentifiableInitializableComponent 
        implements MetadataResolver {

    /** Unmarshaller factory used to get an unmarshaller for the metadata DOM. */
    private UnmarshallerFactory unmarshallerFactory;

    /** Whether metadata is required to be valid. */
    private boolean requireValidMetadata;

    /** Filter applied to all metadata. */
    private MetadataFilter mdFilter;
    
    /** Constructor. */
    public BaseMetadataResolver() {
        super();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        setId(UUID.randomUUID().toString());
    }
    
    /** {@inheritDoc} */
    public boolean isRequireValidMetadata() {
        return requireValidMetadata;
    }

    /** {@inheritDoc} */
    public void setRequireValidMetadata(boolean require) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        requireValidMetadata = require;
    }

    /** {@inheritDoc} */
    @Nullable  public MetadataFilter getMetadataFilter() {
        return mdFilter;
    }

    /** {@inheritDoc} */
    public void setMetadataFilter(@Nullable MetadataFilter newFilter) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        mdFilter = newFilter;
    }
    
    /** {@inheritDoc} */
    @Nullable public EntityDescriptor resolveSingle(CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        Iterable<EntityDescriptor> iterable = resolve(criteria);
        if (iterable != null) {
            Iterator<EntityDescriptor> iterator = iterable.iterator();
            if (iterator != null && iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        unmarshallerFactory = null;
        mdFilter = null;
        
        super.doDestroy();
    }

    /**
     * Get the XMLObject unmarshaller factory to use. 
     * 
     * @return the unmarshaller factory instance to use
     */
    protected UnmarshallerFactory getUnmarshallerFactory() {
        return unmarshallerFactory;
    }

}