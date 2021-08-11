/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class PanCancerLinkJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqImportInstance,
                FileType,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    PanCancerLinkJob job
    RoddyBamFile roddyBamFile
    TestConfigService configService
    WorkflowStep workflowStep

    @Rule
    TemporaryFolder temporaryFolder

    void setupData() {
        roddyBamFile = createBamFile(roddyExecutionDirectoryNames: ["exec_123456_123456789_test_test"])
        workflowStep = createWorkflowStep()

        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        job = Spy(PanCancerLinkJob) {
            getRoddyBamFile(workflowStep) >> roddyBamFile
        }
        job.fileSystemService = new TestFileSystemService()
        job.fileService = new FileService()
        job.logService = Mock(LogService)
    }

    void cleanup() {
        configService.clean()
    }

    void "test getLinkMap"() {
        given:
        setupData()
        List<Path> linkedFiles = createLinkedFilesList(roddyBamFile)

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        linkedFiles.every {
            it in result*.link
        }
    }

    private List<Path> createLinkedFilesList(RoddyBamFile roddyBamFile) {
        return [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalMergedQADirectory,
                roddyBamFile.finalExecutionDirectories,
                roddyBamFile.finalSingleLaneQADirectories.values(),
        ].flatten()*.toPath()
    }
}
