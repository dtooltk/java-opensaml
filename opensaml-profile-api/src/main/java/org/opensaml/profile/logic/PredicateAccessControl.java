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

package org.opensaml.profile.logic;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletRequest;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.AccessControl;

import org.opensaml.profile.context.AccessControlContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access control implementation based on a predicate over a {@link ProfileRequestContext}.
 * 
 * <p>To bridge the two designs, this adapter populates an {@link AccessControlContext} placed
 * beneath the {@link ProfileRequestContext} to carry the operation and resource parameters
 * and allow access to them from the {@link Predicate}.</p>
 * 
 * @since 3.3.0
 */
public class PredicateAccessControl extends AbstractIdentifiableInitializableComponent
        implements AccessControl {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PredicateAccessControl.class);

    /** The predicate to use. */
    @Nonnull private final Predicate<ProfileRequestContext> predicate;
    
    /**
     * Constructor.
     *
     * @param condition the predicate to use
     */
    public PredicateAccessControl(@Nonnull @ParameterName(name="condition")
                                   final Predicate<ProfileRequestContext> condition) {
        predicate = Constraint.isNotNull(condition, "Predicate cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean checkAccess(@Nonnull final ServletRequest request, @Nullable final String operation,
            @Nullable final String resource) {
        
        Constraint.isNotNull(request, "ServletRequest cannot be null");
        
        final Object attribute = request.getAttribute(ProfileRequestContext.BINDING_KEY);
        if (attribute != null && attribute instanceof ProfileRequestContext) {
            final ProfileRequestContext prc = (ProfileRequestContext) attribute;
            final AccessControlContext acc = prc.getSubcontext(AccessControlContext.class, true);
            acc.setOperation(operation);
            acc.setResource(resource);
            if (predicate.test(prc)) {
                prc.removeSubcontext(acc);
                log.debug("{} Granted access based on predicate (Operation: {}, Resource: {})",
                        new Object[] {getLogPrefix(), operation, resource});
                return true;
            }
            prc.removeSubcontext(acc);
            log.warn("{} Denied request based on predicate (Operation: {}, Resource: {})",
                    new Object[] {getLogPrefix(), operation, resource});
        } else {
            log.warn("{} Denied request based on predicate, missing ProfileRequestContext (Operation: {}, Resource: {})"
                    , new Object[] {getLogPrefix(), operation, resource});
        }
        
        return false;
    }
    
    /**
     * Get logging prefix.
     * 
     * @return  prefix
     */
    @Nonnull private String getLogPrefix() {
        return "Policy " + getId() + ":";
    }

}