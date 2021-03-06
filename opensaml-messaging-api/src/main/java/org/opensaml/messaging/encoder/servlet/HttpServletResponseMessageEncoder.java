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

package org.opensaml.messaging.encoder.servlet;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.messaging.encoder.MessageEncoder;

/**
 * A specialization of {@link MessageEncoder} that operates on a sink message data type of {@link HttpServletResponse}.
 */
public interface HttpServletResponseMessageEncoder extends MessageEncoder {
    
    /**
     * Get the HTTP servlet response on which to operate.
     * 
     * @return the HTTP servlet response
     */
    @Nullable HttpServletResponse getHttpServletResponse();
    
    /**
     * Set the HTTP servlet response on which to operate.
     * 
     * @param response the HTTP servlet response
     */
    void setHttpServletResponse(@Nullable final HttpServletResponse response);
    
}