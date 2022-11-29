/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.security.Key;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.jwt.JwtUtils;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.storage.StorageFactory;

public class TokenResponseValidator {

    public static final TraceComponent tc = Tr.register(TokenResponseValidator.class);

    OidcClientConfig clientConfig;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Storage storage;

    public TokenResponseValidator(OidcClientConfig oidcClientConfig) {
        this.clientConfig = oidcClientConfig;
    }

    @FFDCIgnore({ TokenValidationException.class, Exception.class })
    public JwtClaims validate(TokenResponse tokenResponse) throws TokenValidationException {
        String idtoken = null;
        if (tokenResponse != null) {
            idtoken = tokenResponse.getIdTokenString();
        }
        if (idtoken == null) {
            return new JwtClaims();
        }

        JwtContext jwtcontext = getJwtContextForIdToken(idtoken);
        JwtClaims jwtClaims = getJwtClaimsFromIdTokenContext(jwtcontext);

        String clientSecret = null;
        ProtectedString clientSecretProtectedString = clientConfig.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }
        try {
            String issuerconfigured = validateIdTokenClaimsAndGetIssuer(jwtClaims, clientSecret);
            return validateIdTokenFormat(idtoken, jwtcontext, clientSecret, issuerconfigured);
        } catch (TokenValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenValidationException(this.clientConfig.getClientId(), e.toString());
        }
    }

    @FFDCIgnore(InvalidJwtException.class)
    JwtContext getJwtContextForIdToken(String idTokenString) throws TokenValidationException {
        JwtContext jwtContext = null;
        try {
            jwtContext = JwtParsingUtils.parseJwtWithoutValidation(idTokenString);
        } catch (InvalidJwtException e) {
            throw new TokenValidationException(this.clientConfig.getClientId(), e.toString());
        }
        return jwtContext;
    }

    JwtClaims getJwtClaimsFromIdTokenContext(JwtContext jwtContext) throws TokenValidationException {
        JwtClaims jwtClaims = null;
        if (jwtContext != null) {
            jwtClaims = jwtContext.getJwtClaims();
        }
        if (jwtContext == null || jwtClaims == null) {
            // TODO - NLS message
            throw new TokenValidationException(this.clientConfig.getClientId(), "not a valid token to continue the flow");
        }
        return jwtClaims;
    }

    String validateIdTokenClaimsAndGetIssuer(JwtClaims jwtClaims,
                                             @Sensitive String clientSecret) throws MalformedClaimException, TokenValidationException, OidcDiscoveryException, OidcClientConfigurationException {
        // must have claims - iat and exp
        if (jwtClaims.getIssuedAt() == null) {
            String nlsMsg = Tr.formatMessage(tc, "TOKEN_MISSING_REQUIRED_CLAIM", "iat");
            throw new TokenValidationException(this.clientConfig.getClientId(), nlsMsg);
        }
        if (jwtClaims.getExpirationTime() == null) {
            String nlsMsg = Tr.formatMessage(tc, "TOKEN_MISSING_REQUIRED_CLAIM", "exp");
            throw new TokenValidationException(this.clientConfig.getClientId(), nlsMsg);
        }
        TokenValidator tokenValidator = new IdTokenValidator(clientConfig);
        String issuerconfigured = MetadataUtils.getIssuer(clientConfig);
        tokenValidator.issuer(jwtClaims.getIssuer()).subject(jwtClaims.getSubject()).audiences(jwtClaims.getAudience()).azp(((String) jwtClaims.getClaimValue("azp"))).iat(jwtClaims.getIssuedAt()).exp(jwtClaims.getExpirationTime()).nbf(jwtClaims.getNotBefore()).issuerconfigured(issuerconfigured);
        tokenValidator.validate();
        if (this.clientConfig.isUseNonce()) {
            ((IdTokenValidator) tokenValidator).nonce(((String) jwtClaims.getClaimValue("nonce")));
            ((IdTokenValidator) tokenValidator).state(getStateParameter());
            ((IdTokenValidator) tokenValidator).secret(clientSecret);
            storage = StorageFactory.instantiateStorage(request, response, clientConfig.isUseSession());
            ((IdTokenValidator) tokenValidator).storage(storage);

            ((IdTokenValidator) tokenValidator).validateNonce();
        }
        return issuerconfigured;
    }

    JwtClaims validateIdTokenFormat(String idtoken, JwtContext jwtcontext, String clientSecret, String issuerconfigured) throws Exception {
        JsonWebStructure jws = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtcontext);
        Key verificationKey = JwtUtils.getJwsVerificationKey(jws, clientConfig);

        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        builder.setJwsAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST, MetadataUtils.getIdTokenSigningAlgorithmsSupported(clientConfig)));
        builder.setRequireExpirationTime().setAllowedClockSkewInSeconds(120).setExpectedAudience(clientConfig.getClientId()).setExpectedIssuer(false,
                                                                                                                                               issuerconfigured).setRequireSubject().setSkipDefaultAudienceValidation().setVerificationKey(verificationKey).setRelaxVerificationKeyValidation();

        JwtConsumer jwtConsumer = builder.build();
        return JwtParsingUtils.parseJwtWithValidation(idtoken, jwtConsumer);
    }

    public String getStateParameter() {
        return request.getParameter(AuthorizationRequestParameters.STATE);
        //TODO: maybe throw an exception right here if this state is not valid
    }

    /**
     * @param request
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * @param response
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

}
