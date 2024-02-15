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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.*

class DataInstallationInitializationServiceSpec extends Specification
        implements ServiceUnitTest<DataInstallationInitializationService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                SeqTrack,
                WorkflowArtefact,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "createWorkflowRuns, when called, then return a WorkflowRun for each seqTrack of the datafiles of the instance"() {
        given:
        Workflow workflow = createWorkflow([
                name: DataInstallationWorkflow.WORKFLOW,
        ])

        Collection<SeqTrack> seqTracks = (1..3).collect {
            createSeqTrackWithTwoFastqFile()
        }

        FastqImportInstance instance = createFastqImportInstance([
                sequenceFiles: seqTracks*.sequenceFiles.flatten(),
        ])
        DataInstallationInitializationService service = new DataInstallationInitializationService()

        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            3 * getDirectoryPath(_) >> TestCase.uniqueNonExistentPath.toPath()
        }
        service.workflowRunService = new WorkflowRunService()
        service.workflowRunService.configFragmentService = Mock(ConfigFragmentService) {
            mergeSortedFragments(_) >> "{}"
        }
        service.workflowArtefactService = new WorkflowArtefactService()
        service.workflowService = Mock(WorkflowService) {
            1 * getExactlyOneWorkflow(DataInstallationWorkflow.WORKFLOW) >> workflow
        }

        when:
        List<WorkflowRun> runs = service.createWorkflowRuns(instance)

        then:
        runs
        runs.size() == 3
        runs.each {
            assert it.inputArtefacts.isEmpty()
            assert it.workflow == workflow
            assert it.outputArtefacts.size() == 1
        }
        TestCase.assertContainSame(runs, seqTracks*.workflowArtefact*.producedBy)
    }
}
