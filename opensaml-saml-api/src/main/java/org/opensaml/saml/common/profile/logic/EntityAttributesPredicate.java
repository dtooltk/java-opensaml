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

package org.opensaml.saml.common.profile.logic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.Predicate;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

/**
 * Predicate to determine whether an {@link EntityDescriptor} or its parent groups contain an {@link EntityAttributes}
 * extension {@link Attribute} that matches the predicate's criteria. 
 */
public class EntityAttributesPredicate implements Predicate<EntityDescriptor> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(EntityAttributesPredicate.class);

    /** Whether to trim the values in the metadata before comparison. */
    private final boolean trimTags;
    
    /** Whether all the candidates must match. */
    private final boolean matchAll;
    
    /** Candidates to check for. */
    @Nonnull @NonnullElements private final Collection<Candidate> candidateSet;

    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     */
    public EntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates) {
        
        Constraint.isNotNull(candidates, "Attribute collection cannot be null");
        
        candidateSet = new ArrayList<>(Collections2.filter(candidates, Predicates.notNull()));
        
        trimTags = true;
        matchAll = false;
    }

    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     */
    public EntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim) {
        
        Constraint.isNotNull(candidates, "Attribute collection cannot be null");
        
        candidateSet = new ArrayList<>(Collections2.filter(candidates, Predicates.notNull()));
        
        trimTags = trim;
        matchAll = false;
    }
    
    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     * @param all true iff all the criteria must match to be a successful test
     */
    public EntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim,
            @ParameterName(name="all") final boolean all) {
        
        Constraint.isNotNull(candidates, "Attribute collection cannot be null");
        
        candidateSet = new ArrayList<>(Collections2.filter(candidates, Predicates.notNull()));
        
        trimTags = trim;
        matchAll = all;
    }    
    
    /**
     * Get whether to trim tags for comparison.
     * 
     * @return  true iff tags are to be trimmed for comparison
     */
    public boolean getTrimTags() {
        return trimTags;
    }

    /**
     * Get whether all candidates must match.
     * 
     * @return  true iff all candidates have to match 
     */
    public boolean getMatchAll() {
        return matchAll;
    }
    
    /**
     * Get the candidate criteria.
     * 
     * @return  the candidate criteria
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Collection<Candidate> getCandidates() {
        return ImmutableList.copyOf(candidateSet);
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    public boolean test(@Nullable final EntityDescriptor input) {
        if (input == null) {
            return false;
        }
        
        Collection<Attribute> entityAttributes = null;

        // Check for a tag match in the EntityAttributes extension of the entity and its parent(s).
        Extensions exts = input.getExtensions();
        if (exts != null) {
            final List<XMLObject> children = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
            if (!children.isEmpty() && children.get(0) instanceof EntityAttributes) {
                if (entityAttributes == null) {
                    entityAttributes = new ArrayList<>();
                }
                entityAttributes.addAll(((EntityAttributes) children.get(0)).getAttributes());
            }
        }

        EntitiesDescriptor group = (EntitiesDescriptor) input.getParent();
        while (group != null) {
            exts = group.getExtensions();
            if (exts != null) {
                final List<XMLObject> children = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
                if (!children.isEmpty() && children.get(0) instanceof EntityAttributes) {
                    if (entityAttributes == null) {
                        entityAttributes = new ArrayList<>();
                    }
                    entityAttributes.addAll(((EntityAttributes) children.get(0)).getAttributes());
                }
            }
            group = (EntitiesDescriptor) group.getParent();
        }

        if (entityAttributes == null || entityAttributes.isEmpty()) {
            log.trace("No Entity Attributes found for {}", input.getEntityID());
            return false;
        }
        
        log.trace("Checking for match against {} Entity Attributes for {}", entityAttributes.size(),
                input.getEntityID());
        
        // If we find a matching tag, we win. Each tag is treated in OR fashion.
        final EntityAttributesMatcher matcher = new EntityAttributesMatcher(entityAttributes);
        
        // Then we determine whether the overall set of tag containers is AND or OR.
        if (matchAll) {
            return Iterables.all(candidateSet, matcher::test);
        }
        if (Iterables.tryFind(candidateSet, matcher::test).isPresent()) {
            return true;
        }

        return false;
    }
    
    /**
     * An object to encapsulate the set of criteria that must be satisfied by an {@link EntityAttributes}
     * extension to satisfy the enclosing predicate.
     */
    public static class Candidate {
        
        /** Attribute Name. */
        @Nonnull @NotEmpty private final String nam;
        
        /** Attribute NameFormat. */
        @Nullable private final String nameFormat;
        
        /** Values that must match exactly. */
        @Nonnull @NonnullElements private List<String> values;
        
        /** Regular expressions that must be satisfied. */
        @Nonnull @NonnullElements private List<Pattern> regexps;

        /**
         * Constructor.
         *
         * @param name   Attribute Name to match
         */
        public Candidate(@Nonnull @NotEmpty @ParameterName(name="name") final String name) {
            nam = Constraint.isNotNull(StringSupport.trimOrNull(name), "Attribute Name cannot be null or empty");
            nameFormat = null;
            
            values = Collections.emptyList();
            regexps = Collections.emptyList(); 
        }

        /**
         * Constructor.
         *
         * @param name   Attribute Name to match
         * @param format Attribute NameFormat to match
         */
        public Candidate(@Nonnull @NotEmpty @ParameterName(name="name") final String name,
                @Nullable @ParameterName(name="format") final String format) {
            nam = Constraint.isNotNull(StringSupport.trimOrNull(name), "Attribute Name cannot be null or empty");
            if (Attribute.UNSPECIFIED.equals(format)) {
                nameFormat = null;
            } else {
                nameFormat = StringSupport.trimOrNull(format);
            }
            
            values = Collections.emptyList();
            regexps = Collections.emptyList(); 
        }

        /**
         * Get the Attribute Name to match.
         * 
         * @return Attribute Name to match
         */
        @Nonnull @NotEmpty public String getName() {
            return nam;
        }

        /**
         * Get the Attribute NameFormat to match.
         * 
         * @return Attribute NameFormat to match
         */
        @Nullable public String getNameFormat() {
            return nameFormat;
        }

        /**
         * Get the exact values to match.
         * 
         * @return the exact values to match
         */
        @Nonnull @NonnullElements @Unmodifiable @NotLive public List<String> getValues() {
            return ImmutableList.copyOf(values);
        }

        /**
         * Set the exact values to match.
         * 
         * @param vals the exact values to match
         */
        public void setValues(@Nonnull @NonnullElements final Collection<String> vals) {
            Constraint.isNotNull(vals, "Values collection cannot be null");
            values = new ArrayList<>(vals.size());
            for (final String value : vals) {
                if (value != null) {
                    values.add(value);
                }
            }
        }

        /**
         * Get the regular expressions to match.
         * 
         * @return the regular expressions to match.
         */
        @Nonnull @NonnullElements @Unmodifiable @NotLive public List<Pattern> getRegexps() {
            return ImmutableList.copyOf(regexps);
        }

        /**
         * Set the regular expressions to match.
         * 
         * @param exps the regular expressions to match
         */
        public void setRegexps(@Nonnull @NonnullElements final Collection<Pattern> exps) {
            Constraint.isNotNull(exps, "Regular expressions collection cannot be null");
            regexps = new ArrayList<>(Collections2.filter(exps, Predicates.notNull()));
        }
    }
    
    /**
     * Determines whether an {@link Candidate} criterion is satisfied by the {@link Attribute}s
     * in an {@link EntityAttributes} extension.
     */
    private class EntityAttributesMatcher implements Predicate<Candidate> {
        
        /** Population to evaluate for a match. */
        private final Collection<Attribute> attributes;
        
        /**
         * Constructor.
         *
         * @param attrs population to evaluate for a match
         */
        public EntityAttributesMatcher(@Nonnull @NonnullElements final Collection<Attribute> attrs) {
            attributes = Constraint.isNotNull(attrs, "Extension attributes cannot be null");
        }

// Checkstyle: MethodLength OFF
        /** {@inheritDoc} */
        public boolean test(@Nonnull final Candidate input) {
            final List<String> tagvals = input.values;
            final List<Pattern> tagexps = input.regexps;

            // Track whether we've found every match we need (possibly with arrays of 0 size).
            final boolean[] valflags = new boolean[tagvals.size()];
            final boolean[] expflags = new boolean[tagexps.size()];

            // Check each attribute/tag in the populated set.
            for (final Attribute a : attributes) {
                // Compare Name and NameFormat for a matching tag.
                if (a.getName() != null && a.getName().equals(input.getName())
                        && (input.getNameFormat() == null || input.getNameFormat().equals(a.getNameFormat()))) {

                    final List<String> attributeValues = getPossibleAttributeValuesAsStrings(a);
                    // Check each tag value's simple content for a value match.
                    for (int tagindex = 0; tagindex < tagvals.size(); ++tagindex) {
                        final String tagvalstr = tagvals.get(tagindex);
                        for (final String cvalstr: attributeValues) {
                            if (tagvalstr != null && cvalstr != null) {
                                if (tagvalstr.equals(cvalstr)) {
                                    log.trace("Matched Entity Attribute ({}:{}) value {}", a.getNameFormat(),
                                            a.getName(), tagvalstr);
                                    valflags[tagindex] = true;
                                    break;
                                } else if (trimTags) {
                                    if (tagvalstr.equals(cvalstr.trim())) {
                                        log.trace("Matched Entity Attribute ({}:{}) value {}", a.getNameFormat(),
                                                a.getName(), tagvalstr);
                                        valflags[tagindex] = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Check each tag regular expression for a match.
                    for (int tagindex = 0; tagindex < tagexps.size(); ++tagindex) {
                        for (final String cvalstr: attributeValues) {
                            if (tagexps.get(tagindex) != null && cvalstr != null) {
                                if (tagexps.get(tagindex).matcher(cvalstr).matches()) {
                                    log.trace("Matched Entity Attribute ({}:{}) value {}", a.getNameFormat(),
                                            a.getName(), cvalstr);
                                    expflags[tagindex] = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            for (final boolean flag : valflags) {
                if (!flag) {
                    return false;
                }
            }

            for (final boolean flag : expflags) {
                if (!flag) {
                    return false;
                }
            }

            return true;
        }
// Checkstyle: MethodLength ON

        /** Get all possible strings values for the attribute.  This copes with the fact that
         * an attribute can return multiple values {@link Attribute#getAttributeValues()} and that some
         * type of value can have multiple values (for instance a boolean can be 1/0/true/false).
         *
         * @param attribute what to inspect
         * @return all possible values, as string.
         */
        @Nonnull List<String> getPossibleAttributeValuesAsStrings(final @Nonnull Attribute attribute) {
            final List<XMLObject> cvals = attribute.getAttributeValues();
            final List<String> result = new ArrayList<>(cvals.size()*2);
            for (final XMLObject cval : cvals) {
                result.addAll(xmlObjectToStrings(cval));
            }
            return result;
        }
     
        /**
         * Convert an XMLObject to an array of String which can represent the type, if recognized.
         * 
         * @param object object to convert
         * @return the converted value, or null
         */
        @Nullable private List<String> xmlObjectToStrings(@Nonnull final XMLObject object) {
            String toMatch = null;
            String toMatchAlt = null;
            if (object instanceof XSString) {
                toMatch = ((XSString) object).getValue();
            } else if (object instanceof XSURI) {
                toMatch = ((XSURI) object).getValue();
            } else if (object instanceof XSBoolean) {
                toMatch = ((XSBoolean) object).getValue().getValue() ? "1" : "0";
                toMatchAlt = ((XSBoolean) object).getValue().getValue() ? "true" : "false";
            } else if (object instanceof XSInteger) {
                toMatch = ((XSInteger) object).getValue().toString();
            } else if (object instanceof XSDateTime) {
                final Instant dt = ((XSDateTime) object).getValue();
                if (dt != null) {
                    toMatch = DOMTypeSupport.instantToString(dt);
                }
            } else if (object instanceof XSBase64Binary) {
                toMatch = ((XSBase64Binary) object).getValue();
            } else if (object instanceof XSAny) {
                final XSAny wc = (XSAny) object;
                if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                    toMatch = wc.getTextContent();
                }
            }
            if (toMatchAlt != null) {
                return List.of(toMatch, toMatchAlt);
            } else if (toMatch != null) {
                return Collections.singletonList(toMatch);
            }
            log.warn("Unrecognized XMLObject type ({}), unable to convert to a string for comparison",
                    object.getClass().getName());
            return Collections.emptyList();
        }
    }
// Checkstyle: CyclomaticComplexity OFF

}