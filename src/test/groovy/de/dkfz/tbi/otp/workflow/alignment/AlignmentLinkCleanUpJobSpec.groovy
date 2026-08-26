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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentLinkFileServiceFactoryService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerLinkFileService
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeIndex
import de.dkfz.tbi.otp.ngsdata.ToolName
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService

import java.nio.file.Files
import java.nio.file.Path

class AlignmentLinkCleanUpJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @TempDir
    Path tempDir

    AlignmentLinkCleanUpJob job

    ConcreteArtefactService concreteArtefactService
    AlignmentLinkFileServiceFactoryService alignmentLinkFileServiceFactoryService
    FileService fileService
    LogService logService
    WorkflowStateChangeService workflowStateChangeService

    RoddyPanCancerFactory roddyPanCancerFactory
    CellRangerFactory cellRangerFactory

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                LibraryPreparationKit,
                MergingWorkPackage,
                CellRangerMergingWorkPackage,
                Pipeline,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RnaRoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                SingleCellBamFile,
                WorkflowStep,
                WorkflowRun,
                Workflow,
                WorkflowVersion,
                Individual,
                Project,
                ReferenceGenome,
                ReferenceGenomeIndex,
                ToolName,
                SeqType,
                SeqTrack,
        ]
    }

    void setup() {
        concreteArtefactService = Mock(ConcreteArtefactService)
        alignmentLinkFileServiceFactoryService = Mock(AlignmentLinkFileServiceFactoryService)
        fileService = Mock(FileService)
        logService = Mock(LogService)
        workflowStateChangeService = Mock(WorkflowStateChangeService)

        roddyPanCancerFactory = new RoddyPanCancerFactory() {}
        cellRangerFactory = new CellRangerFactory() {}

        job = new AlignmentLinkCleanUpJob()
        job.concreteArtefactService = concreteArtefactService
        job.alignmentLinkFileServiceFactoryService = alignmentLinkFileServiceFactoryService
        job.fileService = fileService
        job.logService = logService
        job.workflowStateChangeService = workflowStateChangeService
    }

    RoddyBamFile createRoddyBamFileHelper(Map properties = [:]) {
        return roddyPanCancerFactory.createBamFile(properties)
    }

    SingleCellBamFile createSingleCellBamFileHelper(Map properties = [:]) {
        return cellRangerFactory.createBamFile(properties)
    }

    private WorkflowStep createWorkflowStepHelper(String workflowName) {
        return createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflow: findOrCreateWorkflow(workflowName)
                ])
        ])
    }

    /**
     * Creates a real view-by-pid base directory containing a symbolic link, a subdirectory and the nonOTP marker,
     * to verify that every entry except the nonOTP marker is passed to the recursive deletion.
     */
    private Map createBaseDirectoryWithContent() {
        Path baseDir = Files.createDirectories(tempDir.resolve("merged-alignment"))
        Path target = Files.createFile(tempDir.resolve("target"))
        Path bamLink = Files.createSymbolicLink(baseDir.resolve("blood_pid_merged.mdup.bam"), target)
        Path subDir = Files.createDirectories(baseDir.resolve("qualitycontrol"))
        Path nonOtp = Files.createFile(baseDir.resolve(ExternallyProcessedBamFile.NON_OTP))
        return [baseDir: baseDir, bamLink: bamLink, subDir: subDir, nonOtp: nonOtp]
    }

    void "execute, should recursively delete every entry of the shared base directory except the nonOTP marker"() {
        given:
        RoddyBamFile bamFile = createRoddyBamFileHelper()
        WorkflowStep workflowStep = createWorkflowStepHelper(PanCancerWorkflow.WORKFLOW)

        PanCancerLinkFileService linkFileService = Mock(PanCancerLinkFileService)
        Map content = createBaseDirectoryWithContent()

        when:
        job.execute(workflowStep)

        then:
        1 * concreteArtefactService.getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        1 * alignmentLinkFileServiceFactoryService.getService(bamFile) >> linkFileService
        1 * linkFileService.getDirectoryPath(bamFile) >> content.baseDir
        1 * fileService.deleteDirectoryRecursively(content.bamLink)
        1 * fileService.deleteDirectoryRecursively(content.subDir)
        0 * fileService.deleteDirectoryRecursively(content.nonOtp)
        1 * workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "execute, for the CellRanger structure, should recursively delete every entry of the link directory except the nonOTP marker"() {
        given:
        SingleCellBamFile bamFile = createSingleCellBamFileHelper()
        WorkflowStep workflowStep = createWorkflowStepHelper(CellRangerWorkflow.WORKFLOW)

        CellRangerLinkFileService linkFileService = Mock(CellRangerLinkFileService)
        Map content = createBaseDirectoryWithContent()

        when:
        job.execute(workflowStep)

        then:
        1 * concreteArtefactService.getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        1 * alignmentLinkFileServiceFactoryService.getService(bamFile) >> linkFileService
        1 * linkFileService.getDirectoryPath(bamFile) >> content.baseDir
        1 * fileService.deleteDirectoryRecursively(content.bamLink)
        1 * fileService.deleteDirectoryRecursively(content.subDir)
        0 * fileService.deleteDirectoryRecursively(content.nonOtp)
        1 * workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "execute, when the base directory does not exist, should not delete anything and just succeed"() {
        given:
        RoddyBamFile bamFile = createRoddyBamFileHelper()
        WorkflowStep workflowStep = createWorkflowStepHelper(PanCancerWorkflow.WORKFLOW)

        PanCancerLinkFileService linkFileService = Mock(PanCancerLinkFileService)
        Path missingDir = tempDir.resolve("does-not-exist")

        when:
        job.execute(workflowStep)

        then:
        1 * concreteArtefactService.getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        1 * alignmentLinkFileServiceFactoryService.getService(bamFile) >> linkFileService
        1 * linkFileService.getDirectoryPath(bamFile) >> missingDir
        0 * fileService.deleteDirectoryRecursively(_)
        1 * workflowStateChangeService.changeStateToSuccess(workflowStep)
    }
}
