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

package org.opensaml.soap.client.soap11.decoder.http.impl;

import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.httpclient.BaseHttpClientResponseXMLMessageDecoder;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.soap.common.SOAP11FaultDecodingException;
import org.opensaml.soap.messaging.context.SOAP11Context;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Fault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic SOAP 1.1 decoder for HTTP transport via an HttpClient's {@link HttpResponse}.
 * 
 * <p>
 * This decoder takes a mandatory {@link MessageHandler} instance which is used to
 * populate the message that is returned as the {@link MessageContext#getMessage()}.
 * </p>
 * 
 *  <p>
 *  A SOAP message oriented message exchange style might just populate the Envelope as the message.
 *  An application-specific payload-oriented message exchange would handle a specific type
 * of payload structure.  
 * </p>
 */
public class HttpClientResponseSOAP11Decoder extends BaseHttpClientResponseXMLMessageDecoder {
    
    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(HttpClientResponseSOAP11Decoder.class);
    
    /** Message handler to use in processing the message body. */
    private MessageHandler bodyHandler;
    
    /**
     * Get the configured body handler MessageHandler.
     * 
     * @return Returns the bodyHandler.
     */
    public MessageHandler getBodyHandler() {
        return bodyHandler;
    }

    /**
     * Set the configured body handler MessageHandler.
     * 
     * @param newBodyHandler The bodyHandler to set.
     */
    public void setBodyHandler(final MessageHandler newBodyHandler) {
        bodyHandler = newBodyHandler;
    }

    /** {@inheritDoc} */
    protected void doDecode() throws MessageDecodingException {
        final MessageContext messageContext = new MessageContext();
        final HttpResponse response = getHttpResponse();
        
        log.debug("Unmarshalling SOAP message");
        try {
            final int responseStatusCode = response.getStatusLine().getStatusCode();
            
            switch(responseStatusCode) {
                case HttpStatus.SC_OK:
                    final SOAP11Context soapContext = messageContext.getSubcontext(SOAP11Context.class, true);
                    processSuccessResponse(response, soapContext);
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw buildFaultException(response);
                default:
                    throw new MessageDecodingException("Received non-success HTTP response status code from SOAP call: "
                            + responseStatusCode);
            }
            
        } catch (final IOException e) {
            log.error("Unable to obtain input stream from HttpResponse", e);
            throw new MessageDecodingException("Unable to obtain input stream from HttpResponse", e);
        } finally {
            if (response instanceof CloseableHttpResponse) {
                try {
                    ((CloseableHttpResponse)response).close();
                } catch (final IOException e) {
                    log.warn("Error closing HttpResponse", e);
                }
            }
        }
        
        try {
            getBodyHandler().invoke(messageContext);
        } catch (final MessageHandlerException e) {
            log.error("Error processing SOAP Envelope body", e);
            throw new MessageDecodingException("Error processing SOAP Envelope body", e);
        }
        
        if (messageContext.getMessage() == null) {
            log.warn("Body handler did not properly populate the message in message context");
            throw new MessageDecodingException("Body handler did not properly populate the message in message context");
        }
        
        setMessageContext(messageContext);
        
    }
    
    /**
     * Process a successful response, i.e. one where the HTTP response code was 200.
     * 
     * @param httpResponse the HTTP client response
     * @param soapContext the SOAP11Context instance
     * 
     * @throws MessageDecodingException  if message can not be unmarshalled
     * @throws IOException if there is a problem with the response entity input stream
     */
    protected void processSuccessResponse(final HttpResponse httpResponse, final SOAP11Context soapContext) 
            throws MessageDecodingException, IOException {
        
        if (httpResponse.getEntity() == null) {
            throw new MessageDecodingException("No response body from server");
        }
        final Envelope soapMessage = (Envelope) unmarshallMessage(httpResponse.getEntity().getContent());
        
        // Defensive sanity check, otherwise body handler could later fail non-gracefully with runtime exception
        final Fault fault = getFault(soapMessage);
        if (fault != null) {
            throw new SOAP11FaultDecodingException(fault);
        }
        
        soapContext.setEnvelope(soapMessage);
        soapContext.setHTTPResponseStatus(httpResponse.getStatusLine().getStatusCode());
    }

    /**
     * Build an exception by processing a fault response, i.e. one where the HTTP response code was 500.
     * 
     * @param response the HTTP client response
     * @return the message decoding exception representing the SOAP fault
     * 
     * @throws MessageDecodingException if message can not be unmarshalled
     * @throws IOException if there is a problem with the response entity input stream
     */
    protected MessageDecodingException buildFaultException(final HttpResponse response) 
            throws MessageDecodingException, IOException {
        
        if (response.getEntity() == null) {
            throw new MessageDecodingException("No response body from server");
        }
        final Envelope soapMessage = (Envelope) unmarshallMessage(response.getEntity().getContent());
        
        final Fault fault = getFault(soapMessage);
        if (fault == null) {
            throw new MessageDecodingException("HTTP status code was 500 but SOAP response did not contain a Fault");
        }
        
        QName code = null;
        if (fault.getCode() != null) {
            code = fault.getCode().getValue();
        }
        String msg = null;
        if (fault.getMessage() != null) {
            msg = fault.getMessage().getValue();
        }
        log.debug("SOAP fault code '{}' with message '{}'", code != null ? code.toString() : "(not set)", msg);
        
        return new SOAP11FaultDecodingException(fault);
    }
    
    /**
     * Return the Fault element from the SOAP message, if any.
     * 
     * @param soapMessage the SOAP 1.1. Envelope being processed
     * @return the first Fault element found, or null
     */
    protected Fault getFault(final Envelope soapMessage) {
        if (soapMessage.getBody() != null) {
            final List<XMLObject> faults = soapMessage.getBody().getUnknownXMLObjects(Fault.DEFAULT_ELEMENT_NAME);
            if (!faults.isEmpty()) {
                return (Fault) faults.get(0);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (getBodyHandler() == null) {
            throw new ComponentInitializationException("Body handler MessageHandler cannot be null");
        }
    }    
    
    /** {@inheritDoc} */
    @Override
    protected XMLObject getMessageToLog() {
        return getMessageContext().getSubcontext(SOAP11Context.class, true).getEnvelope();
    }
    
}
