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

package org.opensaml.xmlsec.keyinfo;

import java.math.BigInteger;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAParameterSpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.X509Support;
import org.opensaml.xmlsec.algorithm.AlgorithmSupport;
import org.opensaml.xmlsec.signature.DEREncodedKeyValue;
import org.opensaml.xmlsec.signature.DSAKeyValue;
import org.opensaml.xmlsec.signature.Exponent;
import org.opensaml.xmlsec.signature.G;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.KeyName;
import org.opensaml.xmlsec.signature.KeyValue;
import org.opensaml.xmlsec.signature.Modulus;
import org.opensaml.xmlsec.signature.P;
import org.opensaml.xmlsec.signature.Q;
import org.opensaml.xmlsec.signature.RSAKeyValue;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.X509Digest;
import org.opensaml.xmlsec.signature.X509IssuerName;
import org.opensaml.xmlsec.signature.X509IssuerSerial;
import org.opensaml.xmlsec.signature.X509SKI;
import org.opensaml.xmlsec.signature.X509SerialNumber;
import org.opensaml.xmlsec.signature.X509SubjectName;
import org.opensaml.xmlsec.signature.Y;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Utility class for working with data inside a KeyInfo object.
 * 
 * Methods are provided for converting the representation stored in the XMLTooling KeyInfo to Java java.security native
 * types, and for storing these Java native types inside a KeyInfo.
 */
public class KeyInfoSupport {

    /**
     * Factory for {@link java.security.cert.X509Certificate} and {@link java.security.cert.X509CRL} creation.
     */
    private static CertificateFactory x509CertFactory;

    /** Constructor. */
    protected KeyInfoSupport() {

    }

    /**
     * Get the set of key names inside the specified {@link KeyInfo} as a list of strings.
     * 
     * @param keyInfo {@link KeyInfo} to retrieve key names from
     * 
     * @return a list of key name strings
     */
    @Nonnull public static List<String> getKeyNames(@Nullable final KeyInfo keyInfo) {
        final List<String> keynameList = new LinkedList<>();

        if (keyInfo == null) {
            return keynameList;
        }

        final List<KeyName> keyNames = keyInfo.getKeyNames();
        for (final KeyName keyName : keyNames) {
            if (keyName.getValue() != null) {
                keynameList.add(keyName.getValue());
            }
        }

        return keynameList;
    }

    /**
     * Add a new {@link KeyName} value to a KeyInfo.
     * 
     * @param keyInfo the KeyInfo to which to add the new value
     * @param keyNameValue the new key name value to add
     */
    public static void addKeyName(@Nonnull final KeyInfo keyInfo, @Nullable final String keyNameValue) {
        Constraint.isNotNull(keyInfo, "KeyInfo cannot be null");

        final XMLObjectBuilder<KeyName> keyNameBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(KeyName.DEFAULT_ELEMENT_NAME);
        final KeyName keyName = keyNameBuilder.buildObject(KeyName.DEFAULT_ELEMENT_NAME);
        keyName.setValue(keyNameValue);
        keyInfo.getKeyNames().add(keyName);
    }

    /**
     * Get a list of the Java {@link java.security.cert.X509Certificate} within the given KeyInfo.
     * 
     * @param keyInfo key info to extract the certificates from
     * 
     * @return a list of Java {@link java.security.cert.X509Certificate}s
     * 
     * @throws CertificateException thrown if there is a problem converting the X509 data into
     *             {@link java.security.cert.X509Certificate}s.
     */
    @Nonnull public static List<X509Certificate> getCertificates(@Nullable final KeyInfo keyInfo)
            throws CertificateException {
        final List<X509Certificate> certList = new LinkedList<>();

        if (keyInfo == null) {
            return certList;
        }

        final List<X509Data> x509Datas = keyInfo.getX509Datas();
        for (final X509Data x509Data : x509Datas) {
            certList.addAll(getCertificates(x509Data));
        }

        return certList;
    }

    /**
     * Get a list of the Java {@link java.security.cert.X509Certificate} within the given {@link X509Data}.
     * 
     * @param x509Data {@link X509Data} from which to extract the certificate
     * 
     * @return a list of Java {@link java.security.cert.X509Certificate}s
     * 
     * @throws CertificateException thrown if there is a problem converting the X509 data into
     *             {@link java.security.cert.X509Certificate}s.
     */
    @Nonnull public static List<X509Certificate> getCertificates(@Nullable final X509Data x509Data)
            throws CertificateException {
        final List<X509Certificate> certList = new LinkedList<>();

        if (x509Data == null) {
            return certList;
        }

        for (final org.opensaml.xmlsec.signature.X509Certificate xmlCert : x509Data.getX509Certificates()) {
            final X509Certificate newCert = getCertificate(xmlCert);
            if (newCert != null) {
                certList.add(newCert);
            }
        }

        return certList;
    }

