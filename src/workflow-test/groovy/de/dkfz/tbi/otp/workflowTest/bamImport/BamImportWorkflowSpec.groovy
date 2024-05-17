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
package de.dkfz.tbi.otp.workflowTest.bamImport

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.bamImport.BamImportInitializationService
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowTest.AbstractWorkflowSpec

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class BamImportWorkflowSpec extends AbstractWorkflowSpec implements ExternalBamFactory {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(BamImportWorkflowSpec)

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

    /**
     * Test bam file that exists (in #referenceDataDirectory) and has correct content
     */
    static final private String BAM_ORIGINAL_FILENAME = "bamFiles/wgs/tumor_SOMEPID_merged.mdup.bam"

    static final int WORKFLOW_RUN_COUNT = 1

    Class<BamImportWorkflow> workflowComponentClass = BamImportWorkflow

    BamImportInitializationService bamImportInitializationService
    ExternallyProcessedBamFileService externallyProcessedBamFileService

    private ExternallyProcessedBamFile bamFile

    /**
     * Bam file path to be imported
     */
    Path bamFilePath

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
//    protected BamImportInstance bamImportInstance
    private BamImportInstance bamImportInstance

    /**
     * create bamImportInstance
     */
    private void initBamImportInstance(BamImportInstance.LinkOperation linkOperation) {
        log.debug("creating bamImportInstance")
        SessionUtils.withTransaction {
            bamFile = createBamFile([
                    importedFrom: bamFilePath,
                    fileName    : bamFilePath.fileName,
                    furtherFiles: FURTHER_FILES_LINKED,
            ])
            bamImportInstance = createBamImportInstance([
                    externallyProcessedBamFiles: [bamFile],
                    workflowCreateState        : WorkflowCreateState.SUCCESS,
                    linkOperation              : linkOperation,
            ])
        }
        log.debug("finished creating bamImportInstance")
    }

    private void prepareFileSystemForFile(String orgName) {
        Path realDir = additionalDataDirectory.resolve("real")
        Path linkDir = additionalDataDirectory.resolve("link")

        Path bamPath = prepareFileSystemCopyBamBai(orgName, realDir)

        String bamFileName = prepareFileSystemLinkBamBai(bamPath, linkDir, realDir)

        prepareFileSystemCreateAndLinkFurtherFiles(realDir, linkDir)

        realBamFilePath = realDir.resolve(bamFileName)
        linkBamFilePath = linkDir.resolve(bamFileName)
    }

    private Path prepareFileSystemCopyBamBai(String orgName, Path realDir) {
        Path bamPath = referenceDataDirectory.resolve(orgName)
        Path baiPath = referenceDataDirectory.resolve(orgName + ".bai")
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realDir)
        remoteShellHelper.executeCommandReturnProcessOutput("cp ${bamPath} ${baiPath} ${realDir}").assertExitCodeZeroAndStderrEmpty()
        return bamPath
    }

    private String prepareFileSystemLinkBamBai(Path bamPath, Path linkDir, Path realDir) {
        String bamFileName = bamPath.fileName
        String baiFileName = "${bamFileName}.bai"
        [bamFileName, baiFileName].each { String filePath ->
            fileService.createLink(linkDir.resolve(filePath), realDir.resolve(filePath))
        }
        return bamFileName
    }

    private void prepareFileSystemCreateAndLinkFurtherFiles(Path realDir, Path linkDir) {
        ALL_FILES.each { String filePath ->
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realDir.resolve(filePath))
            fileService.createFileWithContent(realDir.resolve(filePath), "dummy", FileService.DEFAULT_FILE_PERMISSION, true)
            fileService.createLink(linkDir.resolve(filePath), realDir.resolve(filePath))
        }
    }

    protected void setupWorkflow(int expectedWorkflows = 1) {
        SessionUtils.withTransaction {
            bamImportInstance.refresh()
            assert bamImportInstance.externallyProcessedBamFiles
            List<WorkflowRun> workflowRuns = bamImportInitializationService.createWorkflowRuns(bamImportInstance)
            assert workflowRuns.size() == expectedWorkflows
        }
    }

    @Override
    void setup() {
        prepareFileSystemForFile(BAM_ORIGINAL_FILENAME)
    }

    @Unroll
    void "test BamImport, when COPY_AND_KEEP and #name, then copy files and do not adapt source"() {
        given:
        bamFilePath = bamImportPath()
        initBamImportInstance(BamImportInstance.LinkOperation.COPY_AND_KEEP)
        setupWorkflow(WORKFLOW_RUN_COUNT)

        when:
        execute(WORKFLOW_RUN_COUNT)

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
        bamFilePath = bamImportPath()
        initBamImportInstance(BamImportInstance.LinkOperation.COPY_AND_LINK)
        setupWorkflow(WORKFLOW_RUN_COUNT)

        when:
        execute(WORKFLOW_RUN_COUNT)

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
        bamFilePath = bamImportPath()
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
        execute(WORKFLOW_RUN_COUNT)

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

                Path uuidDir = externallyProcessedBamFileService.getImportFolder(bamFile, PathOption.REAL_PATH)

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
    }

    /**
     * check, if a path should be a link in the uuid
     */
    private boolean shouldBeLinkInUuid(boolean isSourceLinked, String pathName) {
        return bamImportInstance.linkOperation == BamImportInstance.LinkOperation.LINK_SOURCE && (
                isSourceLinked || (pathName in FURTHER_FILES_LINKED) || pathName.endsWithAny('.bam', '.bai')
        )
    }

    /**
     * check the viewByPid structure
     */
    private void assertViewByPidStructure(ExternallyProcessedBamFile bamFile, Path uuidDir) {
        Path viewByPidDir = externallyProcessedBamFileService.getImportFolder(bamFile)
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
