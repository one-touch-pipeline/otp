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
package de.dkfz.tbi.otp.infrastructure.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.FastqFile

import java.nio.file.Path
import java.nio.file.Paths

class FastqcWorkFileServiceSpec extends Specification implements DataTest, FastqcDomainFactory, WorkflowSystemDomainFactory {

    static final Path BASE_DIRECTORY = Paths.get("/tmp/base/")

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
        ]
    }

    void "getDirectoryPath, when called with fastqc in old system, return path in project"() {
        given:
        Path linkPath = Path.of("/tmp/link")
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile()
        FastqcWorkFileService fastqcWorkFileService = new FastqcWorkFileService([
                fastqcLinkFileService: Mock(FastqcLinkFileService) {
                    1 * getDirectoryPath(fastqcProcessedFile) >> linkPath
                    0 * _
                },
                fileSystemService    : Mock(FileSystemService) {
                    0 * _
                },
                filestoreService     : Mock(FilestoreService) {
                    0 * _
                },
        ])
        Path expected = linkPath

        when:
        Path path = fastqcWorkFileService.getDirectoryPath(fastqcProcessedFile)

        then:
        path == expected
    }

    void "getDirectoryPath, when called with fastqc in new system, return path in work folder"() {
        given:
        Path uuidPath = Path.of("/tmp/uuid")
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        FastqcWorkFileService fastqcWorkFileService = new FastqcWorkFileService([
                fastqcLinkFileService: Mock(FastqcLinkFileService) {
                    0 * _
                },
                fileSystemService    : Mock(FileSystemService) {
                    0 * _
                },
                filestoreService     : Mock(FilestoreService) {
                    1 * getWorkFolderPath(fastqcProcessedFile.workflowArtefact.producedBy) >> uuidPath
                    0 * _
                },
        ])
        Path expected = uuidPath

        when:
        Path path = fastqcWorkFileService.getDirectoryPath(fastqcProcessedFile)

        then:
        path == expected
    }

    void "fastqcOutputPath, when called with pathInWorkFolder, then return correct path using pathInWorkFolder"() {
        given:
        Path uuidPath = Path.of("/tmp/uuid")
        String path = "something/file.zip"
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                pathInWorkFolder: path,
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        FastqcWorkFileService fastqcWorkFileService = new FastqcWorkFileService([
                fastqcLinkFileService: Mock(FastqcLinkFileService) {
                    0 * _
                },
                fileSystemService    : Mock(FileSystemService) {
                    0 * _
                },
                filestoreService     : Mock(FilestoreService) {
                    1 * getWorkFolderPath(fastqcProcessedFile.workflowArtefact.producedBy) >> uuidPath
                    0 * _
                },
        ])
        Path expectedPath = uuidPath.resolve(path)

        expect:
        fastqcWorkFileService.fastqcOutputPath(fastqcProcessedFile) == expectedPath
    }

    void "fastqcOutputPath, when called without pathInWorkFolder, but with uuid, then return correct path using uuid and not pathInWorkFolder"() {
        given:
        Path uuidPath = Path.of("/tmp/uuid")
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        FastqcWorkFileService fastqcWorkFileService = new FastqcWorkFileService([
                fastqcLinkFileService: Mock(FastqcLinkFileService) {
                    0 * _
                },
                fileSystemService    : Mock(FileSystemService) {
                    0 * _
                },
                filestoreService     : Mock(FilestoreService) {
                    1 * getWorkFolderPath(fastqcProcessedFile.workflowArtefact.producedBy) >> uuidPath
                    0 * _
                },
        ])
        String fileName = fastqcWorkFileService.fastqcFileName(fastqcProcessedFile)
        Path expectedPath = uuidPath.resolve(fileName)

        expect:
        fastqcWorkFileService.fastqcOutputPath(fastqcProcessedFile) == expectedPath
    }

    void "fastqcOutputPath, when called without pathInWorkFolder and no uuid, then return correct path in viewbypid structure"() {
        given:
        Path linkPath = Path.of("/tmp/link")
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile()
        FastqcWorkFileService fastqcWorkFileService = new FastqcWorkFileService([
                fastqcLinkFileService: Mock(FastqcLinkFileService) {
                    1 * getDirectoryPath(fastqcProcessedFile) >> linkPath
                    0 * _
                },
                fileSystemService    : Mock(FileSystemService) {
                    0 * _
                },
                filestoreService     : Mock(FilestoreService) {
                    0 * _
                },
        ])
        String fileName = fastqcWorkFileService.fastqcFileName(fastqcProcessedFile)
        Path expectedPath = linkPath.resolve(fileName)

        expect:
        fastqcWorkFileService.fastqcOutputPath(fastqcProcessedFile) == expectedPath
    }

    void "pathToFastQcResultFromSeqCenter, when called, then return correct path"() {
        given:
        String initDir = "/tmp/initDir"
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                sequenceFile: createSequenceDataFile([
                        initialDirectory: initDir,
                ]),
        ])
        FastqcWorkFileService service = new FastqcWorkFileService([
                fileSystemService: new TestFileSystemService(),
        ])
        String fastqcName = service.fastqcFileName(fastqcProcessedFile)
        Path expected = Paths.get(initDir, fastqcName)

        when:
        Path path = service.pathToFastQcResultFromSeqCenter(fastqcProcessedFile)

        then:
        path == expected
    }

    void "pathToFastQcResultMd5SumFromSeqCenter, when called, then return correct path"() {
        given:
        String initDir = "/tmp/initDir"
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                sequenceFile: createSequenceDataFile([
                        initialDirectory: initDir,
                ]),
        ])
        FastqcWorkFileService service = new FastqcWorkFileService([
                fileSystemService: new TestFileSystemService(),
        ])
        String fastqcName = service.fastqcFileName(fastqcProcessedFile).concat('.md5sum')
        Path expected = Paths.get(initDir, fastqcName)

        when:
        Path path = service.pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile)

        then:
        path == expected
    }
}
