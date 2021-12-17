/*
 * Copyright 2011-2021 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.workflowExecution.wes.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService

/**
 * Service to authenticate on the WESkit API authorization server via oAuth2.
 * This service is a stateful singleton class and handles the caching of oAuth2 access tokens.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WesAuthService {

    @Autowired
    ConfigService configService

    private OAuth2AccessToken oAuth2AccessToken

    /**
     * Get the cached OAuth2AccessToken if it is available. This token will be refreshed automatically when the
     * access token is expired. The expiration date can be set on the oAuth2 authorization server (KeyCloak).
     *
     * @return OAuth2AccessToken
     */
    OAuth2AccessToken getAccessToken() {
        if (!oAuth2AccessToken || oAuth2AccessToken.expired) {
            oAuth2AccessToken = new OAuth2RestTemplate(oAuthConfigDetails()).accessToken
        }

        return oAuth2AccessToken
    }

    /**
     * Generate an oAuth2 configuration which contains all required request parameters to fetch a
     * Bearer token from the WESkit authorization server.
     *
     * This config is built to work with the oAuth2 client credentials grant method.
     *
     * The following parameters are required and should be configured in the .otp.properties
     *  - baseUrl
     *  - clientId
     *  - clientSecret
     *
     * @return ClientCredentialsResourceDetails, which contains the oAuth2 configuration
     */
    private ClientCredentialsResourceDetails oAuthConfigDetails() {
        ClientCredentialsResourceDetails authConfig = new ClientCredentialsResourceDetails()
        authConfig.accessTokenUri = configService.wesAuthBaseUrl
        authConfig.clientId = configService.wesAuthClientId
        authConfig.clientSecret = configService.wesAuthClientSecret
        return authConfig
    }
}
