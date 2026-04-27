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
package de.dkfz.tbi.otp.workflowTest.bamImport

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.AbstractExternalBamFactory

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentWorkFileService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.bamImport.BamImportInitializationService
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowTest.AbstractWorkflowSpec

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

abstract class AbstractBamImportWorkflowSpec extends AbstractWorkflowSpec implements AbstractExternalBamFactory {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractBamImportWorkflowSpec)

    protected final static String FURTHER_FILE_NAME = "furtherFile.txt"

    protected final static String DIRECTORY1 = "directory1"
    protected final static String DIRECTORY2 = "directory2"
    protected final static String SUBDIRECTORY11 = "${DIRECTORY1}/subdirectory1"
    protected final static String SUBDIRECTORY21 = "${DIRECTORY2}/subdirectory1"
    protected final static String SUBDIRECTORY22 = "${DIRECTORY2}/subdirectory2"
    protected final static String SUBSUBDIRECTORY221 = "${SUBDIRECTORY22}/subsubdirectory1"

    protected final static String FURTHER_FILE_NAME_DIRECTORY1 = "${DIRECTORY1}/furtherFile1.txt"
    protected final static String FURTHER_FILE_NAME_DIRECTORY2 = "${DIRECTORY2}/furtherFile2.txt"
    protected final static String FURTHER_FILE_NAME_SUBDIRECTORY11 = "${SUBDIRECTORY11}/furtherFile11.txt"
    protected final static String FURTHER_FILE_NAME_SUBDIRECTORY21 = "${SUBDIRECTORY21}/furtherFile21.txt"
    protected final static String FURTHER_FILE_NAME_SUBDIRECTORY22 = "${SUBDIRECTORY22}/furtherFile22.txt"
    protected final static String FURTHER_FILE_NAME_SUBSUBDIRECTORY221 = "${SUBSUBDIRECTORY221}/furtherFile221.txt"

    /**
     * Files and folders which will be created for testing
     */
    protected final static List<String> ALL_FILES = [
            FURTHER_FILE_NAME,
            FURTHER_FILE_NAME_DIRECTORY1,
            FURTHER_FILE_NAME_DIRECTORY2,
            FURTHER_FILE_NAME_SUBDIRECTORY11,
            FURTHER_FILE_NAME_SUBDIRECTORY21,
            FURTHER_FILE_NAME_SUBDIRECTORY22,
            FURTHER_FILE_NAME_SUBSUBDIRECTORY221,
    ].asImmutable()

    /**
     * Files and folders which should be linked
     */
    protected final static List<String> FURTHER_FILES_LINKED = [
            FURTHER_FILE_NAME,
            DIRECTORY1,
            FURTHER_FILE_NAME_DIRECTORY2,
            SUBDIRECTORY21,
            SUBDIRECTORY22,
    ].asImmutable()

    static final int WORKFLOW_RUN_COUNT = 1

    Class<BamImportWorkflow> workflowComponentClass = BamImportWorkflow

    BamImportInitializationService bamImportInitializationService
    ExternalAlignmentLinkFileService externalAlignmentLinkFileService
    ExternalAlignmentWorkFileService externalAlignmentWorkFileService

    protected ExternallyProcessedBamFile bamFile

    /**
     * Bam/CRAM file path to be imported
     */
    Path filePath

    /**
     * Two folders are created to simulate
     * 1. realBamFilePath: stores all the files/folder of further files
     * 2. linkBamFilePath: link all the files to the realBamFilePath
     */
    @Shared
    Path realBamFilePath

    @Shared
    Path linkBamFilePath

    /**
     * An instance for bam import
     */
    protected BamImportInstance bamImportInstance

    protected abstract String getAlignmentFileName()

    protected abstract String getIndexFileName()

    /**
     * Helper to create bamImportInstance
     */
    private void initBamImportInstance(BamImportInstance.LinkOperation linkOperation) {
        log.debug("creating bamImportInstance")
        SessionUtils.withTransaction {
            bamFile = createBamFile([
                    importedFrom: filePath,
                    fileName    : filePath.fileName,
                    furtherFiles: FURTHER_FILES_LINKED,
                    workPackage : createExternalMergingWorkPackage(),
            ])
            bamImportInstance = createBamImportInstance([
                    externallyProcessedBamFiles: [bamFile],
                    workflowCreateState        : WorkflowCreateState.SUCCESS,
                    linkOperation              : linkOperation,
            ])
        }
        log.debug("finished creating bamImportInstance")
    }

    protected ExternalMergingWorkPackage createExternalMergingWorkPackage() {
        return createMergingWorkPackage()
    }

    protected void prepareFileSystemForFile() {
        Path realDir = additionalDataDirectory.resolve("real")
        Path linkDir = additionalDataDirectory.resolve("link")

        Path bamPath = prepareFileSystemCopyFiles(realDir)

        String bamFileName = prepareFileSystemLinkFiles(bamPath, linkDir, realDir)

        prepareFileSystemCreateAndLinkFurtherFiles(realDir, linkDir)

        realBamFilePath = realDir.resolve(bamFileName)
        linkBamFilePath = linkDir.resolve(bamFileName)
    }

    private Path prepareFileSystemCopyFiles(Path realDir) {
        Path bamPath = referenceDataDirectory.resolve(alignmentFileName)
        Path indexPath = referenceDataDirectory.resolve(indexFileName)
        String unixGroup = configService.testingGroup
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realDir, unixGroup)
        remoteShellHelper.executeCommandReturnProcessOutput("cp ${bamPath} ${indexPath} ${realDir}").assertExitCodeZeroAndStderrEmpty()
        return bamPath
    }

    private String prepareFileSystemLinkFiles(Path bamPath, Path linkDir, Path realDir) {
        String bamBaseName = bamPath.fileName
        String indexBaseName = Paths.get(indexFileName).fileName
        String unixGroup = configService.testingGroup
        [bamBaseName, indexBaseName].each { String fileName ->
            fileService.createLink(linkDir.resolve(fileName), realDir.resolve(fileName), unixGroup)
        }
        return bamBaseName
    }

    private void prepareFileSystemCreateAndLinkFurtherFiles(Path realDir, Path linkDir) {
        String unixGroup = configService.testingGroup
        ALL_FILES.each { String filePath ->
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realDir.resolve(filePath), unixGroup)
            fileService.createFileWithContent(realDir.resolve(filePath), "dummy", unixGroup, FileService.DEFAULT_FILE_PERMISSION, true)
            fileService.createLink(linkDir.resolve(filePath), realDir.resolve(filePath), unixGroup)
        }
    }

    protected void setupWorkflow(int expectedWorkflows = 1) {
        SessionUtils.withTransaction {
            bamImportInstance.refresh()
            assert bamImportInstance.externallyProcessedBamFiles
            newWorkflowRuns = bamImportInitializationService.createWorkflowRuns(bamImportInstance)
            assert newWorkflowRuns.size() == expectedWorkflows
            newWorkflowArtefact = WorkflowArtefact.findAllByProducedBy(newWorkflowRuns)
        }
    }

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        prepareFileSystemForFile()
        log.debug("Finish setup ${this.class.simpleName}")
    }

    @Unroll
    void "test BamImport, when COPY_AND_KEEP and #name, then copy files and do not adapt source"() {
        given:
        filePath = bamImportPath()
        initBamImportInstance(BamImportInstance.LinkOperation.COPY_AND_KEEP)
        setupWorkflow(WORKFLOW_RUN_COUNT)

        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(sourceLinked)

        where:
        sourceLinked | bamImportPath
        false        | { realBamFilePath }
        true         | { linkBamFilePath }

        name = (sourceLinked ? "source is linked" : "source is real file")
    }

    @Unroll
    void "test BamImport, when COPY_AND_LINK and #name, then copy files and link source to copied files"() {
        given:
        filePath = bamImportPath()
        initBamImportInstance(BamImportInstance.LinkOperation.COPY_AND_LINK)
        setupWorkflow(WORKFLOW_RUN_COUNT)

        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(sourceLinked)

        where:
        sourceLinked | bamImportPath
        false        | { realBamFilePath }
        true         | { linkBamFilePath }

        name = (sourceLinked ? "source is linked" : "source is real file")
    }

    @Unroll
    void "test BamImport, when LINK_SOURCE and #name, then link source into uuid"() {
        given:
        filePath = bamImportPath()
        initBamImportInstance(BamImportInstance.LinkOperation.LINK_SOURCE)
        setupWorkflow(WORKFLOW_RUN_COUNT)
        SessionUtils.withTransaction {
            bamImportInstance.refresh()
            bamImportInstance.externallyProcessedBamFiles.each { ExternallyProcessedBamFile bamFile ->
                bamFile.maximumReadLength = 100
                bamFile.md5sum = HelperUtils.randomMd5sum
                bamFile.save(flush: true)
            }
        }

        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(sourceLinked)

        where:
        sourceLinked | bamImportPath
        false        | { realBamFilePath }
        true         | { linkBamFilePath }

        name = (sourceLinked ? "source is linked" : "source is real file")
    }

    @SuppressWarnings("InvertedIfElse")
    protected void checkThatFileCopyingWasSuccessful(boolean isSourceLinked) {
        SessionUtils.withTransaction {
            bamImportInstance.refresh()
            assert bamImportInstance.externallyProcessedBamFiles.size() == 1

            bamImportInstance.externallyProcessedBamFiles.each { ExternallyProcessedBamFile bamFile ->
                assertBamProperties(bamFile)

                Path uuidDir = externalAlignmentWorkFileService.getDirectoryPath(bamFile)

                assertUuidPaths(bamFile, uuidDir, isSourceLinked)

                // Check ViewByPid directory in which all files are links to the uuid folder
                assertViewByPidStructure(bamFile, uuidDir)
            }
        }
    }

    private void assertBamProperties(ExternallyProcessedBamFile bamFile) {
        assert bamFile.maximumReadLength == 100
        assert bamFile.fileSize > 0
        assert bamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
    }

    /**
     * check the uuid structure
     * - if all paths exist
     * - depending on case: if they are links or real files/dirs
     */
    private void assertUuidPaths(ExternallyProcessedBamFile bamFile, Path uuidDir, boolean isSourceLinked) {
        ([
                bamFile.bamFileName,
                bamFile.baiFileName,
        ] + ALL_FILES).each {
            Path path = uuidDir.resolve(it)
            fileService.ensureFileIsReadableAndNotEmpty(path)
            if (shouldBeLinkInUuid(isSourceLinked, it)) {
                assert Files.isSymbolicLink(path)
            } else {
                assert !Files.isSymbolicLink(path)
            }
        }

        if (bamImportInstance.linkOperation != BamImportInstance.LinkOperation.LINK_SOURCE) {
            Path md5sumPath = uuidDir.resolve("${bamFile.bamFileName}.md5sum")
            fileService.ensureFileIsReadableAndNotEmpty(md5sumPath)
            assert !Files.isSymbolicLink(md5sumPath)

            Path md5sumBaiPath = uuidDir.resolve("${bamFile.baiFileName}.md5sum")
            fileService.ensureFileIsReadableAndNotEmpty(md5sumBaiPath)
            assert !Files.isSymbolicLink(md5sumBaiPath)
        }
    }

    /**
     * check, if a path should be a link in the uuid
     */
    private boolean shouldBeLinkInUuid(boolean isSourceLinked, String pathName) {
        return bamImportInstance.linkOperation == BamImportInstance.LinkOperation.LINK_SOURCE && (
                isSourceLinked ||
                        (pathName in FURTHER_FILES_LINKED) ||
                        // Use the bam file's own names instead of relying on file extensions
                        pathName == bamFile.bamFileName || pathName == bamFile.baiFileName
        )
    }

    /**
     * check the viewByPid structure: all files are links to the uuid folder
     */
    private void assertViewByPidStructure(ExternallyProcessedBamFile bamFile, Path uuidDir) {
        Path viewByPidDir = externalAlignmentLinkFileService.getDirectoryPath(bamFile)
        ([
                bamFile.bamFileName,
                bamFile.baiFileName,
        ] + FURTHER_FILES_LINKED).each {
            Path path = viewByPidDir.resolve(it)
            if (it.endsWithAny("directory1", "directory2")) {
                fileService.ensureDirIsReadable(path)
            } else {
                fileService.ensureFileIsReadableAndNotEmpty(path)
            }
            assert Files.isSymbolicLink(path)
            assert uuidDir.resolve(it).toRealPath() == path.toRealPath()
        }

        if (bamImportInstance.linkOperation != BamImportInstance.LinkOperation.LINK_SOURCE) {
            String md5sumFileName = "${bamFile.bamFileName}.md5sum"
            Path md5sumPath = viewByPidDir.resolve(md5sumFileName)
            fileService.ensureFileIsReadableAndNotEmpty(md5sumPath)
            assert Files.isSymbolicLink(md5sumPath)
            assert uuidDir.resolve(md5sumFileName).toRealPath() == md5sumPath.toRealPath()

            String md5sumBaiFileName = "${bamFile.baiFileName}.md5sum"
            Path md5sumBaiPath = viewByPidDir.resolve(md5sumBaiFileName)
            fileService.ensureFileIsReadableAndNotEmpty(md5sumBaiPath)
            assert Files.isSymbolicLink(md5sumBaiPath)
            assert uuidDir.resolve(md5sumBaiFileName).toRealPath() == md5sumBaiPath.toRealPath()
        }
    }

    @Override
    Duration getRunningTimeout() {
        return Duration.ofHours(5)
    }

    @Override
    String getWorkflowName() {
        return BamImportWorkflow.WORKFLOW
    }

    @Override
    protected Map<JobSubmissionOption, String> getJobSubmissionOptions() {
        return [
                (JobSubmissionOption.WALLTIME): Duration.ofMinutes(15).toString(),
                (JobSubmissionOption.MEMORY)  : "5g",
        ]
    }
}
