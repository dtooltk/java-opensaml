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

package org.opensaml.saml.metadata.resolver.index.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.saml.metadata.resolver.index.MetadataIndexKey;
import org.opensaml.saml.metadata.resolver.index.SimpleStringMetadataIndexKey;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

public class ConstantEntityDescriptorFunction implements Function<EntityDescriptor, Set<MetadataIndexKey>> {
    
    private String value;
    
    public ConstantEntityDescriptorFunction(String val) {
        value = Constraint.isNotNull(StringSupport.trimOrNull(val), "Value was null or empty");
    }


    @Nullable public Set<MetadataIndexKey> apply(@Nullable EntityDescriptor input) {
        if (input == null) {
            return Collections.emptySet();
        }
        HashSet<MetadataIndexKey> result = new HashSet<>();
        if (input != null) {
            result.add(new SimpleStringMetadataIndexKey(value));
        }
        return result;
    }
    
}