    /**
     * Convert an {@link org.opensaml.xmlsec.signature.X509Certificate} into a native Java representation.
     * 
     * @param xmlCert an {@link org.opensaml.xmlsec.signature.X509Certificate}
     * 
     * @return a {@link java.security.cert.X509Certificate}
     * 
     * @throws CertificateException thrown if there is a problem converting the X509 data into
     *             {@link java.security.cert.X509Certificate}s.
     */
    @Nullable public static X509Certificate getCertificate(
            @Nullable final org.opensaml.xmlsec.signature.X509Certificate xmlCert) throws CertificateException {

        if (xmlCert == null || xmlCert.getValue() == null) {
            return null;
        }

        return X509Support.decodeCertificate(xmlCert.getValue());
    }

    /**
     * Get a list of the Java {@link java.security.cert.X509CRL}s within the given {@link KeyInfo}.
     * 
     * @param keyInfo the {@link KeyInfo} to extract the CRLs from
     * 
     * @return a list of Java {@link java.security.cert.X509CRL}s
     * 
     * @throws CRLException thrown if there is a problem converting the CRL data into {@link java.security.cert.X509CRL}
     *             s
     */
    @Nonnull public static List<X509CRL> getCRLs(@Nullable final KeyInfo keyInfo) throws CRLException {
        final List<X509CRL> crlList = new LinkedList<>();

        if (keyInfo == null) {
            return crlList;
        }

        final List<X509Data> x509Datas = keyInfo.getX509Datas();
        for (final X509Data x509Data : x509Datas) {
            crlList.addAll(getCRLs(x509Data));
        }

        return crlList;
    }

    /**
     * Get a list of the Java {@link java.security.cert.X509CRL}s within the given {@link X509Data}.
     * 
     * @param x509Data {@link X509Data} to extract the CRLs from
     * 
     * @return a list of Java {@link java.security.cert.X509CRL}s
     * 
     * @throws CRLException thrown if there is a problem converting the CRL data into {@link java.security.cert.X509CRL}
     *             s
     */
    @Nonnull public static List<X509CRL> getCRLs(@Nullable final X509Data x509Data) throws CRLException {
        final List<X509CRL> crlList = new LinkedList<>();

        if (x509Data == null) {
            return crlList;
        }

        for (final org.opensaml.xmlsec.signature.X509CRL xmlCRL : x509Data.getX509CRLs()) {
            final X509CRL newCRL = getCRL(xmlCRL);
            if (newCRL != null) {
                crlList.add(newCRL);
            }
        }

        return crlList;
    }

    /**
     * Convert an {@link org.opensaml.xmlsec.signature.X509CRL} into a native Java representation.
     * 
     * @param xmlCRL object to extract the CRL from
     * 
     * @return a native Java {@link java.security.cert.X509CRL} object
     * 
     * @throws CRLException thrown if there is a problem converting the CRL data into {@link java.security.cert.X509CRL}
     */
    @Nullable public static X509CRL getCRL(@Nullable final org.opensaml.xmlsec.signature.X509CRL xmlCRL)
            throws CRLException {

        if (xmlCRL == null || xmlCRL.getValue() == null) {
            return null;
        }

        try {
            return X509Support.decodeCRL(xmlCRL.getValue());
        } catch (final CertificateException e) {
            throw new CRLException("Certificate error attempting to decode CRL", e);
        }
    }

    /**
     * Converts a native Java {@link java.security.cert.X509Certificate} into the corresponding XMLObject and stores it
     * in a {@link KeyInfo} in the first {@link X509Data} element. The X509Data element will be created if necessary.
     * 
     * @param keyInfo the {@link KeyInfo} object into which to add the certificate
     * @param cert the Java {@link java.security.cert.X509Certificate} to add
     * @throws CertificateEncodingException thrown when there is an error converting the Java certificate representation
     *             to the XMLObject representation
     */
    public static void addCertificate(@Nonnull final KeyInfo keyInfo, @Nonnull final X509Certificate cert)
            throws CertificateEncodingException {
        Constraint.isNotNull(keyInfo, "KeyInfo cannot be null");
        
        final X509Data x509Data;
        if (keyInfo.getX509Datas().size() == 0) {
            final XMLObjectBuilder<X509Data> x509DataBuilder =
                    XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(
                            X509Data.DEFAULT_ELEMENT_NAME);
            x509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
            keyInfo.getX509Datas().add(x509Data);
        } else {
            x509Data = keyInfo.getX509Datas().get(0);
        }
        x509Data.getX509Certificates().add(buildX509Certificate(cert));
    }

