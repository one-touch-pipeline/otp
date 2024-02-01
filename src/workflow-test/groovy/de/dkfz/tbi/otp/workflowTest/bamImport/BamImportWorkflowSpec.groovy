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
                    fileName: bamFilePath.fileName,
                    furtherFiles: FURTHER_FILES_LINKED,
            ])
            bamImportInstance = createImportInstance([
                    externallyProcessedBamFiles: [bamFile],
                    workflowCreateState        : WorkflowCreateState.SUCCESS,
                    state                      : BamImportInstance.State.NOT_STARTED,
                    linkOperation              : linkOperation,
            ])
        }
        log.debug("finished creating bamImportInstance")
    }

    private void prepareFileSystemForFile(String orgName) {
        Path realDir = additionalDataDirectory.resolve("real")
        Path linkDir = additionalDataDirectory.resolve("link")

        // step 1; copy bam and bai files
        Path bamPath = referenceDataDirectory.resolve(orgName)
        Path baiPath = referenceDataDirectory.resolve(orgName + ".bai")
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realDir)
        remoteShellHelper.executeCommandReturnProcessOutput("cp ${bamPath} ${baiPath} ${realDir}").assertExitCodeZeroAndStderrEmpty()

        // step 2; link bam and bai files
        String bamFileName = bamPath.fileName
        String baiFileName = "${bamFileName}.bai"
        [ bamFileName, baiFileName ].each { String filePath ->
            fileService.createLink(linkDir.resolve(filePath), realDir.resolve(filePath))
        }

        // step 3: further files
        ALL_FILES.each { String filePath ->
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realDir.resolve(filePath))
            fileService.createFileWithContent(realDir.resolve(filePath), "dummy", FileService.DEFAULT_FILE_PERMISSION, true)
            fileService.createLink(linkDir.resolve(filePath), realDir.resolve(filePath))
        }

        realBamFilePath = realDir.resolve(bamFileName)
        linkBamFilePath = linkDir.resolve(bamFileName)
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
    void "testImportProcess_FilesHaveToBeCopied"() {
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
    }

    @Unroll
    void "testImportProcess_FilesHaveToBeCopiedLinkedAndDeleted"() {
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
    }

    @Unroll
    void "testImportProcess_FilesHaveToBeLink"() {
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
    }

    @SuppressWarnings("InvertedIfElse")
    protected void checkThatFileCopyingWasSuccessful(boolean isSourceLinked) {
        SessionUtils.withTransaction {
            bamImportInstance.refresh()
            assert bamImportInstance.externallyProcessedBamFiles.size() == 1

            bamImportInstance.externallyProcessedBamFiles.each { ExternallyProcessedBamFile bamFile ->
                assert bamFile.maximumReadLength == 100
                assert bamFile.fileSize > 0
                assert bamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED

                // Check the UUID files which are all real files except in the 3. testcase
                // In 3. testcase some files are not directly linked since its directory is linked
                Path uuidDir = externallyProcessedBamFileService.getImportFolder(bamFile, PathOption.REAL_PATH)
                ([
                        bamFile.bamFileName,
                        bamFile.baiFileName,
                ] + ALL_FILES).each {
                    Path path = uuidDir.resolve(it)
                    fileService.ensureFileIsReadableAndNotEmpty(path)
                    if (bamImportInstance.linkOperation == BamImportInstance.LinkOperation.LINK_SOURCE) {
                        if (isSourceLinked) { // x == 2: all files are symbolic links since source of import dir is linked
                            assert Files.isSymbolicLink(path)
                        } else { // x == 1: some files are not linked since its directory in real is linked
                            if (!(it in FURTHER_FILES_LINKED + [bamFile.bamFileName, bamFile.baiFileName])) {
                                assert !Files.isSymbolicLink(path)
                            } else {
                                assert Files.isSymbolicLink(path)
                            }
                        }
                    } else {
                        assert !Files.isSymbolicLink(path)
                    }
                }

                // Check ViewByPid directory in which all files are links to the uuid folder
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
}
