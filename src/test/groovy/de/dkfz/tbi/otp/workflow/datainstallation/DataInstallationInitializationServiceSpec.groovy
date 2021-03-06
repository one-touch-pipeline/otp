/*
 * Copyright 2011-2020 The OTP authors
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
import org.grails.web.json.JSONObject
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.*

class DataInstallationInitializationServiceSpec extends Specification
        implements ServiceUnitTest<DataInstallationInitializationService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
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
                name: DataInstallationInitializationService.WORKFLOW,
        ])

        Collection<SeqTrack> seqTracks = (1..3).collect {
            createSeqTrackWithTwoDataFile()
        }

        FastqImportInstance instance = createFastqImportInstance([
                dataFiles: seqTracks*.dataFiles.flatten(),
        ])
        DataInstallationInitializationService service = Spy(DataInstallationInitializationService) {
            getConfigFragments(_, _) >> []
        }

        service.lsdfFilesService = Mock(LsdfFilesService) {
            3 * getFileViewByPidPath(_) >> TestCase.uniqueNonExistentPath
        }
        service.workflowRunService = new WorkflowRunService()
        service.workflowRunService.configFragmentService = Mock(ConfigFragmentService) {
            mergeSortedFragments(_) >> new JSONObject()
        }
        service.workflowArtefactService = new WorkflowArtefactService()

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
