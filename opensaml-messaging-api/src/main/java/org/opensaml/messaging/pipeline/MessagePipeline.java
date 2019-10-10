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

package org.opensaml.messaging.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.encoder.MessageEncoder;
import org.opensaml.messaging.handler.MessageHandler;

/**
 * Interface representing the basic components of a message processing pipeline.
 */
public interface MessagePipeline {
    
    /**
     * Get the message encoder instance.
     * 
     * @return the message encoder
     */
    @Nonnull public MessageEncoder getEncoder();
    
    /**
     * Get the message decoder instance.
     * 
     * @return the message decoder
     */
    @Nonnull public MessageDecoder getDecoder();
    
    /**
     * Get the (optional) outbound payload message handler instance.  
     * 
     * <p>
     * This is the handler intended to be called on the outbound message context, prior to any message encoding.
     * </p>
     * 
     * @return the outbound message handler, may be null
     */
    @Nullable public MessageHandler getOutboundPayloadMessageHandler();
    
    /**
     * Get the (optional) outbound transport message handler instance.  
     * 
     * <p>
     * This is the handler intended to be called after {@link MessageEncoder#prepareContext()},
     * but before {@link MessageEncoder#encode()}.
     * </p>
     * 
     * @return the outbound message handler, may be null
     */
    @Nullable public MessageHandler getOutboundTransportMessageHandler();
    
    /**
     * Get the (optional) inbound message handler instance.  
     * 
     * @return the inbound message handler, may be null
     */
    public MessageHandler getInboundMessageHandler();

}
