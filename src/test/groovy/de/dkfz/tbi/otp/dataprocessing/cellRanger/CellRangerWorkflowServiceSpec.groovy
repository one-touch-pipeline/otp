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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.*

class CellRangerWorkflowServiceSpec extends Specification implements CellRangerFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                CellRangerMergingWorkPackage,
                CellRangerConfig,
                DataFile,
                Individual,
                LibraryPreparationKit,
                FileType,
                MergingCriteria,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                ReferenceGenomeIndex,
                Run,
                FastqImportInstance,
                SeqTrack,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqType,
                SingleCellBamFile,
                SoftwareTool,
                ToolName,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder


    void "linkResultFiles, if all target files exist, then create link for each"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        createResultFiles(singleCellBamFile)

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        when:
        service.linkResultFiles(singleCellBamFile)

        then:
        singleCellBamFile.getLinkedResultFiles().each {
            Path path = it.toPath()
            assert Files.exists(path, LinkOption.NOFOLLOW_LINKS)
            assert Files.isSymbolicLink(path)
            Path link = Files.readSymbolicLink(path)
            assert !link.isAbsolute()
        }
    }


    @Unroll
    void "linkResultFiles, if file/directory '#missingFile' does not exist, then throw an assert"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        createResultFiles(singleCellBamFile)

        Path result = singleCellBamFile.resultDirectory.toPath()

        new FileService().deleteDirectoryRecursively(result.resolve(missingFile))

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        when:
        service.linkResultFiles(singleCellBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(missingFile)

        where:
        missingFile << SingleCellBamFile.CREATED_RESULT_FILES_AND_DIRS
    }

    void "cleanupOutputDirectory, when other files/directories exists in the result directory, delete them, but not the results"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()

        File outputDirectory = singleCellBamFile.outputDirectory
        File resultDirectory = singleCellBamFile.resultDirectory

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        and: 'create result files/directories'
        createResultFiles(singleCellBamFile)

        and: 'create additional directory'
        File subDir = new File(outputDirectory, 'subDir')
        assert subDir.mkdir()

        and: 'create additional file'
        File file = new File(outputDirectory, 'file')
        CreateFileHelper.createFile(file)

        when:
        service.cleanupOutputDirectory(singleCellBamFile)

        then: 'additional files/directories deleted'
        !subDir.exists()
        !file.exists()

        and: 'result are not deleted'
        singleCellBamFile.outputDirectory.exists()
        SingleCellBamFile.CREATED_RESULT_FILES_AND_DIRS.each {
            assert new File(resultDirectory, it).exists()
        }
    }

    void "deleteOutputDirectory, call deleteDirectoryRecursively for the work directory"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        Path workDirectory = singleCellBamFile.workDirectory.toPath()

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : Mock(FileService) {
                    1 * deleteDirectoryRecursively(workDirectory)
                    0 * _
                },
        ])

        when:
        service.deleteOutputDirectory(singleCellBamFile)

        then:
        noExceptionThrown()
    }

    void "correctFilePermissions, call correctPathPermissionRecursive for the work directory"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        Path workDirectory = singleCellBamFile.workDirectory.toPath()

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : Mock(FileService) {
                    1 * correctPathPermissionRecursive(workDirectory)
                    0 * _
                },
        ])

        when:
        service.correctFilePermissions(singleCellBamFile)

        then:
        noExceptionThrown()
    }
}
