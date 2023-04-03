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
import io.swagger.client.wes.model.*
import reactor.core.publisher.Mono
import spock.lang.Specification

class WeskitAccessServiceSpec extends Specification implements ServiceUnitTest<WeskitAccessService> {

    private WorkflowExecutionServiceApi api

    void setupData() {
        api = Mock(WorkflowExecutionServiceApi) {
            0 * _
        }
        service.weskitApiService = Mock(WeskitApiService) {
            1 * createApiInstance() >> api
            0 * _
        }
    }

    void "serviceInfo, when called, then return ServiceInfo object"() {
        given:
        setupData()
        ServiceInfo mockedServiceInfo = Mock(ServiceInfo)
        Mono<ServiceInfo> mockedMonoServiceInfo = Mock(Mono) {
            1 * block(_) >> mockedServiceInfo
        }

        when:
        ServiceInfo serviceInfo = service.serviceInfo

        then:
        1 * api.serviceInfo >> mockedMonoServiceInfo

        and:
        serviceInfo == mockedServiceInfo
    }

    void "getRunStatus, when called, then return RunStatus object"() {
        given:
        setupData()
        RunStatus mockedRunStatus = Mock(RunStatus)
        Mono<RunStatus> mockedMonoRunStatus = Mock(Mono) {
            1 * block(_) >> mockedRunStatus
        }

        when:
        RunStatus runStatus = service.getRunStatus("1")

        then:
        1 * api.getRunStatus("1") >> mockedMonoRunStatus

        and:
        runStatus == mockedRunStatus
    }

    void "getRunLog, when called, then return RunLog object"() {
        given:
        setupData()
        RunLog mockedRunLog = Mock(RunLog)
        Mono<RunLog> mockedMonoRunLog = Mock(Mono) {
            1 * block(_) >> mockedRunLog
        }

        when:
        RunLog runLog = service.getRunLog("1")

        then:
        1 * api.getRunLog("1") >> mockedMonoRunLog

        and:
        runLog == mockedRunLog
    }
}
