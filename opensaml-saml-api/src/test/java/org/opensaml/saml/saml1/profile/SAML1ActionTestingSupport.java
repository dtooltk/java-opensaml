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

package org.opensaml.saml.saml1.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.AttributeQuery;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.Subject;

/**
 * Helper methods for creating/testing SAML 1 objects within profile action tests. When methods herein refer to mock
 * objects they are always objects that have been created via Mockito unless otherwise noted.
 */
public class SAML1ActionTestingSupport {

    /** ID used for all generated {@link Response} objects. */
    public final static String REQUEST_ID = "request";

    /** ID used for all generated {@link Response} objects. */
    public final static String RESPONSE_ID = "response";

    /** ID used for all generated {@link Assertion} objects. */
    public final static String ASSERTION_ID = "assertion";

    /**
     * Builds an empty response. The ID of the message is {@link SamlActionTestingSupport#OUTBOUND_MSG_ID}, the issues
     * instant is 1970-01-01T00:00:00Z and the SAML version is {@link SAMLVersion#VERSION_11}.
     * 
     * @return the constructed response
     */
    @Nonnull public static Response buildResponse() {
        final SAMLObjectBuilder<Response> responseBuilder =
                (SAMLObjectBuilder<Response>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Response.DEFAULT_ELEMENT_NAME);

        final Response response = responseBuilder.buildObject();
        response.setID(ActionTestingSupport.OUTBOUND_MSG_ID);
        response.setIssueInstant(new DateTime(0));
        response.setVersion(SAMLVersion.VERSION_11);

        return response;
    }

    /**
     * Builds an empty assertion. The ID of the message is {@link #ASSERTION_ID}, the issues instant is
     * 1970-01-01T00:00:00Z and the SAML version is {@link SAMLVersion#VERSION_11}.
     * 
     * @return the constructed assertion
     */
    @Nonnull public static Assertion buildAssertion() {
        final SAMLObjectBuilder<Assertion> assertionBuilder =
                (SAMLObjectBuilder<Assertion>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Assertion.TYPE_NAME);

        final Assertion assertion = assertionBuilder.buildObject();
        assertion.setID(ASSERTION_ID);
        assertion.setIssueInstant(new DateTime(0));
        assertion.setVersion(SAMLVersion.VERSION_11);

        return assertion;
    }

    /**
     * Builds a {@link Subject}. If a principal name is given a {@link NameIdentifier}, whose value is the given
     * principal name, will be created and added to the {@link Subject}.
     * 
     * @param principalName the principal name to add to the subject
     * 
     * @return the built subject
     */
    @Nonnull public static Subject buildSubject(final @Nullable String principalName) {
        final SAMLObjectBuilder<Subject> subjectBuilder =
                (SAMLObjectBuilder<Subject>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Subject.TYPE_NAME);
        final Subject subject = subjectBuilder.buildObject();

        if (principalName != null) {
            final SAMLObjectBuilder<NameIdentifier> nameIdBuilder =
                    (SAMLObjectBuilder<NameIdentifier>) XMLObjectProviderRegistrySupport.getBuilderFactory()
                            .getBuilder(NameIdentifier.TYPE_NAME);
            final NameIdentifier nameId = nameIdBuilder.buildObject();
            nameId.setNameIdentifier(principalName);
            subject.setNameIdentifier(nameId);
        }

        return subject;
    }

    /**
     * Builds a {@link Request} containing an {@link AttributeQuery}. If a {@link Subject} is given, it will be added to
     * the constructed {@link AttributeQuery}.
     * 
     * @param subject the subject to add to the query
     * 
     * @return the built query
     */
    @Nonnull public static Request buildAttributeQueryRequest(final @Nullable Subject subject) {
        final SAMLObjectBuilder<AttributeQuery> queryBuilder =
                (SAMLObjectBuilder<AttributeQuery>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        AttributeQuery.TYPE_NAME);
        final AttributeQuery query = queryBuilder.buildObject();

        if (subject != null) {
            query.setSubject(subject);
        }

        SAMLObjectBuilder<Request> requestBuilder =
                (SAMLObjectBuilder<Request>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Request.DEFAULT_ELEMENT_NAME);
        Request request = requestBuilder.buildObject();
        request.setID(REQUEST_ID);
        request.setIssueInstant(new DateTime(0));
        request.setQuery(query);
        request.setVersion(SAMLVersion.VERSION_11);

        return request;
    }
}