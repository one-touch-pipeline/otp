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
package de.dkfz.tbi.otp.errors

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.utils.ExceptionUtils
import de.dkfz.tbi.otp.utils.RequestUtilService

import static javax.servlet.http.HttpServletResponse.*

class ErrorControllerSpec extends Specification implements ControllerUnitTest<ErrorController> {

    void setup() {
        controller.requestUtilService = Mock(RequestUtilService)
        controller.processingOptionService = Mock(ProcessingOptionService)
    }

    void "index, forwards to error500"() {
        given:
        controller.request.method = method

        when:
        controller.index()

        then:
        controller.response.forwardedUrl == '/error/error500'

        where:
        method << ErrorController.allHttpMethods
    }

    void "error403, when called with #method and not ajax, returns 403 with empty model"() {
        given:
        controller.request.method = method

        when:
        Map model = controller.error403()

        then:
        1 * controller.requestUtilService.isAjax(_) >> false
        controller.response.status == SC_FORBIDDEN
        model == [:]

        where:
        method << ErrorController.allHttpMethods
    }

    void "error403, when called with #method and ajax, returns 403 and renders an empty map"() {
        given:
        controller.request.method = method

        when:
        controller.error403()

        then:
        1 * controller.requestUtilService.isAjax(_) >> true
        controller.response.status == SC_FORBIDDEN
        controller.response.text == '[:]'

        where:
        method << ErrorController.allHttpMethods
    }

    void "error404, when called with #method and not ajax, returns 404 with the forward URI in the model"() {
        given:
        controller.request.method = method
        controller.request.forwardURI = '/some/missing/page'

        when:
        Map model = controller.error404()

        then:
        1 * controller.requestUtilService.isAjax(_) >> false
        controller.response.status == SC_NOT_FOUND
        model == [notFound: '/some/missing/page']

        where:
        method << ErrorController.allHttpMethods
    }

    void "error404, when called with #method and ajax, returns 404 and renders the forward URI"() {
        given:
        controller.request.method = method
        controller.request.forwardURI = '/some/missing/page'

        when:
        controller.error404()

        then:
        1 * controller.requestUtilService.isAjax(_) >> true
        controller.response.status == SC_NOT_FOUND
        controller.response.text == '/some/missing/page'

        where:
        method << ErrorController.allHttpMethods
    }

    void "error405, when called with #method, returns 405 with the request method in the model"() {
        given:
        controller.request.method = method

        when:
        Map model = controller.error405()

        then:
        controller.response.status == SC_METHOD_NOT_ALLOWED
        model == [method: method]

        where:
        method << ErrorController.allHttpMethods
    }

    void "error500, when called with #method and not ajax, returns 500 with digest, message, and stack trace in the model"() {
        given:
        controller.request.method = method
        controller.request.exception = new IllegalStateException('something broke')

        when:
        Map model = controller.error500()

        then:
        1 * controller.requestUtilService.isAjax(_) >> false
        controller.response.status == SC_INTERNAL_SERVER_ERROR
        model.code == model.stackTrace.encodeAsMD5()
        model.message == 'something broke'
        model.stackTrace.contains('IllegalStateException')

        where:
        method << ErrorController.allHttpMethods
    }

    void "error500, when called with ajax, returns 500 and renders only the digest"() {
        given:
        IllegalStateException exception = new IllegalStateException('something broke')
        controller.request.exception = exception

        when:
        controller.error500()

        then:
        1 * controller.requestUtilService.isAjax(_) >> true
        controller.response.status == SC_INTERNAL_SERVER_ERROR
        controller.response.text == ExceptionUtils.getStackTrace(exception).encodeAsMD5()
    }

    void "error500, when no exception is present, returns 500 with digest of an empty stack trace"() {
        when:
        Map model = controller.error500()

        then:
        1 * controller.requestUtilService.isAjax(_) >> false
        controller.response.status == SC_INTERNAL_SERVER_ERROR
        model == [code: ''.encodeAsMD5(), message: null, stackTrace: '']
    }

    void "noProject, returns 404 with the gain access option in the model"() {
        given:
        controller.request.method = 'GET'

        when:
        Map model = controller.noProject()

        then:
        1 * controller.processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_GAIN_PROJECT_ACCESS) >> 'ask your PI'
        controller.response.status == SC_NOT_FOUND
        model == [gainAccess: 'ask your PI']
    }

    void "noProject, when called with POST, is rejected with 405"() {
        given:
        controller.request.method = 'POST'

        when:
        controller.noProject()

        then:
        0 * controller.processingOptionService._
        controller.response.status == SC_METHOD_NOT_ALLOWED
    }

    void "switchedUserDeniedException, returns 401 with the exception in the model"() {
        given:
        controller.request.method = 'GET'
        Exception exception = new IllegalStateException('denied')
        controller.request.setAttribute('exception', exception)

        when:
        Map model = controller.switchedUserDeniedException()

        then:
        controller.response.status == SC_UNAUTHORIZED
        model == [exception: exception]
    }

    void "switchedUserDeniedException, when called with POST, is rejected with 405"() {
        given:
        controller.request.method = 'POST'

        when:
        controller.switchedUserDeniedException()

        then:
        controller.response.status == SC_METHOD_NOT_ALLOWED
    }
}
