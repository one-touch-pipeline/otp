/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.converters.JSON
import org.grails.web.json.JSONElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

import de.dkfz.tbi.otp.config.ConfigService

/**
 * Helper service for {@link WeskitAccessService} and {@link WeskitApiService} to get the access token from keycloak.
 */
class WeskitAuthService {

    static final String CLIENT_REGISTRATION_ID = "wes"

    ConfigService configService

    @Autowired
    WebClient webClient

    /**
     * returns the token to access WESKit.
     */
    //library use directly RuntimeException
    @SuppressWarnings('CatchRuntimeException')
    String requestWeskitAccessToken() {
        try {
            WebClient.RequestBodyUriSpec requestBuilder = webClient.post()
            requestBuilder.uri(configService.wesAuthTokenUri)
            requestBuilder.attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(CLIENT_REGISTRATION_ID))
            requestBuilder.accept(MediaType.APPLICATION_JSON)
            requestBuilder.body(BodyInserters.fromFormData(createParameters()))

            String accessTokenJsonString = requestBuilder.retrieve()
                    .bodyToMono(String)
                    .block()
            JSONElement accessTokenJson = JSON.parse(accessTokenJsonString)
            assert accessTokenJson['access_token']
            return accessTokenJson['access_token']
        } catch (RuntimeException e) {
            throw new WeskitRequestAccessTokenFailedException("Failed to get weskit access token", e)
        }
    }

    /**
     * create the parameters for request
     */
    private MultiValueMap<String, String> createParameters() {
        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>()
        bodyValues.add('grant_type', 'password')
        bodyValues.add('client_id', configService.wesAuthClientId)
        bodyValues.add('client_secret', configService.wesAuthClientSecret)
        bodyValues.add('username', configService.wesAuthClientUser)
        bodyValues.add('password', configService.wesAuthClientPassword)
        return bodyValues
    }
}
