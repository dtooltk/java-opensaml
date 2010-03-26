/*
 * Copyright 2008 Members of the EGEE Collaboration.
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensaml.ws.wstrust;

import javax.xml.namespace.QName;

import org.opensaml.xml.XMLObject;

/**
 * Interface ParticipantType complex type.
 * 
 */
public interface ParticipantType extends WSTrustObject {
    
    /** Local name of the XSI type. */
    public static final String TYPE_LOCAL_NAME = "ParticipantType"; 
        
    /** QName of the XSI type. */
    public static final QName TYPE_NAME = 
        new QName(WSTrustConstants.WST_NS, TYPE_LOCAL_NAME, WSTrustConstants.WST_PREFIX);
    
    /**
     * Get the unknown child element.
     * 
     * @return the child element
     */
    public XMLObject getUnknownXMLObject();
    
    /**
     * Set the unknown child element.
     * 
     * @param unknownObject the new child element
     */
    public void setUnknownXMLObject(XMLObject unknownObject);

}
