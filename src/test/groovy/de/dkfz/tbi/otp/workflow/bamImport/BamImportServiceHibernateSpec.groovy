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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.workflow.WorkflowCreateState

class BamImportServiceHibernateSpec extends HibernateSpec implements ServiceUnitTest<BamImportService>, ExternalBamFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                ImportProcess,
                ExternallyProcessedBamFile,
                ExternalMergingWorkPackage,
        ]
    }

    @Unroll
    void "waiting, should return a valid ImportProcess or null object depending on the available ImportProcesses"() {
        given:
        states.each {
            createImportProcessHelper(it)
        }

        when:
        ImportProcess importProcess = service.waiting()

        then:
        (importProcess != null) == returnOne

        where:
        states                                                        || returnOne
        [WorkflowCreateState.WAITING]                                 || true
        [WorkflowCreateState.PROCESSING]                              || false
        [WorkflowCreateState.SUCCESS]                                 || false
        [WorkflowCreateState.FAILED]                                  || false
        [WorkflowCreateState.WAITING, WorkflowCreateState.WAITING]    || true
        [WorkflowCreateState.WAITING, WorkflowCreateState.PROCESSING] || true
        [WorkflowCreateState.WAITING, WorkflowCreateState.SUCCESS]    || true
        [WorkflowCreateState.WAITING, WorkflowCreateState.FAILED]     || true
        [WorkflowCreateState.PROCESSING, WorkflowCreateState.SUCCESS] || false
        [WorkflowCreateState.PROCESSING, WorkflowCreateState.FAILED]  || false

        name = "instances with states ${states.join(' and ')}"
        result = returnOne ? "return a waiting" : "do not return any"
    }

    void "waiting, when multiple waiting and no one is in process, then return the oldest"() {
        given:
        ImportProcess importProcessExpected = (1..3).collect {
            createImportProcessHelper(WorkflowCreateState.WAITING)
        }.first()

        when:
        ImportProcess importProcess = service.waiting()

        then:
        importProcess == importProcessExpected
    }

    private ImportProcess createImportProcessHelper(WorkflowCreateState state) {
        return createImportProcess([
                externallyProcessedBamFiles: [
                        DomainFactory.createExternallyProcessedBamFile(),
                        DomainFactory.createExternallyProcessedBamFile(),
                ],
                workflowCreateState: state,
        ])
    }
}
