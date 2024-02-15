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
package de.dkfz.tbi.otp.workflow.alignment.wgbs

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WgbsAlignmentWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.*
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class WgbsLinkJobSpec extends Specification implements DataTest, WgbsAlignmentWorkflowDomainFactory, RoddyPanCancerFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
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

    WgbsLinkJob job
    RoddyBamFile roddyBamFile
    WorkflowStep workflowStep
    PanCancerLinkFileService panCancerLinkFileService
    WgbsAlignmentLinkFileService wgbsAlignmentLinkFileService

    void setupData() {
        roddyBamFile = createBamFile(roddyExecutionDirectoryNames: ["exec_123456_123456789_test_test"])
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateWgbsAlignmenWorkflow(),
                ]),
        ])

        job = new WgbsLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> roddyBamFile
            0 * _
        }
        job.fileSystemService = new TestFileSystemService()
        job.fileService = new FileService()
        job.logService = Mock(LogService)

        AbstractBamFileService abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/")
        }

        panCancerLinkFileService = new PanCancerLinkFileService(abstractBamFileService : abstractBamFileService)
        job.panCancerLinkFileService = panCancerLinkFileService
        job.panCancerWorkFileService = new PanCancerWorkFileService(abstractBamFileService : abstractBamFileService)

        wgbsAlignmentLinkFileService = new WgbsAlignmentLinkFileService(abstractBamFileService : abstractBamFileService)
        job.wgbsAlignmentLinkFileService = wgbsAlignmentLinkFileService
        job.wgbsAlignmentWorkFileService = new WgbsAlignmentWorkFileService(abstractBamFileService : abstractBamFileService)
    }

    void "test getLinkMap"() {
        given:
        setupData()

        if (multipleLibraries) {
            roddyBamFile.seqTracks.first().libraryName = "2"
            roddyBamFile.seqTracks.first().normalizedLibraryName = "2"
            roddyBamFile.seqTracks.add(createSeqTrackWithTwoFastqFile(libraryName: "1"))
            roddyBamFile.numberOfMergedLanes = 2
            roddyBamFile.save(flush: true)
        }
        List<Path> linkedFiles = createLinkedFilesList(roddyBamFile, multipleLibraries)

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        linkedFiles.every {
            it in result*.link
        }

        where:
        multipleLibraries << [true, false]
    }

    private List<Path> createLinkedFilesList(RoddyBamFile roddyBamFile, boolean multipleLibraries) {
        List list = [
                panCancerLinkFileService.getBamFile(roddyBamFile),
                panCancerLinkFileService.getBaiFile(roddyBamFile),
                panCancerLinkFileService.getMd5sumFile(roddyBamFile),
                panCancerLinkFileService.getMergedQADirectory(roddyBamFile),
                panCancerLinkFileService.getExecutionDirectories(roddyBamFile),
                panCancerLinkFileService.getSingleLaneQADirectories(roddyBamFile).values(),
                wgbsAlignmentLinkFileService.getMergedMethylationDirectory(roddyBamFile),
                wgbsAlignmentLinkFileService.getMetadataTableFile(roddyBamFile),
        ]
        if (multipleLibraries) {
            list.addAll(wgbsAlignmentLinkFileService.getLibraryQADirectories(roddyBamFile).values())
            list.addAll(wgbsAlignmentLinkFileService.getLibraryMethylationDirectories(roddyBamFile).values())
        }
        return list.flatten()
    }
}
