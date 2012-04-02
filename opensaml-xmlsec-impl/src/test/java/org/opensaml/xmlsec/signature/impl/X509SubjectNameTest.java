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

package org.opensaml.xmlsec.signature.impl;


import org.opensaml.core.xml.XMLObjectProviderBaseTestCase;
import org.opensaml.xmlsec.signature.X509SubjectName;

/**
 *
 */
public class X509SubjectNameTest extends XMLObjectProviderBaseTestCase {
    
    private String expectedStringContent;

    /**
     * Constructor
     *
     */
    public X509SubjectNameTest() {
        singleElementFile = "/data/org/opensaml/xml/signature/impl/X509SubjectName.xml";
        
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        expectedStringContent = "someX509SubjectName";
    }

    /** {@inheritDoc} */
    public void testSingleElementUnmarshall() {
        X509SubjectName x509Element = (X509SubjectName) unmarshallElement(singleElementFile);
        
        assertNotNull("X509SubjectName", x509Element);
        assertEquals("X509SubjectName value", x509Element.getValue(), expectedStringContent);
    }

    /** {@inheritDoc} */
    public void testSingleElementMarshall() {
        X509SubjectName x509Element = (X509SubjectName) buildXMLObject(X509SubjectName.DEFAULT_ELEMENT_NAME);
        x509Element.setValue(expectedStringContent);
        
        assertEquals(expectedDOM, x509Element);
    }

}