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
package de.dkfz.tbi.otp

import grails.artefact.Artefact
import grails.converters.JSON
import grails.testing.web.controllers.ControllerUnitTest
import grails.validation.Validateable
import grails.validation.ValidationException
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException

import static javax.servlet.http.HttpServletResponse.*

class CheckAndCallSpec extends Specification implements ControllerUnitTest<CheckAndCallTestController> {

    private static final String MSG_CODE = 'checkAndCall.test'

    private static CheckAndCallTestCommand createCommand(String value) {
        CheckAndCallTestCommand cmd = new CheckAndCallTestCommand(value: value)
        cmd.validate()
        return cmd
    }

    void "checkErrorsAndCallMethod, when command is valid, then call method and keep its response"() {
        given:
        boolean called = false

        when:
        controller.checkErrorsAndCallMethod(createCommand('valid')) {
            called = true
            controller.render([key: 'value'] as JSON)
        }

        then:
        called
        response.status == SC_OK
        response.json == [key: 'value']
    }

    void "checkErrorsAndCallMethod, when command has errors, then respond with NOT_ACCEPTABLE and a JSON list of the errors"() {
        given:
        boolean called = false

        when:
        controller.checkErrorsAndCallMethod(createCommand(null)) {
            called = true
        }

        then:
        !called
        response.status == SC_NOT_ACCEPTABLE
        response.contentType.startsWith('application/json')
        response.json.status == SC_NOT_ACCEPTABLE
        response.json.error == 'Not Acceptable'
        (JSON.parse(response.json.message as String) as List).size() == 1
    }

    void "checkErrorsAndCallMethod, when command has multiple errors, then respond with all error messages"() {
        given:
        CheckAndCallTestCommand cmd = createCommand('valid')
        cmd.errors.rejectValue('value', 'invalid.value', 'Invalid value')
        cmd.errors.reject('invalid.combination', 'Invalid combination')
        boolean called = false

        when:
        controller.checkErrorsAndCallMethod(cmd) {
            called = true
        }

        then:
        !called
        response.status == SC_NOT_ACCEPTABLE
        response.contentType.startsWith('application/json')
        response.json.status == SC_NOT_ACCEPTABLE
        response.json.error == 'Not Acceptable'
        (JSON.parse(response.json.message as String) as List).sort() == ['Invalid combination', 'Invalid value']
    }

    void "checkErrorsAndCallMethod, when method throws ValidationException, then respond with BAD_REQUEST and a JSON list of the errors"() {
        given:
        CheckAndCallTestCommand invalidCmd = createCommand(null)

        when:
        controller.checkErrorsAndCallMethod(createCommand('valid')) {
            throw new ValidationException('validation failed', invalidCmd.errors)
        }

        then:
        response.status == SC_BAD_REQUEST
        response.json.status == SC_BAD_REQUEST
        (JSON.parse(response.json.message as String) as List).size() == 1
    }

    @Unroll
    void "checkErrorsAndCallMethod, when method throws #exception.class.simpleName, then respond with BAD_REQUEST and the exception message"() {
        when:
        controller.checkErrorsAndCallMethod(createCommand('valid')) {
            throw exception
        }

        then:
        response.status == SC_BAD_REQUEST
        response.json.status == SC_BAD_REQUEST
        response.json.error == 'Bad Request'
        response.json.message == 'failure'

        where:
        exception << [
                new AssertionError('failure'),
                new OtpRuntimeException('failure'),
                new UserException('failure'),
        ]
    }

    void "checkErrorsAndCallMethod, when method throws another exception, then do not handle it"() {
        when:
        controller.checkErrorsAndCallMethod(createCommand('valid')) {
            throw new IllegalStateException('failure')
        }

        then:
        thrown(IllegalStateException)
    }

    void "checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck, when command is valid, then return result and set success message"() {
        when:
        String result = controller.checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(createCommand('valid'), MSG_CODE) {
            return 'result'
        }

        then:
        result == 'result'
        flash.message.message == "${MSG_CODE}.success"
        !flash.cmd
    }

    void "checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck, when command has errors, then set failed message with errors"() {
        given:
        CheckAndCallTestCommand cmd = createCommand(null)
        boolean called = false

        when:
        controller.checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, MSG_CODE) {
            called = true
        }

        then:
        !called
        flash.message.message == "${MSG_CODE}.failed"
        flash.message.errorObject == cmd.errors
        flash.cmd == cmd
    }

    @Unroll
    void "checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck, when method throws #exception.class.simpleName, then set failed message with the exception message"() {
        given:
        CheckAndCallTestCommand cmd = createCommand('valid')

        when:
        controller.checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, MSG_CODE) {
            throw exception
        }

        then:
        flash.message.message == "${MSG_CODE}.failed"
        flash.message.errorList == ['failure']
        flash.cmd == cmd

        where:
        exception << [
                new AssertionError('failure'),
                new OtpRuntimeException('failure'),
        ]
    }

    void "checkErrorAndCallMethodWithFlashMessage, when form token is valid, then call method"() {
        given:
        SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_URI] = '/checkAndCallTest/index'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken('/checkAndCallTest/index')

        when:
        String result = controller.checkErrorAndCallMethodWithFlashMessage(createCommand('valid'), MSG_CODE) {
            return 'result'
        }

        then:
        result == 'result'
        flash.message.message == "${MSG_CODE}.success"
    }

    void "checkErrorAndCallMethodWithFlashMessage, when form token is missing, then do not call method and set expired session message"() {
        given:
        CheckAndCallTestCommand cmd = createCommand('valid')
        boolean called = false

        when:
        controller.checkErrorAndCallMethodWithFlashMessage(cmd, MSG_CODE) {
            called = true
        }

        then:
        !called
        flash.message.message == 'default.expired.session'
        flash.cmd == cmd
    }
}

@Artefact('Controller')
class CheckAndCallTestController implements CheckAndCall {
}

class CheckAndCallTestCommand implements Validateable {
    String value
}