    /**
     * Converts a native Java {@link java.security.cert.X509CRL} into the corresponding XMLObject and stores it in a
     * {@link KeyInfo} in the first {@link X509Data} element. The X509Data element will be created if necessary.
     * 
     * @param keyInfo the {@link KeyInfo} object into which to add the CRL
     * @param crl the Java {@link java.security.cert.X509CRL} to add
     * @throws CRLException thrown when there is an error converting the Java CRL representation to the XMLObject
     *             representation
     */
    public static void addCRL(@Nonnull final KeyInfo keyInfo, @Nonnull final X509CRL crl) throws CRLException {
        Constraint.isNotNull(keyInfo, "KeyInfo cannot be null");
        
        final X509Data x509Data;
        if (keyInfo.getX509Datas().size() == 0) {
            final XMLObjectBuilder<X509Data> x509DataBuilder =
                    XMLObjectProviderRegistrySupport.getBuilderFactory().<X509Data>getBuilderOrThrow(
                            X509Data.DEFAULT_ELEMENT_NAME);
            x509Data = x509DataBuilder.buildObject(X509Data.DEFAULT_ELEMENT_NAME);
            keyInfo.getX509Datas().add(x509Data);
        } else {
            x509Data = keyInfo.getX509Datas().get(0);
        }
        x509Data.getX509CRLs().add(buildX509CRL(crl));
    }

