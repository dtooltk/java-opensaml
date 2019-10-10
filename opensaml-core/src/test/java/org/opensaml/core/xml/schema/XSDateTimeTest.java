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

package org.opensaml.core.xml.schema;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.XMLParserException;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSDateTime;
import org.w3c.dom.Document;

/**
 * Unit test for {@link XSDateTime}
 */
public class XSDateTimeTest extends XMLObjectBaseTestCase {
    
    private QName expectedXMLObjectQName;
    private Instant expectedValue;
    
    @BeforeMethod
    protected void setUp() throws Exception{
        expectedXMLObjectQName = new QName("urn:example.org:foo", "bar", "foo");
        expectedValue = Instant.parse("2010-04-05T18:52:42.790Z");
    }

    /**
     * Tests Marshalling a dateTime type.
     * 
     * @throws MarshallingException 
     * @throws XMLParserException 
     */
    @Test
    public void testMarshall() throws MarshallingException, XMLParserException{
        String testDocumentLocation = "/org/opensaml/core/xml/schema/xsDateTime-basic.xml";
        
        XMLObjectBuilder<XSDateTime> xsdtBuilder = builderFactory.getBuilderOrThrow(XSDateTime.TYPE_NAME);
        XSDateTime xsDateTime = xsdtBuilder.buildObject(expectedXMLObjectQName, XSDateTime.TYPE_NAME);
        xsDateTime.setValue(expectedValue);
        
        Marshaller marshaller = marshallerFactory.getMarshaller(xsDateTime);
        marshaller.marshall(xsDateTime);
        
        Document document = parserPool.parse(XSDateTimeTest.class.getResourceAsStream(testDocumentLocation));
        assertXMLEquals("Marshalled XSDateTime does not match example document", document, xsDateTime);
    }
    
    /**
     * Tests Unmarshalling a dateTime type.
     * 
     * @throws XMLParserException 
     * @throws UnmarshallingException 
     */
    @Test
    public void testUnmarshall() throws XMLParserException, UnmarshallingException{
        String testDocumentLocation = "/org/opensaml/core/xml/schema/xsDateTime-basic.xml";
        
        Document document = parserPool.parse(XSDateTimeTest.class.getResourceAsStream(testDocumentLocation));

        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(document.getDocumentElement());
        XSDateTime xsDateTime = (XSDateTime) unmarshaller.unmarshall(document.getDocumentElement());
        
        Assert.assertEquals(xsDateTime.getElementQName(), expectedXMLObjectQName, "Unexpected XSDate QName");
        Assert.assertEquals(xsDateTime.getSchemaType(), XSDateTime.TYPE_NAME, "Unexpected XSDateTime schema type");
        // For equivalence testing of DateTime instances, need to make sure are in the same chronology
        Assert.assertEquals(xsDateTime.getValue(), expectedValue, "Unexpected value of XSDateTime");
    }
    
    /**
     * Tests Unmarshalling a dateTime type in canonical form, i.e. no trailing zeros in fractional seconds.
     * 
     * @throws XMLParserException 
     * @throws UnmarshallingException 
     */
    @Test
    public void testUnmarshallCanonical() throws XMLParserException, UnmarshallingException{
        String testDocumentLocation = "/org/opensaml/core/xml/schema/xsDateTime-canonical.xml";
        
        Document document = parserPool.parse(XSDateTimeTest.class.getResourceAsStream(testDocumentLocation));

        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(document.getDocumentElement());
        XSDateTime xsDateTime = (XSDateTime) unmarshaller.unmarshall(document.getDocumentElement());
        
        Assert.assertEquals(xsDateTime.getElementQName(), expectedXMLObjectQName, "Unexpected XSDate QName");
        Assert.assertEquals(xsDateTime.getSchemaType(), XSDateTime.TYPE_NAME, "Unexpected XSDateTime schema type");
        // For equivalence testing of DateTime instances, need to make sure are in the same chronology
        Assert.assertEquals(xsDateTime.getValue(), expectedValue, "Unexpected value of XSDateTime");
    }
    
    /**
     * Tests Unmarshalling a dateTime type that has no fractional seconds.
     * 
     * @throws XMLParserException 
     * @throws UnmarshallingException 
     */
    @Test
    public void testUnmarshallNoFractional() throws XMLParserException, UnmarshallingException{
        String testDocumentLocation = "/org/opensaml/core/xml/schema/xsDateTime-nofractional.xml";
        expectedValue = expectedValue.truncatedTo(ChronoUnit.SECONDS);
        
        Document document = parserPool.parse(XSDateTimeTest.class.getResourceAsStream(testDocumentLocation));

        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(document.getDocumentElement());
        XSDateTime xsDateTime = (XSDateTime) unmarshaller.unmarshall(document.getDocumentElement());
        
        Assert.assertEquals(xsDateTime.getElementQName(), expectedXMLObjectQName, "Unexpected XSDate QName");
        Assert.assertEquals(xsDateTime.getSchemaType(), XSDateTime.TYPE_NAME, "Unexpected XSDateTime schema type");
        // For equivalence testing of DateTime instances, need to make sure are in the same chronology
        Assert.assertEquals(xsDateTime.getValue(), expectedValue, "Unexpected value of XSDateTime");
    }
}