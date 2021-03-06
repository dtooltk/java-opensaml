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

package org.opensaml.xmlsec.encryption.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.xmlsec.encryption.EncryptedData;
import org.opensaml.xmlsec.encryption.EncryptedKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * An implementation of {@link EncryptedKeyResolver} which chains multiple other resolver implementations together,
 * calling them in the order specified in the resolver list.
 */
public class ChainingEncryptedKeyResolver extends AbstractEncryptedKeyResolver {

    /** The list of resolvers which form the resolution chain. */
    @Nonnull @NonnullElements private final List<EncryptedKeyResolver> resolvers;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ChainingEncryptedKeyResolver.class);

    /** 
     * Constructor. 
     * 
     * @param encKeyResolvers the chain of encrypted key resolvers
     */
    public ChainingEncryptedKeyResolver(
            @Nonnull @ParameterName(name="encKeyResolvers") final List<EncryptedKeyResolver> encKeyResolvers) {
        Constraint.isNotNull(encKeyResolvers, "List of EncryptedKeyResolvers cannot be null");
        resolvers = new ArrayList<>(Collections2.filter(encKeyResolvers, Predicates.notNull()));
    }

    /** 
     * Constructor. 
     * 
     * @param encKeyResolvers the chain of encrypted key resolvers
     * @param recipients the set of recipients
     */
    public ChainingEncryptedKeyResolver(
            @Nonnull @ParameterName(name="encKeyResolvers") final List<EncryptedKeyResolver> encKeyResolvers,
            @Nullable @ParameterName(name="recipients") final Set<String> recipients) {
        super(recipients);
        Constraint.isNotNull(encKeyResolvers, "List of EncryptedKeyResolvers cannot be null");
        resolvers = new ArrayList<>(Collections2.filter(encKeyResolvers, Predicates.notNull()));
    }
    
    /** 
     * Constructor. 
     * 
     * @param encKeyResolvers the chain of encrypted key resolvers
     * @param recipient the recipient
     */
    public ChainingEncryptedKeyResolver(
            @Nonnull @ParameterName(name="encKeyResolvers") final List<EncryptedKeyResolver> encKeyResolvers,
            @Nullable @ParameterName(name="recipient") final String recipient) {
        this(encKeyResolvers, Collections.singleton(recipient));
    }

    /**
     * Get the unmodifiable list of resolvers which form the resolution chain.
     * 
     * @return a list of EncryptedKeyResolver instances
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<EncryptedKeyResolver> getResolverChain() {
        return ImmutableList.copyOf(resolvers);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Iterable<EncryptedKey> resolve(@Nonnull final EncryptedData encryptedData) {
        if (resolvers.isEmpty()) {
            log.warn("Chaining encrypted key resolver resolution was attempted with an empty resolver chain");
            throw new IllegalStateException("The resolver chain is empty");
        }
        return new ChainingIterable(this, encryptedData);
    }

    /**
     * Implementation of {@link Iterable} to be returned by {@link ChainingEncryptedKeyResolver}.
     */
    public class ChainingIterable implements Iterable<EncryptedKey> {

        /** The chaining encrypted key resolver which owns this instance. */
        private final ChainingEncryptedKeyResolver parent;

        /** The EncryptedData context for resolution. */
        private final EncryptedData encryptedData;

        /**
         * Constructor.
         * 
         * @param resolver the ChainingEncryptedKeyResolver parent
         * @param encData the EncryptedData context for resolution
         */
        public ChainingIterable(@Nonnull final ChainingEncryptedKeyResolver resolver,
                @Nonnull final EncryptedData encData) {
            parent = resolver;
            encryptedData = encData;
        }

        /** {@inheritDoc} */
        @Override
        @Nonnull public Iterator<EncryptedKey> iterator() {
            return new ChainingIterator(parent, encryptedData);
        }

    }

    /**
     * Implementation of {@link Iterator} to be (indirectly) returned by {@link ChainingEncryptedKeyResolver}.
     * 
     */
    public class ChainingIterator implements Iterator<EncryptedKey> {

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(ChainingEncryptedKeyResolver.ChainingIterator.class);

        /** The chaining encrypted key resolver which owns this instance. */
        private final ChainingEncryptedKeyResolver parent;

        /** The EncryptedData context for resolution. */
        private final EncryptedData encryptedData;

        /** The iterator over resolvers in the chain. */
        private final Iterator<EncryptedKeyResolver> resolverIterator;

        /** The iterator over EncryptedKey instances from the current resolver. */
        private Iterator<EncryptedKey> keyIterator;

        /** The current resolver which is returning encrypted keys. */
        private EncryptedKeyResolver currentResolver;

        /** The next encrypted key that is safe to return. */
        private EncryptedKey nextKey;

        /**
         * Constructor.
         * 
         * @param resolver the ChainingEncryptedKeyResolver parent
         * @param encData the EncryptedData context for resolution
         */
        public ChainingIterator(@Nonnull final ChainingEncryptedKeyResolver resolver,
                @Nonnull final EncryptedData encData) {
            parent = resolver;
            encryptedData = encData;
            resolverIterator = parent.getResolverChain().iterator();
            keyIterator = getNextKeyIterator();
            nextKey = null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            if (nextKey != null) {
                return true;
            }
            nextKey = getNextKey();
            if (nextKey != null) {
                return true;
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public EncryptedKey next() {
            EncryptedKey tempKey;
            if (nextKey != null) {
                tempKey = nextKey;
                nextKey = null;
                return tempKey;
            }
            tempKey = getNextKey();
            if (tempKey != null) {
                return tempKey;
            }
            throw new NoSuchElementException("No more EncryptedKey elements are available");
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove operation is not supported by this iterator");
        }

        /**
         * Get the iterator from the next resolver in the chain.
         * 
         * @return an iterator of encrypted keys, or null if none remain
         */
        @Nullable private Iterator<EncryptedKey> getNextKeyIterator() {
            if (resolverIterator.hasNext()) {
                currentResolver = resolverIterator.next();
                log.debug("Getting key iterator from next resolver: {}", currentResolver.getClass().toString());
                return currentResolver.resolve(encryptedData).iterator();
            }
            log.debug("No more resolvers available in the resolver chain");
            currentResolver = null;
            return null;
        }

        /**
         * Get the next encrypted key that will be returned by this iterator.
         * 
         * @return the next encrypted key to return, or null if none remain
         */
        @Nullable private EncryptedKey getNextKey() {
            EncryptedKey tempKey;

            if (keyIterator != null) {
                while (keyIterator.hasNext()) {
                    tempKey = keyIterator.next();
                    if (parent.matchRecipient(tempKey.getRecipient())) {
                        log.debug("Found matching encrypted key: {}", tempKey.toString());
                        return tempKey;
                    }
                }
            }

            keyIterator = getNextKeyIterator();
            while (keyIterator != null) {
                while (keyIterator.hasNext()) {
                    tempKey = keyIterator.next();
                    if (parent.matchRecipient(tempKey.getRecipient())) {
                        log.debug("Found matching encrypted key: {}", tempKey.toString());
                        return tempKey;
                    }
                }
                keyIterator = getNextKeyIterator();
            }

            return null;
        }

    }

}