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

import grails.testing.services.ServiceUnitTest
import io.swagger.client.wes.api.WorkflowExecutionServiceApi
import io.swagger.client.wes.auth.Authentication
import io.swagger.client.wes.auth.OAuth
import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService

class WeskitApiServiceSpec extends Specification implements ServiceUnitTest<WeskitApiService> {

    void "createApiInstance, when called, then return WorkflowExecutionServiceApi object"() {
        given:
        String url = 'url'
        String token = 'token'

        service.configService = Mock(ConfigService) {
            1 * wesUrl >> url
            0 * _
        }
        service.weskitAuthService = Mock(WeskitAuthService) {
            1 * requestWeskitAccessToken() >> token
            0 * _
        }

        when:
        WorkflowExecutionServiceApi apiInstance = service.createApiInstance()

        then:
        apiInstance.apiClient.basePath == url
        Collection<Authentication> authentications = apiInstance.apiClient.authentications.values()
        authentications.size() == 1
        ((OAuth) authentications.first()).accessToken == token
    }
}
