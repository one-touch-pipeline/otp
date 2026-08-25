/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.testing

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import static javax.servlet.http.HttpServletResponse.SC_OK

class TestingControllerSpec extends Specification implements ControllerUnitTest<TestingController> {

    void "beginPage, when the feature flag is disabled, returns 403 and does not touch the transaction"() {
        given:
        controller.configService = Mock(ConfigService) {
            1 * isTestingEndpointsEnabled() >> false
        }
        controller.testTransactionService = Mock(TestTransactionService)
        controller.request.method = 'POST'

        when:
        controller.beginPage()

        then:
        controller.response.status == SC_FORBIDDEN
        0 * controller.testTransactionService.beginPage()
    }

    void "beginTest, when the feature flag is disabled, returns 403 and does not touch the transaction"() {
        given:
        controller.configService = Mock(ConfigService) {
            1 * isTestingEndpointsEnabled() >> false
        }
        controller.testTransactionService = Mock(TestTransactionService)
        controller.request.method = 'POST'

        when:
        controller.beginTest()

        then:
        controller.response.status == SC_FORBIDDEN
        0 * controller.testTransactionService.beginTest()
    }

    void "beginPage, when the feature flag is enabled, opens the page transaction and returns ok"() {
        given:
        controller.configService = Mock(ConfigService) {
            1 * isTestingEndpointsEnabled() >> true
        }
        controller.testTransactionService = Mock(TestTransactionService)
        controller.request.method = 'POST'

        when:
        controller.beginPage()

        then:
        1 * controller.testTransactionService.beginPage()
        controller.response.status == SC_OK
        controller.response.json.status == "ok"
    }

    void "beginTest, when the feature flag is enabled, resets the test savepoint and returns ok"() {
        given:
        controller.configService = Mock(ConfigService) {
            1 * isTestingEndpointsEnabled() >> true
        }
        controller.testTransactionService = Mock(TestTransactionService)
        controller.request.method = 'POST'

        when:
        controller.beginTest()

        then:
        1 * controller.testTransactionService.beginTest()
        controller.response.status == SC_OK
        controller.response.json.status == "ok"
    }
}
