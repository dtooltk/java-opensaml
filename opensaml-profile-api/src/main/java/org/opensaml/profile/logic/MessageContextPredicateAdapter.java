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

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.ParentProfileRequestContextLookup;

/**
 * A {@link Predicate} which adapts an existing {@link ProfileRequestContext} predicate
 * for use as a {@link MessageContext} predicate.
 * 
 * <p>
 * In normal usage the message context evaluated must have a {@link ProfileRequestContext} somewhere in 
 * its parent chain. Typically this will be its direct parent context, as supplied by 
 * {@link MessageContext#getParent()}. An optional flag is supplied to determine the evaluation result 
 * when no parent profile request context can be located. This case defaults to <code>false</code>.
 * </p>
 * 
 * <p>
 * One example is for use as a {@link org.opensaml.messaging.handler.MessageHandler} activation condition 
 * via {@link org.opensaml.messaging.handler.AbstractMessageHandler#setActivationCondition(Predicate)}.
 * </p>
 */
public class MessageContextPredicateAdapter
        implements net.shibboleth.utilities.java.support.logic.Predicate<MessageContext> {
    
    /** The lookup function for the ProfileRequestContext. */
    @Nonnull private static final ParentProfileRequestContextLookup<MessageContext> PRC_LOOKUP
        = new ParentProfileRequestContextLookup<>();
    
    /** The adapted predicate. */
    @Nonnull private Predicate<ProfileRequestContext> adapted;
    
    /** Flag indicating whether failure to resolve a parent ProfileRequestContext satisfies the predicate. */
    private boolean noPRCSatisfies;
    
    /**
     * Constructor.
     * 
     * <p>
     * Failure to resolve the {@link ProfileRequestContext} parent results in an evaluation of <code>false</code>.
     * </p>
     *
     * @param prcPredicate the adapted predicate
     */
    public MessageContextPredicateAdapter(
                @Nonnull @ParameterName(name = "prcPredicate") final Predicate<ProfileRequestContext> prcPredicate) {
        this(prcPredicate, false);
    }
    
    /**
     * Constructor.
     *
     * @param prcPredicate the adapted predicate
     * @param unresolvedSatisfies whether failure to resolve a parent ProfileRequestContext satisfies the predicate
     */
    public MessageContextPredicateAdapter(
                @Nonnull @ParameterName(name = "prcPredicate") final Predicate<ProfileRequestContext> prcPredicate,
                @ParameterName(name = "unresolvedSatisfies") final boolean unresolvedSatisfies) {
        adapted = Constraint.isNotNull(prcPredicate, "The adapted predicate may not be null");
        noPRCSatisfies = unresolvedSatisfies;
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final MessageContext input) {
        if (input == null) {
            return false;
        }
        
        final ProfileRequestContext prc = PRC_LOOKUP.apply(input);
        if (prc == null) {
            return noPRCSatisfies;
        }
        
        return adapted.test(prc);
    }

}