    /**
     * Builds an {@link org.opensaml.xmlsec.signature.X509Certificate} XMLObject from a native Java
     * {@link java.security.cert.X509Certificate}.
     * 
     * @param cert the Java {@link java.security.cert.X509Certificate} to convert
     * @return a {@link org.opensaml.xmlsec.signature.X509Certificate} XMLObject
     * @throws CertificateEncodingException thrown when there is an error converting the Java certificate representation
     *             to the XMLObject representation
     */
    @Nonnull public static org.opensaml.xmlsec.signature.X509Certificate
            buildX509Certificate(final X509Certificate cert)
            throws CertificateEncodingException {
        Constraint.isNotNull(cert, "X.509 certificate cannot be null");
        
        final XMLObjectBuilder<org.opensaml.xmlsec.signature.X509Certificate> xmlCertBuilder =
                    XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(
                            org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
        final org.opensaml.xmlsec.signature.X509Certificate xmlCert =
                xmlCertBuilder.buildObject(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
        xmlCert.setValue(Base64Support.encode(cert.getEncoded(), Base64Support.CHUNKED));

        return xmlCert;
    }

    /**
     * Builds an {@link org.opensaml.xmlsec.signature.X509CRL} XMLObject from a native Java
     * {@link java.security.cert.X509CRL}.
     * 
     * @param crl the Java {@link java.security.cert.X509CRL} to convert
     * @return a {@link org.opensaml.xmlsec.signature.X509CRL} XMLObject
     * @throws CRLException thrown when there is an error converting the Java CRL representation to the XMLObject
     *             representation
     */
    @Nonnull public static org.opensaml.xmlsec.signature.X509CRL buildX509CRL(final X509CRL crl) throws CRLException {
        Constraint.isNotNull(crl, "X.509 CRL cannot be null");
        
        final XMLObjectBuilder<org.opensaml.xmlsec.signature.X509CRL> xmlCRLBuilder =
                    XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(
                            org.opensaml.xmlsec.signature.X509CRL.DEFAULT_ELEMENT_NAME);
        final org.opensaml.xmlsec.signature.X509CRL xmlCRL =
                xmlCRLBuilder.buildObject(org.opensaml.xmlsec.signature.X509CRL.DEFAULT_ELEMENT_NAME);
        xmlCRL.setValue(Base64Support.encode(crl.getEncoded(), Base64Support.CHUNKED));

        return xmlCRL;
    }

    /**
     * Build an {@link X509SubjectName} containing a given subject name.
     * 
     * @param subjectName the name content
     * @return the new X509SubjectName
     */
    @Nonnull public static X509SubjectName buildX509SubjectName(@Nullable final String subjectName) {
        final XMLObjectBuilder<X509SubjectName> xmlSubjectNameBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(
                        X509SubjectName.DEFAULT_ELEMENT_NAME);
        final X509SubjectName xmlSubjectName = xmlSubjectNameBuilder.buildObject(X509SubjectName.DEFAULT_ELEMENT_NAME);
        xmlSubjectName.setValue(subjectName);
        return xmlSubjectName;
    }

    /**
     * Build an {@link X509IssuerSerial} containing a given issuer name and serial number.
     * 
     * @param issuerName the name content
     * @param serialNumber the serial number content
     * @return the new X509IssuerSerial
     */
    @Nonnull public static X509IssuerSerial buildX509IssuerSerial(@Nullable final String issuerName,
            @Nullable final BigInteger serialNumber) {
        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        
        final XMLObjectBuilder<X509IssuerName> xmlIssuerNameBuilder =
                builderFactory.getBuilderOrThrow(X509IssuerName.DEFAULT_ELEMENT_NAME);
        final X509IssuerName xmlIssuerName = xmlIssuerNameBuilder.buildObject(X509IssuerName.DEFAULT_ELEMENT_NAME);
        xmlIssuerName.setValue(issuerName);

        final XMLObjectBuilder<X509SerialNumber> xmlSerialNumberBuilder =
                builderFactory.getBuilderOrThrow(X509SerialNumber.DEFAULT_ELEMENT_NAME);
        final X509SerialNumber xmlSerialNumber =
                xmlSerialNumberBuilder.buildObject(X509SerialNumber.DEFAULT_ELEMENT_NAME);
        xmlSerialNumber.setValue(serialNumber);

        final XMLObjectBuilder<X509IssuerSerial> xmlIssuerSerialBuilder =
                builderFactory.getBuilderOrThrow(X509IssuerSerial.DEFAULT_ELEMENT_NAME);
        final X509IssuerSerial xmlIssuerSerial =
                xmlIssuerSerialBuilder.buildObject(X509IssuerSerial.DEFAULT_ELEMENT_NAME);
        xmlIssuerSerial.setX509IssuerName(xmlIssuerName);
        xmlIssuerSerial.setX509SerialNumber(xmlSerialNumber);

        return xmlIssuerSerial;
    }

    /**
     * Build an {@link X509SKI} containing the subject key identifier extension value contained within a certificate.
     * 
     * @param javaCert the Java X509Certificate from which to extract the subject key identifier value.
     * @return a new X509SKI object, or null if the certificate did not contain the subject key identifier extension
     */
    @Nullable public static X509SKI buildX509SKI(@Nonnull final X509Certificate javaCert) {
        final byte[] skiPlainValue = X509Support.getSubjectKeyIdentifier(javaCert);
        if (skiPlainValue == null || skiPlainValue.length == 0) {
            return null;
        }

        final XMLObjectBuilder<X509SKI> xmlSKIBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(X509SKI.DEFAULT_ELEMENT_NAME);
        final X509SKI xmlSKI = xmlSKIBuilder.buildObject(X509SKI.DEFAULT_ELEMENT_NAME);
        xmlSKI.setValue(Base64Support.encode(skiPlainValue, Base64Support.CHUNKED));

        return xmlSKI;
    }

    /**
     * Build an {@link X509Digest} containing the digest of the specified certificate.
     * 
     * @param javaCert the Java X509Certificate to digest
     * @param algorithmURI  digest algorithm URI
     * @return a new X509Digest object
     * @throws NoSuchAlgorithmException if the algorithm specified cannot be used
     * @throws CertificateEncodingException if the certificate cannot be encoded
     */
    @Nonnull public static X509Digest buildX509Digest(@Nonnull final X509Certificate javaCert,
            @Nonnull final String algorithmURI) throws NoSuchAlgorithmException, CertificateEncodingException {
        Constraint.isNotNull(javaCert, "Certificate cannot be null");

        final String jceAlg = AlgorithmSupport.getAlgorithmID(algorithmURI);
        if (jceAlg == null) {
            throw new NoSuchAlgorithmException("No JCE algorithm found for " + algorithmURI);
        }
        final MessageDigest md = MessageDigest.getInstance(jceAlg);
        final byte[] hash = md.digest(javaCert.getEncoded());
        
        final XMLObjectBuilder<X509Digest> builder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(X509Digest.DEFAULT_ELEMENT_NAME);
        final X509Digest xmlDigest = builder.buildObject(X509Digest.DEFAULT_ELEMENT_NAME);
        xmlDigest.setAlgorithm(algorithmURI);
        xmlDigest.setValue(Base64Support.encode(hash, Base64Support.CHUNKED));
        
        return xmlDigest;
    }    
    
    /**
     * Converts a Java DSA or RSA public key into the corresponding XMLObject and stores it in a {@link KeyInfo} in a
     * new {@link KeyValue} element.
     * 
     * As input, only supports {@link PublicKey}s which are instances of either
     * {@link java.security.interfaces.DSAPublicKey} or {@link java.security.interfaces.RSAPublicKey}
     * 
     * @param keyInfo the {@link KeyInfo} element to which to add the key
     * @param pk the native Java {@link PublicKey} to add
     */
    public static void addPublicKey(@Nonnull final KeyInfo keyInfo, @Nullable final PublicKey pk) {
        Constraint.isNotNull(keyInfo, "KeyInfo cannot be null");
        
        final XMLObjectBuilder<KeyValue> keyValueBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(KeyValue.DEFAULT_ELEMENT_NAME);
        final KeyValue keyValue = keyValueBuilder.buildObject(KeyValue.DEFAULT_ELEMENT_NAME);

        // TODO handle ECKeyValue
        
        if (pk instanceof RSAPublicKey) {
            keyValue.setRSAKeyValue(buildRSAKeyValue((RSAPublicKey) pk));
        } else if (pk instanceof DSAPublicKey) {
            keyValue.setDSAKeyValue(buildDSAKeyValue((DSAPublicKey) pk));
        } else {
            throw new IllegalArgumentException("Only RSAPublicKey and DSAPublicKey are supported");
        }

        keyInfo.getKeyValues().add(keyValue);
    }

    /**
     * Builds an {@link RSAKeyValue} XMLObject from the Java security RSA public key type.
     * 
     * @param rsaPubKey a native Java {@link RSAPublicKey}
     * @return an {@link RSAKeyValue} XMLObject
     */
    @Nonnull public static RSAKeyValue buildRSAKeyValue(@Nonnull final RSAPublicKey rsaPubKey) {
        Constraint.isNotNull(rsaPubKey, "RSA public key cannot be null");
        
        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        
        final XMLObjectBuilder<RSAKeyValue> rsaKeyValueBuilder =
                builderFactory.getBuilderOrThrow(RSAKeyValue.DEFAULT_ELEMENT_NAME);
        final RSAKeyValue rsaKeyValue = rsaKeyValueBuilder.buildObject(RSAKeyValue.DEFAULT_ELEMENT_NAME);
        
        final XMLObjectBuilder<Modulus> modulusBuilder = builderFactory.getBuilderOrThrow(Modulus.DEFAULT_ELEMENT_NAME);
        final Modulus modulus = modulusBuilder.buildObject(Modulus.DEFAULT_ELEMENT_NAME);
        
        final XMLObjectBuilder<Exponent> exponentBuilder =
                builderFactory.getBuilderOrThrow(Exponent.DEFAULT_ELEMENT_NAME);
        final Exponent exponent = exponentBuilder.buildObject(Exponent.DEFAULT_ELEMENT_NAME);

        modulus.setValueBigInt(rsaPubKey.getModulus());
        rsaKeyValue.setModulus(modulus);

        exponent.setValueBigInt(rsaPubKey.getPublicExponent());
        rsaKeyValue.setExponent(exponent);

        return rsaKeyValue;
    }

    /**
     * Builds a {@link DSAKeyValue} XMLObject from the Java security DSA public key type.
     * 
     * @param dsaPubKey a native Java {@link DSAPublicKey}
     * @return an {@link DSAKeyValue} XMLObject
     */
    @Nonnull public static DSAKeyValue buildDSAKeyValue(@Nonnull final DSAPublicKey dsaPubKey) {
        Constraint.isNotNull(dsaPubKey, "DSA public key cannot be null");
        
        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

        final XMLObjectBuilder<DSAKeyValue> dsaKeyValueBuilder =
                builderFactory.getBuilderOrThrow(DSAKeyValue.DEFAULT_ELEMENT_NAME);
        final DSAKeyValue dsaKeyValue = dsaKeyValueBuilder.buildObject(DSAKeyValue.DEFAULT_ELEMENT_NAME);

        final XMLObjectBuilder<Y> yBuilder = builderFactory.getBuilderOrThrow(Y.DEFAULT_ELEMENT_NAME);
        final XMLObjectBuilder<G> gBuilder = builderFactory.getBuilderOrThrow(G.DEFAULT_ELEMENT_NAME);
        final XMLObjectBuilder<P> pBuilder = builderFactory.getBuilderOrThrow(P.DEFAULT_ELEMENT_NAME);
        final XMLObjectBuilder<Q> qBuilder = builderFactory.getBuilderOrThrow(Q.DEFAULT_ELEMENT_NAME);
        
        final Y y = yBuilder.buildObject(Y.DEFAULT_ELEMENT_NAME);
        final G g = gBuilder.buildObject(G.DEFAULT_ELEMENT_NAME);
        final P p = pBuilder.buildObject(P.DEFAULT_ELEMENT_NAME);
        final Q q = qBuilder.buildObject(Q.DEFAULT_ELEMENT_NAME);

        y.setValueBigInt(dsaPubKey.getY());
        dsaKeyValue.setY(y);

        g.setValueBigInt(dsaPubKey.getParams().getG());
        dsaKeyValue.setG(g);

        p.setValueBigInt(dsaPubKey.getParams().getP());
        dsaKeyValue.setP(p);

        q.setValueBigInt(dsaPubKey.getParams().getQ());
        dsaKeyValue.setQ(q);

        return dsaKeyValue;
    }

    /**
     * Converts a Java public key into the corresponding XMLObject and stores it in a {@link KeyInfo} in a
     * new {@link DEREncodedKeyValue} element.
     * 
     * @param keyInfo the {@link KeyInfo} element to which to add the key
     * @param pk the native Java {@link PublicKey} to convert
     * @throws NoSuchAlgorithmException if the key type is unsupported
     * @throws InvalidKeySpecException if the key type does not support X.509 SPKI encoding
     */
    public static void addDEREncodedPublicKey(@Nonnull final KeyInfo keyInfo,
            @Nonnull final PublicKey pk) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Constraint.isNotNull(keyInfo, "KeyInfo cannot be null");
        Constraint.isNotNull(pk, "Public key cannot be null");
        
        final XMLObjectBuilder<DEREncodedKeyValue> builder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(
                        DEREncodedKeyValue.DEFAULT_ELEMENT_NAME);
        final DEREncodedKeyValue keyValue = builder.buildObject(DEREncodedKeyValue.DEFAULT_ELEMENT_NAME);
        
        final KeyFactory keyFactory = KeyFactory.getInstance(pk.getAlgorithm());
        final X509EncodedKeySpec keySpec = keyFactory.getKeySpec(pk, X509EncodedKeySpec.class);
        keyValue.setValue(Base64Support.encode(keySpec.getEncoded(), Base64Support.CHUNKED));
        
        keyInfo.getDEREncodedKeyValues().add(keyValue);
    }
    
    /**
     * Extracts all the public keys within the given {@link KeyInfo}'s {@link KeyValue}s and
     * {@link DEREncodedKeyValue}s.
     * 
     * @param keyInfo {@link KeyInfo} to extract the keys out of
     * 
     * @return a list of native Java {@link PublicKey} objects
     * 
     * @throws KeyException thrown if the given key data can not be converted into {@link PublicKey}
     */
    @Nonnull public static List<PublicKey> getPublicKeys(@Nullable final KeyInfo keyInfo) throws KeyException {

        // TODO support ECKeyValue and DEREncodedKeyValue

        final List<PublicKey> keys = new LinkedList<>();

        if (keyInfo == null) {
            return keys;
        }

        for (final KeyValue keyDescriptor : keyInfo.getKeyValues()) {
            final PublicKey newKey = getKey(keyDescriptor);
            if (newKey != null) {
                keys.add(newKey);
            }
        }

        for (final DEREncodedKeyValue keyDescriptor : keyInfo.getDEREncodedKeyValues()) {
            final PublicKey newKey = getKey(keyDescriptor);
            if (newKey != null) {
                keys.add(newKey);
            }
        }        
        return keys;
    }

    /**
     * Extracts the DSA or RSA public key within the {@link KeyValue}.
     * 
     * @param keyValue the {@link KeyValue} to extract the key from
     * 
     * @return a native Java security {@link java.security.Key} object
     * 
     * @throws KeyException thrown if the given key data can not be converted into {@link PublicKey}
     */
    @Nullable public static PublicKey getKey(@Nonnull final KeyValue keyValue) throws KeyException {
        Constraint.isNotNull(keyValue, "KeyValue cannot be null");

        if (keyValue.getDSAKeyValue() != null) {
            return getDSAKey(keyValue.getDSAKeyValue());
        } else if (keyValue.getRSAKeyValue() != null) {
            return getRSAKey(keyValue.getRSAKeyValue());
        } else {
            return null;
        }
    }

    /**
     * Builds an DSA key from a {@link DSAKeyValue} element. The element must contain values for all required DSA public
     * key parameters, including values for shared key family values P, Q and G.
     * 
     * @param keyDescriptor the {@link DSAKeyValue} key descriptor
     * 
     * @return a new {@link DSAPublicKey} instance of {@link PublicKey}
     * 
     * @throws KeyException thrown if the key algorithm is not supported by the JCE or the key spec does not contain
     *             valid information
     */
    @Nonnull public static PublicKey getDSAKey(@Nonnull final DSAKeyValue keyDescriptor) throws KeyException {
        if (!hasCompleteDSAParams(keyDescriptor)) {
            throw new KeyException("DSAKeyValue element did not contain at least one of DSA parameters P, Q or G");
        }

        final BigInteger gComponent = keyDescriptor.getG().getValueBigInt();
        final BigInteger pComponent = keyDescriptor.getP().getValueBigInt();
        final BigInteger qComponent = keyDescriptor.getQ().getValueBigInt();

        final DSAParams dsaParams = new DSAParameterSpec(pComponent, qComponent, gComponent);
        return getDSAKey(keyDescriptor, dsaParams);
    }

    /**
     * Builds a DSA key from an {@link DSAKeyValue} element and the supplied Java {@link DSAParams}, which supplies key
     * material from a shared key family.
     * 
     * @param keyDescriptor the {@link DSAKeyValue} key descriptor
     * @param dsaParams the {@link DSAParams} DSA key family parameters
     * 
     * @return a new {@link DSAPublicKey} instance of {@link PublicKey}
     * 
     * @throws KeyException thrown if the key algorithm is not supported by the JCE or the key spec does not contain
     *             valid information
     */
    @Nonnull public static PublicKey getDSAKey(@Nonnull final DSAKeyValue keyDescriptor,
            @Nonnull final DSAParams dsaParams) throws KeyException {
        Constraint.isNotNull(keyDescriptor, "DSAKeyValue cannot be null");
        Constraint.isNotNull(dsaParams, "DSAParams cannot be null");
        
        final BigInteger yComponent = keyDescriptor.getY().getValueBigInt();

        final DSAPublicKeySpec keySpec =
                new DSAPublicKeySpec(yComponent, dsaParams.getP(), dsaParams.getQ(), dsaParams.getG());
        return buildKey(keySpec, "DSA");
    }

    /**
     * Check whether the specified {@link DSAKeyValue} element has the all optional DSA values which can be shared
     * amongst many keys in a DSA "key family", and are presumed to be known from context.
     * 
     * @param keyDescriptor the {@link DSAKeyValue} element to check
     * @return true if all parameters are present and non-empty, false otherwise
     */
    public static boolean hasCompleteDSAParams(@Nullable final DSAKeyValue keyDescriptor) {
        if (keyDescriptor == null
                || keyDescriptor.getG() == null || Strings.isNullOrEmpty(keyDescriptor.getG().getValue())
                || keyDescriptor.getP() == null || Strings.isNullOrEmpty(keyDescriptor.getP().getValue())
                || keyDescriptor.getQ() == null || Strings.isNullOrEmpty(keyDescriptor.getQ().getValue())) {
            return false;
        }
        return true;
    }

    /**
     * Builds an RSA key from an {@link RSAKeyValue} element.
     * 
     * @param keyDescriptor the {@link RSAKeyValue} key descriptor
     * 
     * @return a new {@link RSAPublicKey} instance of {@link PublicKey}
     * 
     * @throws KeyException thrown if the key algorithm is not supported by the JCE or the key spec does not contain
     *             valid information
     */
    @Nonnull public static PublicKey getRSAKey(@Nonnull final RSAKeyValue keyDescriptor) throws KeyException {
        Constraint.isNotNull(keyDescriptor, "RSAKeyValue cannot be null");
        
        final BigInteger modulus = keyDescriptor.getModulus().getValueBigInt();
        final BigInteger exponent = keyDescriptor.getExponent().getValueBigInt();

        final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        return buildKey(keySpec, "RSA");
    }

    /**
     * Decode a base64-encoded ds:CryptoBinary value to a native Java BigInteger type.
     * 
     * @param base64Value base64-encoded CryptoBinary value
     * @return the decoded BigInteger
     */
    @Nonnull public static final BigInteger decodeBigIntegerFromCryptoBinary(@Nonnull final String base64Value) {
        return new BigInteger(1, Base64Support.decode(base64Value));
    }

    /**
     * Encode a native Java BigInteger type to a base64-encoded ds:CryptoBinary value.
     * 
     * @param bigInt the BigInteger value
     * @return the encoded CryptoBinary value
     */
    @Nonnull public static final String encodeCryptoBinaryFromBigInteger(@Nonnull final BigInteger bigInt) {
        Constraint.isNotNull(bigInt, "BigInteger cannot be null");
        
        // This code is really complicated, for now just use the Apache xmlsec lib code directly.
        final byte[] bigIntBytes = org.apache.xml.security.utils.Base64.encode(bigInt, bigInt.bitLength());
        return Base64Support.encode(bigIntBytes, Base64Support.UNCHUNKED);
    }

    /**
     * Generates a public key from the given key spec.
     * 
     * @param keySpec {@link KeySpec} specification for the key
     * @param keyAlgorithm key generation algorithm, only DSA and RSA supported
     * 
     * @return the generated {@link PublicKey}
     * 
     * @throws KeyException thrown if the key algorithm is not supported by the JCE or the key spec does not contain
     *             valid information
     */
    @Nonnull protected static PublicKey buildKey(@Nonnull final KeySpec keySpec, @Nonnull final String keyAlgorithm)
            throws KeyException {
        final Logger log = getLogger();
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
            return keyFactory.generatePublic(keySpec);
        } catch (final NoSuchAlgorithmException e) {
            final String msg = keyAlgorithm + " algorithm is not supported by this JCE"; 
            log.error(msg, e);
            throw new KeyException(msg, e);
        } catch (final InvalidKeySpecException e) {
            log.error("Invalid key information", e);
            throw new KeyException("Invalid key information", e);
        }
    }

