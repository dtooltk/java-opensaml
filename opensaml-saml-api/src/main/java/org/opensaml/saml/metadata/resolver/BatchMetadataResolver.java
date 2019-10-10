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

package org.opensaml.saml.metadata.resolver;

import java.time.Instant;

import javax.annotation.Nullable;

/**
 * Marker interface for {@link MetadataResolver} implementations which resolve
 * metadata from a batch of data loaded and processed in advance of resolution operations,
 * for example by loading an XML document from a file or HTTP resource at component initialization 
 * time.
 */
public interface BatchMetadataResolver extends MetadataResolver {

    /**
     * Get the validUntil of of the metadata batch root element, if present.
     *
     * @return the validUntil date/time of the root element, or null if not available
     */
    @Nullable public Instant getRootValidUntil();

    /**
     * Get the validity state of the metadata batch root element, as determined in an implementation-specific manner.
     *
     * @return true if root element is valid, false if not valid, null if indeterminate
     */
    @Nullable public Boolean isRootValid();

}
