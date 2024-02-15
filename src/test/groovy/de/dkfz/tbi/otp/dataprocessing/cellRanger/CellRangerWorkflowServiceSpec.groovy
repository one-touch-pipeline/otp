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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.*

class CellRangerWorkflowServiceSpec extends Specification implements CellRangerFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                CellRangerMergingWorkPackage,
                CellRangerConfig,
                RawSequenceFile,
                Individual,
                LibraryPreparationKit,
                FastqFile,
                FileType,
                MergingCriteria,
                Pipeline,
                Project,
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

    @TempDir
    Path tempDir

    void "linkResultFiles, if all target files exist, then create link for each"() {
        given:
        new TestConfigService(tempDir)

        SingleCellBamFile singleCellBamFile = createBamFile()
        Map map = [
                link1: 'resultPathName',
                link2: 'resultPathName2',
        ]

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                cellRangerWorkFileService: Mock(CellRangerWorkFileService) {
                    1 * getDirectoryPath(singleCellBamFile) >> tempDir.resolve('work')
                    1 * getResultDirectory(singleCellBamFile) >> tempDir.resolve('result')
                    1 * getFileMappingForLinks(singleCellBamFile) >> map
                    0 * _
                },
                fileSystemService        : Mock(FileSystemService) {
                    _ * getRemoteFileSystem() >> FileSystems.default
                    0 * _
                },
                fileService              : Mock(FileService) {
                    1 * createLink(tempDir.resolve('work').resolve('link1'), tempDir.resolve('result').resolve('resultPathName'), singleCellBamFile.project.unixGroup)
                    1 * createLink(tempDir.resolve('work').resolve('link2'), tempDir.resolve('result').resolve('resultPathName2'), singleCellBamFile.project.unixGroup)
                    0 * _
                },
        ])

        when:
        service.linkResultFiles(singleCellBamFile)

        then:
        true
    }

    @Unroll
    void "linkResultFiles, if file/directory '#missingFile' does not exist, then throw an assert"() {
        given:
        new TestConfigService(tempDir)

        SingleCellBamFile singleCellBamFile = createBamFile()
        Map map = [
                link1: missingFile,
        ]
        Path workDirectory = tempDir.resolve('work')
        Path resultDirectory = tempDir.resolve('result')
        createResultFiles(resultDirectory)

        new FileService().deleteDirectoryRecursively(resultDirectory.resolve(missingFile))
        CellRangerWorkflowService service = new CellRangerWorkflowService([
                cellRangerWorkFileService: Mock(CellRangerWorkFileService) {
                    1 * getDirectoryPath(singleCellBamFile) >> workDirectory
                    1 * getResultDirectory(singleCellBamFile) >> resultDirectory
                    1 * getFileMappingForLinks(singleCellBamFile) >> map
                    0 * _
                },
                fileSystemService        : Mock(FileSystemService) {
                    _ * getRemoteFileSystem() >> FileSystems.default
                    0 * _
                },
                fileService              : new FileService(),
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
        new TestConfigService(tempDir)

        Path workDirectory = tempDir.resolve('work')
        Path outputDirectory = Files.createDirectories(workDirectory.resolve('output'))
        Path resultDirectory = Files.createDirectories(outputDirectory.resolve('result'))

        SingleCellBamFile singleCellBamFile = createBamFile()

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                cellRangerWorkFileService: Mock(CellRangerWorkFileService) {
                    1 * getOutputDirectory(singleCellBamFile) >> outputDirectory
                    1 * getResultDirectory(singleCellBamFile) >> resultDirectory
                    0 * _
                },
                fileSystemService        : Mock(FileSystemService) {
                    _ * getRemoteFileSystem() >> FileSystems.default
                    0 * _
                },
                fileService              : new FileService(),
        ])

        and: 'create result files/directories'
        createResultFiles(resultDirectory)

        and: 'create additional directory'
        Path subDir = CreateFileHelper.createFile(outputDirectory.resolve('subDir'))
        and: 'create additional file'
        Path file = CreateFileHelper.createFile(outputDirectory.resolve('file'))

        when:
        service.cleanupOutputDirectory(singleCellBamFile)

        then: 'additional files/directories deleted'
        !Files.exists(subDir)
        !Files.exists(file)

        and: 'result are not deleted'
        Files.exists(outputDirectory)
        SingleCellBamFile.CREATED_RESULT_FILES_AND_DIRS.each {
            assert Files.exists(resultDirectory.resolve(it))
        }
    }

    void "deleteOutputDirectory, call deleteDirectoryRecursively for the work directory"() {
        given:
        new TestConfigService(tempDir)
        Path workDirectory = tempDir.resolve('work')

        SingleCellBamFile singleCellBamFile = createBamFile()

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                cellRangerWorkFileService: Mock(CellRangerWorkFileService) {
                    1 * getDirectoryPath(_) >> workDirectory
                    0 * _
                },
                fileSystemService        : Mock(FileSystemService) {
                    _ * getRemoteFileSystem() >> FileSystems.default
                    0 * _
                },
                fileService              : Mock(FileService) {
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
        new TestConfigService(tempDir)

        SingleCellBamFile singleCellBamFile = createBamFile()

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                cellRangerWorkFileService: Mock(CellRangerWorkFileService) {
                    1 * getDirectoryPath(_) >> null
                    0 * _
                },
                fileSystemService        : Mock(FileSystemService) {
                    _ * getRemoteFileSystem() >> FileSystems.default
                    0 * _
                },
                fileService              : Mock(FileService) {
                    1 * correctPathPermissionAndGroupRecursive(null, _)
                    0 * _
                },
        ])

        when:
        service.correctFilePermissions(singleCellBamFile)

        then:
        noExceptionThrown()
    }
}
