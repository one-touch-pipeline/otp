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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class CellRangerCleanUpJobSpec extends Specification implements DataTest, CellRangerFactory, WorkflowSystemDomainFactory {

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowStep,
                SingleCellBamFile,
                CellRangerMergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FastqImportInstance,
        ]
    }

    CellRangerCleanUpJob job
    CellRangerWorkFileService cellRangerWorkFileService
    CellRangerLinkFileService cellRangerLinkFileService

    WorkflowStep workflowStep
    MergingWorkPackage workPackage
    SingleCellBamFile currentBamFile

    void setup() {
        job = new CellRangerCleanUpJob()
        cellRangerWorkFileService = Mock(CellRangerWorkFileService)
        cellRangerLinkFileService = Mock(CellRangerLinkFileService)
        job.cellRangerWorkFileService = cellRangerWorkFileService
        job.cellRangerLinkFileService = cellRangerLinkFileService

        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([
                workflow: findOrCreateWorkflow(CellRangerWorkflow.WORKFLOW)
        ])])
        workPackage = createMergingWorkPackage()
        currentBamFile = createBamFile([workPackage: workPackage])
    }

    void "test getAdditionalPathsToDelete should clean output directory except result directory"() {
        given:
        createBamFile([workPackage: workPackage])
        createBamFile([workPackage: workPackage])

        Path outputDirectory = tempDir.resolve("1234567")
        Path resultDirectory = outputDirectory.resolve("outs")
        Path tempFile1 = outputDirectory.resolve("temp1.log")
        Path tempFile2 = outputDirectory.resolve("temp2.log")
        Path oldPath1 = outputDirectory.resolve("old1")
        Path oldPath2 = outputDirectory.resolve("old2")

        // Create directory structure
        Files.createDirectories(outputDirectory)
        Files.createDirectories(resultDirectory)
        Files.createFile(tempFile1)
        Files.createFile(tempFile2)
        Files.createFile(oldPath1)
        Files.createFile(oldPath2)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> resultDirectory

        and:
        pathsToDelete.contains(tempFile1)
        pathsToDelete.contains(tempFile2)
        !pathsToDelete.contains(resultDirectory) // Should be preserved
        pathsToDelete.contains(oldPath1)
        pathsToDelete.contains(oldPath2)
    }

    void "test getAdditionalPathsToDelete should handle empty old bam files list"() {
        given:
        Path outputDirectory = tempDir.resolve("1234567")
        Path resultDirectory = outputDirectory.resolve("outs")
        Path tempFile1 = outputDirectory.resolve("temp1.log")

        // Create directory structure
        Files.createDirectories(outputDirectory)
        Files.createDirectories(resultDirectory)
        Files.createFile(tempFile1)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> resultDirectory

        and:
        pathsToDelete.size() == 1
        pathsToDelete.contains(tempFile1)
        !pathsToDelete.contains(resultDirectory)
    }

    void "test getAdditionalPathsToDelete should preserve result directory"() {
        given:
        Path outputDirectory = tempDir.resolve("output")
        Path resultDirectory = outputDirectory.resolve("results")

        // Create directory structure with only result directory
        Files.createDirectories(outputDirectory)
        Files.createDirectories(resultDirectory)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }
        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> resultDirectory

        and:
        pathsToDelete.empty // Only result directory exists and it should be preserved
    }

    void "test getWorkFoldersToClear should return work folders for all bam files not in the work package"() {
        given: "old bam files in the other merging work packages"
        SingleCellBamFile oldBamFile1 = createBamFile([workPackage: workPackage])
        SingleCellBamFile oldBamFile2 = createBamFile([workPackage: workPackage])

        WorkFolder workFolder1 = createWorkFolder()
        WorkFolder workFolder2 = createWorkFolder()

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<WorkFolder> workFolders = job.getWorkFoldersToClear(workflowStep)

        then:
        1 * cellRangerWorkFileService.getWorkFolder(oldBamFile1) >> workFolder1
        1 * cellRangerWorkFileService.getWorkFolder(oldBamFile2) >> workFolder2

        and:
        workFolders.size() == 2
        workFolders.contains(workFolder1)
        workFolders.contains(workFolder2)
    }

    void "test getWorkFoldersToClear should handle empty list of old bam files"() {
        given:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<WorkFolder> workFolders = job.getWorkFoldersToClear(workflowStep)

        then:
        workFolders.empty
    }

    void "test getWorkFoldersToClear should filter out null work folders"() {
        given: "old bam files in the other merging work packages"
        SingleCellBamFile oldBamFile1 = createBamFile([workPackage: workPackage])
        SingleCellBamFile oldBamFile2 = createBamFile([workPackage: workPackage])
        SingleCellBamFile oldBamFile3 = createBamFile([workPackage: workPackage])

        WorkFolder workFolder1 = createWorkFolder()
        WorkFolder workFolder3 = createWorkFolder()

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<WorkFolder> workFolders = job.getWorkFoldersToClear(workflowStep)

        then:
        1 * cellRangerWorkFileService.getWorkFolder(oldBamFile1) >> workFolder1
        1 * cellRangerWorkFileService.getWorkFolder(oldBamFile2) >> null // Returns null
        1 * cellRangerWorkFileService.getWorkFolder(oldBamFile3) >> workFolder3

        and:
        workFolders.size() == 2
        workFolders.contains(workFolder1)
        workFolders.contains(workFolder3)
        !workFolders.contains(null)
    }

    void "test getAdditionalPathsToDelete should handle null result directory gracefully"() {
        given:
        Path outputDirectory = tempDir.resolve("output")
        Path tempFile1 = outputDirectory.resolve("temp1.log")

        // Create directory structure
        Files.createDirectories(outputDirectory)
        Files.createFile(tempFile1)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> null

        and:
        pathsToDelete.size() == 1
        pathsToDelete.contains(tempFile1)
    }

    void "test getAdditionalPathsToDelete should handle non-existent result directory gracefully"() {
        given:
        Path outputDirectory = tempDir.resolve("output")
        Path nonExistentResultDirectory = outputDirectory.resolve("non-existent-results")
        Path tempFile1 = outputDirectory.resolve("temp1.log")

        // Create directory structure (but not result directory)
        Files.createDirectories(outputDirectory)
        Files.createFile(tempFile1)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> nonExistentResultDirectory

        and:
        pathsToDelete.size() == 1
        pathsToDelete.contains(tempFile1)
        !pathsToDelete.contains(nonExistentResultDirectory)
    }

    void "test getAdditionalPathsToDelete should include paths from getDirectoryPath calls"() {
        given:
        SingleCellBamFile oldBamFile1 = createBamFile([workPackage: workPackage])
        SingleCellBamFile oldBamFile2 = createBamFile([workPackage: workPackage])

        Path outputDirectory = tempDir.resolve("output")
        Path resultDirectory = outputDirectory.resolve("results")
        Path oldPath1 = tempDir.resolve("old1")
        Path oldPath2 = tempDir.resolve("old2")

        // Create directory structure
        Files.createDirectories(outputDirectory)
        Files.createDirectories(resultDirectory)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> resultDirectory
        1 * cellRangerLinkFileService.getDirectoryPath(oldBamFile1) >> oldPath1
        1 * cellRangerLinkFileService.getDirectoryPath(oldBamFile2) >> oldPath2

        and:
        pathsToDelete.contains(oldPath1)
        pathsToDelete.contains(oldPath2)
        !pathsToDelete.contains(resultDirectory)
    }

    void "test getAdditionalPathsToDelete should filter out null paths from getDirectoryPath"() {
        given:
        SingleCellBamFile oldBamFile1 = createBamFile([workPackage: workPackage])
        SingleCellBamFile oldBamFile2 = createBamFile([workPackage: workPackage])

        Path outputDirectory = tempDir.resolve("output")
        Path resultDirectory = outputDirectory.resolve("results")
        Path oldPath1 = tempDir.resolve("old1")

        // Create directory structure
        Files.createDirectories(outputDirectory)
        Files.createDirectories(resultDirectory)

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        List<Path> pathsToDelete = job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> resultDirectory
        1 * cellRangerLinkFileService.getDirectoryPath(oldBamFile1) >> oldPath1
        1 * cellRangerLinkFileService.getDirectoryPath(oldBamFile2) >> null // Returns null

        and:
        pathsToDelete.contains(oldPath1)
        !pathsToDelete.contains(null)
        !pathsToDelete.contains(resultDirectory)
    }

    void "test getAdditionalPathsToDelete should handle exception during file listing gracefully"() {
        given:
        Path outputDirectory = tempDir.resolve("output")
        Path resultDirectory = outputDirectory.resolve("results")

        // Create directory structure
        Files.createDirectories(outputDirectory)
        Files.createDirectories(resultDirectory)

        // Make the output directory unreadable to trigger exception
        outputDirectory.toFile().readable = false

        and:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        job.getAdditionalPathsToDelete(workflowStep)

        then:
        1 * cellRangerLinkFileService.getOutputDirectory(currentBamFile) >> outputDirectory
        1 * cellRangerLinkFileService.getResultDirectory(currentBamFile) >> resultDirectory

        and:
        thrown(Exception) // Should propagate the exception

        cleanup:
        outputDirectory.toFile().readable = true // Restore permissions
    }

    void "test getBamFile delegation to concrete artefact service"() {
        given:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, CellRangerWorkflow.OUTPUT_BAM) >> currentBamFile
            0 * _
        }

        when:
        SingleCellBamFile result = job.getBamFile(workflowStep)

        then:
        result == currentBamFile
    }
}
