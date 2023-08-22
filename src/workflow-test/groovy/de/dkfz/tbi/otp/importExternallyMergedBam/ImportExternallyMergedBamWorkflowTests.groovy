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
package de.dkfz.tbi.otp.importExternallyMergedBam

import spock.lang.Unroll

import de.dkfz.tbi.otp.WorkflowTestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.*
import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class ImportExternallyMergedBamWorkflowTests extends WorkflowTestCase implements DomainFactoryCore {

    protected ImportProcess importProcess

    protected final static String FURTHER_FILE_NAME = "furtherFile.txt"

    protected final static String DIRECTORY1 = "directory1"
    protected final static String DIRECTORY2 = "directory2"
    protected final static String SUBDIRECTORY11 = "${DIRECTORY1}/subdirectory1"
    protected final static String SUBDIRECTORY21 = "${DIRECTORY2}/subdirectory1"
    protected final static String SUBDIRECTORY22 = "${DIRECTORY2}/subdirectory2"
    protected final static String SUBSUBDIRECTORY221 = "${SUBDIRECTORY22}/subsubdirectory1"

    protected final static String FURTHER_FILE_NAME_DIRECTORY1 = "${DIRECTORY1}/${FURTHER_FILE_NAME}"
    protected final static String FURTHER_FILE_NAME_DIRECTORY2 = "${DIRECTORY2}/${FURTHER_FILE_NAME}"
    protected final static String FURTHER_FILE_NAME_SUBDIRECTORY11 = "${SUBDIRECTORY11}/${FURTHER_FILE_NAME}"
    protected final static String FURTHER_FILE_NAME_SUBDIRECTORY21 = "${SUBDIRECTORY21}/${FURTHER_FILE_NAME}"
    protected final static String FURTHER_FILE_NAME_SUBDIRECTORY22 = "${SUBDIRECTORY22}/${FURTHER_FILE_NAME}"
    protected final static String FURTHER_FILE_NAME_SUBSUBDIRECTORY221 = "${SUBSUBDIRECTORY221}/${FURTHER_FILE_NAME}"

    protected final static List<String> ALL_FILES = [
            FURTHER_FILE_NAME,
            FURTHER_FILE_NAME_DIRECTORY1,
            FURTHER_FILE_NAME_DIRECTORY2,
            FURTHER_FILE_NAME_SUBDIRECTORY11,
            FURTHER_FILE_NAME_SUBDIRECTORY21,
            FURTHER_FILE_NAME_SUBDIRECTORY22,
            FURTHER_FILE_NAME_SUBSUBDIRECTORY221,
    ].asImmutable()

    protected ExternallyProcessedBamFile createFile(Project project, String nameInfix, boolean furtherFiles, boolean useLink = false) {
        File baseDir = new File(ftpDir, nameInfix)
        File targetDir = useLink ? new File(baseDir, 'target') : baseDir

        String bamFileName = "bamFile${nameInfix}.bam"
        String baiFileName = "${bamFileName}.bai"

        List<String> fileNames = [
                baiFileName,
        ] + (furtherFiles ? ALL_FILES : [])

        createFilesWithContent(fileNames.collectEntries {
            [(new File(targetDir, it)): it]
        })

        File bam = new File(inputRootDirectory, "bamFiles/wgs/tumor_SOMEPID_merged.mdup.bam")

        remoteShellHelper.executeCommandReturnProcessOutput(realm,
                "cp ${bam} ${new File(targetDir, bamFileName)}"
        )

        if (useLink) {
            Map<String, String> map = ([
                    (bamFileName): bamFileName,
                    (baiFileName): baiFileName,
            ] + (furtherFiles ? [
                    (FURTHER_FILE_NAME)           : FURTHER_FILE_NAME,
                    (DIRECTORY1)                  : DIRECTORY1,
                    (FURTHER_FILE_NAME_DIRECTORY2): FURTHER_FILE_NAME_DIRECTORY2,
                    (SUBDIRECTORY21)              : SUBDIRECTORY21,
                    (SUBDIRECTORY22)              : SUBDIRECTORY22,
            ] : []))
            linkFileUtils.createAndValidateLinks(map.collectEntries { key, value ->
                [(new File(targetDir, key)): new File(baseDir, value)]
            }, realm)
        }

        File bamFile = new File(baseDir, bamFileName)
        ExternallyProcessedBamFile epmbf = DomainFactory.createExternallyProcessedBamFile(
                importedFrom: bamFile.path,
                fileName: bamFileName,
                fileSize: bamFile.size(),
                furtherFiles: furtherFiles ? [
                        FURTHER_FILE_NAME,
                        DIRECTORY1,
                        DIRECTORY2,
                ] : [],
        )
        epmbf.individual.project = project
        assert epmbf.individual.save(flush: true)
        return epmbf
    }

    @Override
    void setup() {
        SessionUtils.withTransaction {
            DomainFactory.createProcessingOptionLazy(
                    name: COMMAND_LOAD_MODULE_LOADER,
                    type: null,
                    value: ""
            )
            DomainFactory.createProcessingOptionLazy(
                    name: COMMAND_ACTIVATION_SAMTOOLS,
                    type: null,
                    value: "module load samtools/1.2"
            )
            DomainFactory.createProcessingOptionLazy(
                    name: COMMAND_SAMTOOLS,
                    type: null,
                    value: "samtools"
            )
            DomainFactory.createProcessingOptionLazy(
                    name: COMMAND_ACTIVATION_GROOVY,
                    type: null,
                    value: "module load groovy/2.4.15"
            )
            DomainFactory.createProcessingOptionLazy(
                    name: COMMAND_GROOVY,
                    type: null,
                    value: "groovy"
            )
        }
    }

    void setupFiles(boolean furtherFiles) {
        SessionUtils.withTransaction {
            Project project = createProject(realm: realm)
            createDirectories([new File(ftpDir), new File(projectService.getProjectDirectory(project).toString())])
            ExternallyProcessedBamFile epmbf01 = createFile(project, '1', furtherFiles, false)
            ExternallyProcessedBamFile epmbf02 = createFile(project, '2', furtherFiles, true)

            importProcess = new ImportProcess(
                    externallyProcessedBamFiles: [epmbf01, epmbf02],
                    state: ImportProcess.State.NOT_STARTED,
                    linkOperation: ImportProcess.LinkOperation.COPY_AND_LINK,
            ).save(flush: true)
        }
    }

    @Unroll
    void "testImportProcess_FilesHaveToBeCopied"() {
        given:
        setupFiles(furtherFiles)
        SessionUtils.withTransaction {
            importProcess.refresh()
            importProcess.linkOperation = ImportProcess.LinkOperation.COPY_AND_KEEP
            importProcess.save(flush: true)
        }

        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(importProcess, furtherFiles)

        Thread.sleep(1000) // needs a sleep, otherwise the file system cache has not yet the new value
        SessionUtils.withTransaction {
            FileSystem fs = fileSystemService.remoteFileSystem
            importProcess.externallyProcessedBamFiles.each {
                it.refresh()
                Path baseDirSource = fs.getPath(it.importedFrom).parent
                Path baseDirTarget = fs.getPath(it.importFolder.path)

                [
                        it.bamFileName,
                        it.baiFileName,
                        furtherFiles ? it.furtherFiles : [],
                ].flatten().each {
                    Path source = baseDirSource.resolve(it)
                    Path target = baseDirTarget.resolve(it)
                    assert !Files.isSymbolicLink(source) || Files.readSymbolicLink(source) != target
                }
            }
            return true
        }

        where:
        furtherFiles | _
        true         | _
        false        | _
    }

    @Unroll
    void "testImportProcess_FilesHaveToBeCopiedLinkedAndDeleted"() {
        given:
        setupFiles(furtherFiles)
        SessionUtils.withTransaction {
            importProcess.refresh()
            importProcess.linkOperation = ImportProcess.LinkOperation.COPY_AND_LINK
            importProcess.save(flush: true)
        }

        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(importProcess, furtherFiles)

        Thread.sleep(1000) // needs a sleep, otherwise the file system cache has not yet the new value

        SessionUtils.withTransaction {
            importProcess.externallyProcessedBamFiles.each {
                it.refresh()
                Path baseDirSource = Paths.get(it.importedFrom).parent
                Path baseDirTarget = Paths.get(it.importFolder.path)

                [
                        it.bamFileName,
                        it.baiFileName,
                        furtherFiles ? ALL_FILES : [],
                ].flatten().each {
                    Path source = baseDirSource.resolve(it)
                    Path target = baseDirTarget.resolve(it)
                    assert Files.isSymbolicLink(source)
                    fileService.ensureFileIsReadableAndNotEmpty(target, realm)
                    assert source.toRealPath() == target
                }
            }
            return true
        }

        where:
        furtherFiles | _
        true         | _
        false        | _
    }

    @Unroll
    void "testImportProcess_FilesHaveToBeLink"() {
        given:
        setupFiles(furtherFiles)
        SessionUtils.withTransaction {
            importProcess.refresh()
            importProcess.linkOperation = ImportProcess.LinkOperation.LINK_SOURCE
            importProcess.save(flush: true)
            importProcess.externallyProcessedBamFiles.each { ExternallyProcessedBamFile bamFile ->
                bamFile.maximumReadLength = 100
                bamFile.md5sum = HelperUtils.randomMd5sum
                bamFile.save(flush: true)
            }
        }

        when:
        execute()

        then:
        Thread.sleep(1000) // needs a sleep, otherwise the file system cache has not yet the new value

        SessionUtils.withTransaction {
            FileSystem fs = fileSystemService.remoteFileSystem
            importProcess.externallyProcessedBamFiles.each {
                it.refresh()
                assert it.fileSize > 0
                assert it.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED

                Path baseDirSource = fs.getPath(it.importedFrom).parent
                Path baseDirTarget = fs.getPath(it.importFolder.path)

                [
                        it.bamFileName,
                        it.baiFileName,
                        furtherFiles ? it.furtherFiles : [],
                ].flatten().each {
                    Path source = baseDirSource.resolve(it)
                    Path target = baseDirTarget.resolve(it)
                    assert Files.isSymbolicLink(target)
                    assert target.toRealPath() == source.toRealPath()
                }
            }
            return true
        }

        where:
        furtherFiles | _
        true         | _
        false        | _
    }

    protected void checkThatFileCopyingWasSuccessful(ImportProcess impPro, boolean furtherFiles) {
        SessionUtils.withTransaction {
            FileSystem fs = fileSystemService.remoteFileSystem
            importProcess = ImportProcess.get(impPro.id)
            assert importProcess.state == ImportProcess.State.FINISHED
            assert importProcess.externallyProcessedBamFiles.size() == 2

            importProcess.externallyProcessedBamFiles.each {
                it.refresh()
                assert it.fileSize > 0
                File baseDirectory = it.importFolder

                [
                        it.bamFileName,
                        it.baiFileName,
                        furtherFiles ? ALL_FILES : [],
                ].flatten().each {
                    checkThatFileExistAndIsNotLink(fs.getPath(baseDirectory.absolutePath, it as String))
                }

                if (furtherFiles) {
                    [
                            DIRECTORY1,
                            DIRECTORY2,
                            SUBDIRECTORY21,
                            SUBDIRECTORY22,
                    ].each {
                        checkThatDirectoryExistAndIsNotLink(fs.getPath(baseDirectory.absolutePath, it as String))
                    }
                }
                assert it.maximumReadLength == 100
            }
        }
    }

    protected void checkThatDirectoryExistAndIsNotLink(Path path) {
        fileService.ensureDirIsReadable(path, configService.defaultRealm)
    }

    protected void checkThatFileExistAndIsNotLink(Path path) {
        fileService.ensureFileIsReadableAndNotEmpty(path, realm)
        assert !Files.isSymbolicLink(path)
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/ImportExternallyMergedBamWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(5)
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    @Override
    String getJobName() {
        return 'ImportExternallyMergedBamJob'
    }
}
