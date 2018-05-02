/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.compliance.verification.core.responses

import com.jayway.restassured.response.Response
import org.codice.compliance.SAMLComplianceException
import org.codice.compliance.SAMLCore_3_4_1_4_a
import org.codice.compliance.SAMLCore_3_4_1_4_c
import org.codice.compliance.SAMLCore_3_4_1_4_d
import org.codice.compliance.SAMLCore_3_4_1_a
import org.codice.compliance.SAMLCore_3_4_a
import org.codice.compliance.children
import org.codice.compliance.recursiveChildren
import org.codice.compliance.utils.TestCommon.Companion.ASSERTION
import org.codice.compliance.utils.TestCommon.Companion.AUDIENCE
import org.codice.compliance.utils.TestCommon.Companion.AUTHN_STATEMENT
import org.codice.compliance.utils.TestCommon.Companion.SP_ENTITY_INFO
import org.codice.compliance.utils.TestCommon.Companion.SP_ISSUER
import org.codice.compliance.utils.getLocation
import org.codice.compliance.verification.core.NameIDPolicyVerifier
import org.codice.compliance.verification.core.ResponseVerifier
import org.opensaml.saml.saml2.core.AuthnRequest
import org.w3c.dom.Node

class CoreAuthnRequestProtocolVerifier(private val authnRequest: AuthnRequest,
                                       samlResponseDom: Node) :
        ResponseVerifier(authnRequest, samlResponseDom) {

    private val nameIdPolicyVerifier =
            authnRequest.nameIDPolicy?.let { NameIDPolicyVerifier(samlResponseDom, it) }

    /** 3.4 Authentication Request Protocol **/
    override fun verify() {
        super.verify()
        verifyAuthnRequestProtocolResponse()
        // TODO When DDF is fixed to return NameID format based on NameIDPolicy, uncomment this line
//        nameIdPolicyVerifier?.apply { verify() }
    }

    fun verifyAssertionConsumerService(httpResponse: Response) {
        val expectedACS = SP_ENTITY_INFO.getAssertionConsumerService(authnRequest, null,
                authnRequest.assertionConsumerServiceIndex).url
        val actualACS = httpResponse.getLocation()

        if (actualACS == null || actualACS != expectedACS)
            throw SAMLComplianceException.create(SAMLCore_3_4_1_a,
                    message = "The URL at which the Response was received [$actualACS] does not" +
                            " match the expected ACS URL [$expectedACS] based on the request.")
    }

    private fun verifyAuthnRequestProtocolResponse() {
        val assertions = samlResponseDom.children(ASSERTION)

        if (samlResponseDom.localName != "Response" || assertions.isEmpty())
            throw SAMLComplianceException.create(SAMLCore_3_4_1_4_a,
                    message = "Did not find Response elements with one or more Assertion elements.",
                    node = samlResponseDom)

        if (assertions.all { it.children(AUTHN_STATEMENT).isEmpty() })
            throw SAMLComplianceException.create(SAMLCore_3_4_a, SAMLCore_3_4_1_4_c,
                    message = "AuthnStatement not found in any of the Assertions.",
                    node = samlResponseDom)

        if (assertions.any {
                    it.recursiveChildren("AudienceRestriction").flatMap { it.children(AUDIENCE) }
                            .none { it.textContent == SP_ISSUER }
                })
            throw SAMLComplianceException.create(SAMLCore_3_4_1_4_d,
                    message = "Assertion found without an AudienceRestriction referencing the " +
                            "requester.",
                    node = samlResponseDom)
    }

    override fun verifyEncryptedElements() {
        nameIdPolicyVerifier?.apply { verifyEncryptedIds() }
    }
}