    /**
     * Extracts the public key within the {@link DEREncodedKeyValue}.
     * 
     * @param keyValue the {@link DEREncodedKeyValue} to extract the key from
     * 
     * @return a native Java security {@link java.security.Key} object
     * 
     * @throws KeyException thrown if the given key data can not be converted into {@link PublicKey}
     */
    @Nonnull public static PublicKey getKey(@Nonnull final DEREncodedKeyValue keyValue) throws KeyException{
        final String[] supportedKeyTypes = { "RSA", "DSA", "EC"};
        
        Constraint.isNotNull(keyValue, "DEREncodedKeyValue cannot be null");
        if (keyValue.getValue() == null) {
            throw new KeyException("No data found in key value element");
        }
        final byte[] encodedKey = Base64Support.decode(keyValue.getValue());

        // Iterate over the supported key types until one produces a public key.
        for (final String keyType : supportedKeyTypes) {
            try {
                final KeyFactory keyFactory = KeyFactory.getInstance(keyType);
                final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
                final PublicKey publicKey = keyFactory.generatePublic(keySpec);
                if (publicKey != null) {
                    return publicKey;
                }
            } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
                // Do nothing, try the next type
            }
        }
        throw new KeyException("DEREncodedKeyValue did not contain a supported key type");
    }
    
    /**
     * Get the Java certificate factory singleton.
     * 
     * @return {@link CertificateFactory} the factory used to create X509 certificate objects
     * 
     * @throws CertificateException thrown if the factory can not be created
     */
    @Nonnull protected static CertificateFactory getX509CertFactory() throws CertificateException {

        if (x509CertFactory == null) {
            x509CertFactory = CertificateFactory.getInstance("X.509");
        }

        return x509CertFactory;
    }

    /**
     * Obtains a {@link KeyInfoGenerator} for the specified {@link Credential}.
     * 
     * <p>
     * The KeyInfoGenerator returned is resolved via the supplied {@link NamedKeyInfoGeneratorManager}
     * and is determined by the type of the signing credential and an optional KeyInfo generator profile configuration 
     * name. If the latter is ommited, the default manager ({@link NamedKeyInfoGeneratorManager#getDefaultManager()}) 
     * of the security configuration's named generator manager will be used.
     * </p>
     * 
     * @param credential the credential for which a generator is desired
     * @param manager the NamedKeyInfoGeneratorManager instance to use
     * @param keyInfoProfileName the named KeyInfoGeneratorManager configuration to use (may be null)
     * @return a KeyInfoGenerator appropriate for the specified credential
     */
    @Nullable public static KeyInfoGenerator getKeyInfoGenerator(@Nonnull final Credential credential,
            @Nonnull final NamedKeyInfoGeneratorManager manager, @Nullable final String keyInfoProfileName) {
        Constraint.isNotNull(credential, "Credential may not be null");
        Constraint.isNotNull(manager, "NamedKeyInfoGeneratorManager may not be null");
        
        final Logger log = getLogger();
    
        KeyInfoGeneratorFactory factory = null;
        if (keyInfoProfileName != null) {
            log.trace("Resolving KeyInfoGeneratorFactory using profile name: {}", keyInfoProfileName);
            factory = manager.getFactory(keyInfoProfileName, credential);
        } else {
            log.trace("Resolving KeyInfoGeneratorFactory using default manager: {}", keyInfoProfileName);
            factory = manager.getDefaultManager().getFactory(credential);
        }
        
        if (factory != null) {
            log.trace("Found KeyInfoGeneratorFactory: {}", factory.getClass().getName());
            return factory.newInstance();
        }
        
        log.trace("Unable to resolve KeyInfoGeneratorFactory for credential");
        return null;
    }
    
    /**
     * Get an SLF4J Logger.
     * 
     * @return a Logger instance
     */
    @Nonnull private static Logger getLogger() {
        return LoggerFactory.getLogger(KeyInfoSupport.class);
    }

}