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

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService

class WeskitAuthServiceSpec extends Specification {

    void "requestWeskitAccessToken, when called, then return access token"() {
        given:
        WeskitAuthService service = new WeskitAuthService()
        String url = 'url'
        String token = 'token'
        String tokenJson = "{\"access_token\":\"${token}\",\"expires_in\":300,\"refresh_expires_in\":1800,\"refresh_token\":\"refreshToken\"," +
                "\"token_type\":\"Bearer\",\"not-before-policy\":0,\"session_state\":\"sessionState\",\"scope\":\"scope\"}"

        Mono<String> mono = Mock(Mono) {
            0 * _
        }

        WebClient.RequestBodyUriSpec requestBuilder = Mock(WebClient.RequestBodyUriSpec) {
            0 * _
        }

        WebClient.ResponseSpec responseSpec = Mock(WebClient.ResponseSpec) {
            0 * _
        }

        service.configService = Mock(ConfigService) {
            1 * wesAuthTokenUri >> url
            1 * wesAuthClientId >> 'wesAuthClientId'
            1 * wesAuthClientSecret >> 'wesAuthClientSecret'
            0 * _
        }
        service.webClient = Mock(WebClient) {
            1 * post() >> requestBuilder
            0 * _
        }

        when:
        String accessToken = service.requestWeskitAccessToken()

        then:
        1 * requestBuilder.uri(url) >> requestBuilder
        1 * requestBuilder.attributes(_) >> requestBuilder
        1 * requestBuilder.accept(MediaType.APPLICATION_JSON) >> requestBuilder
        1 * requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        1 * requestBuilder.body(_) >> requestBuilder
        1 * requestBuilder.retrieve() >> responseSpec
        1 * responseSpec.bodyToMono(String) >> mono
        1 * mono.block() >> tokenJson

        accessToken == token
    }
}
