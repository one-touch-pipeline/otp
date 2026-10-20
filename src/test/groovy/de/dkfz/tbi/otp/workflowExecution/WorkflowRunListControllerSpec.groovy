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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.HttpMethod
import spock.lang.Specification
import spock.lang.Unroll

import static de.dkfz.tbi.otp.workflowExecution.BulkKillResult.FailureReason.*

class WorkflowRunListControllerSpec extends Specification implements ControllerUnitTest<WorkflowRunListController> {

    void setup() {
        controller.workflowService = Mock(WorkflowService)
        request.method = HttpMethod.POST.name()
    }

    @Unroll
    void "killRuns rejects the complete selection before cancelling anything when the IDs are #runs"() {
        given:
        params.run = runs

        when:
        controller.killRuns()

        then:
        0 * controller.workflowService._
        response.redirectedUrl == '/workflowRunList/index'
        flash.message.message == 'workflowRun.list.killRuns.invalid'

        where:
        runs << [[], ['12', 'bad', '13'], ['12', '0'], ['12', '-5']]
    }

    void "killRuns passes an ID that cannot be represented as a long to the service as Long.MAX_VALUE"() {
        given: 'Grails data binding caps an overflowing Long at Long.MAX_VALUE'
        params.run = ['9223372036854775808']

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([Long.MAX_VALUE] as Set) >> new BulkKillResult(failures: [(Long.MAX_VALUE): NOT_FOUND])
        flash.message.message == 'workflowRun.list.killRuns.failed'
        flash.message.errorList == ["${Long.MAX_VALUE}: workflowRun.list.killRuns.reason.notFound".toString()]
    }

    @Unroll
    void "killRuns redirects to the workflow run list target '#redirect'"() {
        given:
        params.run = ['24']
        params.redirect = redirect

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([24L] as Set) >> new BulkKillResult(successCount: 1)
        response.redirectedUrl == redirect

        where:
        redirect << ['/workflowRunList', '/workflowRunList?state=RUNNING',
                     '/workflowRunList/index?state=RUNNING', '/workflowRunList/index#bulk']
    }

    @Unroll
    void "killRuns falls back to the run list when the redirect is '#redirect'"() {
        given:
        params.run = ['24']
        params.redirect = redirect

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([24L] as Set) >> new BulkKillResult(successCount: 1)
        response.redirectedUrl == '/workflowRunList/index'

        where:
        redirect << ['//evil.example', '/\\evil.example', 'https://evil.example', '/project/index',
                     '/workflowRunDetails/index/24', '/workflowRunListExtra/index',
                     '/workflowRunList/../project/index', '/workflowRunList/%2e%2e/project/index']
    }

    void "killRuns reports an all-success result"() {
        given:
        params.run = ['24', '25']

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([24L, 25L] as Set) >> new BulkKillResult(successCount: 2)
        flash.message.message == 'workflowRun.list.killRuns.allSuccess'
        !flash.message.errorList
    }

    void "killRuns passes each selected ID once to the service, in selection order"() {
        given:
        params.run = ['25', '24', '25']

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns { it as List == [25L, 24L] } >> new BulkKillResult(successCount: 2)
    }

    void "killRuns reports a partial result with the failed runs in the order returned by the service"() {
        given:
        params.run = ['21', '22', '23']

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([21L, 22L, 23L] as Set) >>
                new BulkKillResult(successCount: 1, failures: [(23L): NOT_KILLABLE, (21L): UNEXPECTED])
        flash.message.message == 'workflowRun.list.killRuns.partial'
        flash.message.errorList == [
                '23: workflowRun.list.killRuns.reason.notKillable',
                '21: workflowRun.list.killRuns.reason.unexpected',
        ]
    }

    void "killRuns reports an all-failed result"() {
        given:
        params.run = ['23']

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([23L] as Set) >> new BulkKillResult(failures: [(23L): UNEXPECTED])
        flash.message.message == 'workflowRun.list.killRuns.failed'
        flash.message.errorList == ['23: workflowRun.list.killRuns.reason.unexpected']
    }

    @Unroll
    void "killRuns shows the message for failure reason #reason"() {
        given:
        params.run = ['41']

        when:
        controller.killRuns()

        then:
        1 * controller.workflowService.killWorkflowRuns([41L] as Set) >> new BulkKillResult(failures: [(41L): reason])
        flash.message.errorList == ["41: ${code}".toString()]

        where:
        reason                 || code
        NOT_FOUND              || 'workflowRun.list.killRuns.reason.notFound'
        PROJECT_NOT_ACCESSIBLE || 'workflowRun.list.killRuns.reason.projectNotAccessible'
        NOT_KILLABLE           || 'workflowRun.list.killRuns.reason.notKillable'
        UNEXPECTED             || 'workflowRun.list.killRuns.reason.unexpected'
    }
}
