/*
 * Copyright 2011-2019 The OTP authors
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

package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.*
import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class ImportExternallyMergedBamWorkflowTests extends WorkflowTestCase {

    ImportProcess importProcess


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


    protected ExternallyProcessedMergedBamFile createFile(Project project, String nameInfix, boolean useLink = false) {
        File baseDir = new File(ftpDir, nameInfix)
        File targetDir = useLink ? new File(baseDir, 'target') : baseDir

        String bamFileName = "bamFile${nameInfix}.bam"
        String baiFileName = "${bamFileName}.bai"

        List<String> fileNames = [
                baiFileName,
        ] + ALL_FILES

        createFilesWithContent(fileNames.collectEntries {
            [(new File(targetDir, it)): it]
        })

        File bam = new File(getInputRootDirectory(), "bamFiles/wgs/tumor_SOMEPID_merged.mdup.bam")

        remoteShellHelper.executeCommandReturnProcessOutput(realm,
                "cp ${bam} ${new File(targetDir, bamFileName)}"
        )

        if (useLink) {
            linkFileUtils.createAndValidateLinks([
                    (bamFileName)                 : bamFileName,
                    (baiFileName)                 : baiFileName,
                    (FURTHER_FILE_NAME)           : FURTHER_FILE_NAME,
                    (DIRECTORY1)                  : DIRECTORY1,
                    (FURTHER_FILE_NAME_DIRECTORY2): FURTHER_FILE_NAME_DIRECTORY2,
                    (SUBDIRECTORY21)              : SUBDIRECTORY21,
                    (SUBDIRECTORY22)              : SUBDIRECTORY22,
            ].collectEntries { key, value ->
                [(new File(targetDir, key)): new File(baseDir, value)]
            }, realm)
        }

        File bamFile = new File(baseDir, bamFileName)
        ExternallyProcessedMergedBamFile epmbf = DomainFactory.createExternallyProcessedMergedBamFile(
                importedFrom: bamFile.path,
                fileName: bamFileName,
                fileSize: bamFile.size(),
                furtherFiles: [
                        FURTHER_FILE_NAME,
                        DIRECTORY1,
                        DIRECTORY2,
                ]
        )
        epmbf.individual.project = project
        assert epmbf.individual.save(flush: true)
        return epmbf
    }

    @Override
    void setup() {
        SessionUtils.withNewSession {
            Project project = DomainFactory.createProject(realm: realm)
            createDirectories([new File(ftpDir), project.projectDirectory])

            ExternallyProcessedMergedBamFile epmbf01 = createFile(project, '1', false)
            ExternallyProcessedMergedBamFile epmbf02 = createFile(project, '2', true)

            importProcess = new ImportProcess(
                    externallyProcessedMergedBamFiles: [epmbf01, epmbf02],
                    state: ImportProcess.State.NOT_STARTED,
                    replaceSourceWithLink: true
            ).save(flush: true)

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
            DomainFactory.createProcessingOptionLazy(
                    name: FILESYSTEM_BAM_IMPORT,
                    type: null,
                    value: realm.name
            )
        }
    }

    void "testImportProcess_FilesHaveToBeCopied"() {
        given:
        SessionUtils.withNewSession {
            importProcess.refresh()
            importProcess.replaceSourceWithLink = false
            importProcess.save(flush: true)
        }

        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(importProcess)

        Thread.sleep(1000) //needs a sleep, otherwise the file system cache has not yet the new value
        SessionUtils.withNewSession {
            importProcess.externallyProcessedMergedBamFiles.each {
                it.refresh()
                File baseDirSource = new File(it.importedFrom).parentFile
                File baseDirTarget = it.importFolder

                [
                        it.bamFileName,
                        it.baiFileName,
                        it.furtherFiles,
                ].flatten().each {
                    Path source = new File(baseDirSource, it).toPath()
                    Path target = new File(baseDirTarget, it).toPath()
                    assert !Files.isSymbolicLink(source) || Files.readSymbolicLink(source) != target
                }
            }
            return true
        }
    }

    void "testImportProcess_FilesHaveToBeCopiedLinkedAndDeleted"() {
        when:
        execute()

        then:
        checkThatFileCopyingWasSuccessful(importProcess)

        Thread.sleep(1000) //needs a sleep, otherwise the file system cache has not yet the new value

        SessionUtils.withNewSession {
            importProcess.externallyProcessedMergedBamFiles.each {
                File baseDirSource = new File(it.importedFrom).parentFile
                File baseDirTarget = it.importFolder

                [
                        it.bamFileName,
                        it.baiFileName,
                        ALL_FILES,
                ].flatten().each {
                    Path source = new File(baseDirSource, it).toPath()
                    Path target = new File(baseDirTarget, it).toPath()
                    assert Files.isSymbolicLink(source)
                    assert source.toRealPath() == target
                }
            }
            return true
        }
    }

    protected void checkThatFileCopyingWasSuccessful(ImportProcess impPro) {
        SessionUtils.withNewSession {
            FileSystem fs = fileSystemService.filesystemForBamImport
            importProcess = ImportProcess.get(impPro.id)
            assert importProcess.state == ImportProcess.State.FINISHED
            assert 2 == importProcess.externallyProcessedMergedBamFiles.size()

            importProcess.externallyProcessedMergedBamFiles.each {
                it.refresh()
                assert it.fileSize > 0
                File baseDirectory = it.importFolder

                [
                        it.bamFileName,
                        it.baiFileName,
                        ALL_FILES,
                ].flatten().each {
                    checkThatFileExistAndIsNotLink(fs.getPath(baseDirectory.absolutePath, it as String))
                }

                [
                        DIRECTORY1,
                        DIRECTORY2,
                        SUBDIRECTORY21,
                        SUBDIRECTORY22,
                ].each {
                    checkThatDirectoryExistAndIsNotLink(fs.getPath(baseDirectory.absolutePath, it as String))
                }

                assert it.maximumReadLength == 100
            }
        }
    }

    protected static void checkThatDirectoryExistAndIsNotLink(Path path) {
        assert Files.exists(path, LinkOption.NOFOLLOW_LINKS)
        assert Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
    }

    protected static void checkThatFileExistAndIsNotLink(Path path) {
        assert Files.exists(path, LinkOption.NOFOLLOW_LINKS)
        assert Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        assert Files.size(path) > 0
    }


    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/ImportExternallyMergedBamWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofMinutes(20)
    }
}
