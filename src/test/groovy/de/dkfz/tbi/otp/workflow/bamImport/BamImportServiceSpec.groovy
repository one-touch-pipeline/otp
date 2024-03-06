/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.*

class BamImportServiceSpec extends Specification implements ServiceUnitTest<BamImportService>,
        DataTest, WorkflowSystemDomainFactory, ExternalBamFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BamImportInstance,
                ExternallyProcessedBamFile,
                ExternalMergingWorkPackage,
                WorkflowArtefact,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "findCountWithWaitingState, should return the correct count of FastqImportInstance in WAITING state"() {
        given:
        [
                WorkflowCreateState.PROCESSING,
                WorkflowCreateState.SUCCESS,
                WorkflowCreateState.FAILED,
                WorkflowCreateState.WAITING,
                WorkflowCreateState.WAITING,
        ].each {
            createBamImportInstance([
                    workflowCreateState: it
            ])
        }

        when:
        int count = service.countInstancesInWaitingState()

        then:
        count == 2
    }

    void "changeProcessToWait, should change the state from PROCESSING to WAITING"() {
        given:
        BamImportInstance bamImportInstance = createBamImportInstance([
                workflowCreateState     : WorkflowCreateState.PROCESSING,
        ])

        when:
        service.changeProcessToWait()

        then:
        bamImportInstance.workflowCreateState == WorkflowCreateState.WAITING
    }

    @Unroll
    void "changeProcessToWait, should not change state #state"() {
        given:
        BamImportInstance bamImportInstance = createBamImportInstance([
                workflowCreateState     : state,
        ])

        when:
        service.changeProcessToWait()

        then:
        bamImportInstance.workflowCreateState == state

        where:
        state << [
                WorkflowCreateState.FAILED,
                WorkflowCreateState.SUCCESS,
                WorkflowCreateState.WAITING,
        ]
    }
}
