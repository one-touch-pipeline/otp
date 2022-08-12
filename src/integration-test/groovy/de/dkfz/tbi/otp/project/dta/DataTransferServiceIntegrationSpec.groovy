/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.project.dta

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.ProcessOutput

import java.nio.file.FileSystems
import java.nio.file.Path

@Rollback
@Integration
class DataTransferServiceIntegrationSpec extends Specification implements DocumentFactory, UserAndRoles {

    DataTransferService dataTransferService
    TestConfigService configService
    ProjectService projectService

    @TempDir
    Path tempDir

    void setupData() {
        createUserAndRoles()
        dataTransferService = new DataTransferService(
                executionHelperService: Mock(ExecutionHelperService),
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService: new FileService([
                        remoteShellHelper: Mock(RemoteShellHelper) {
                            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                                return new ProcessOutput(command, '', 0)
                            }
                        }
                ]),
                securityService : Mock(SecurityService) {
                    _ * getCurrentUser() >> getUser(ADMIN)
                },
                dataTransferAgreementService: new DataTransferAgreementService(projectService: projectService),
        )
        configService.addOtpProperties(tempDir)
    }

    void cleanup() {
        configService.clean()
    }

    void "createDataTransfer, should create data transfer with files when files are given"() {
        given:
        setupData()
        List<MultipartFile> files = [createMultipartFile(), createMultipartFile()]
        DataTransfer unsavedTransfer = createDataTransfer([:], false)

        when:
        DataTransfer returnedTransfer = null

        returnedTransfer = doWithAuth(OPERATOR) {
            dataTransferService.persistDataTransferWithTransferDocuments(unsavedTransfer, files)
        }

        then:
        noExceptionThrown()
        DataTransfer.get(returnedTransfer.id) == returnedTransfer
        returnedTransfer.dataTransferDocuments.size() == 2
    }

    void "addFileToDataTransfer, should add a file to a transfer when file and transfer is given"() {
        given:
        setupData()
        DataTransfer transfer = createDataTransfer()
        MultipartFile file = createMultipartFile()

        when:
        DataTransfer resultTransfer = null

        resultTransfer = doWithAuth(OPERATOR) {
            dataTransferService.addFileToDataTransfer(transfer, file)
        }

        then:
        resultTransfer.dataTransferDocuments.size() == 1
        DataTransfer.get(transfer.id).dataTransferDocuments[0].fileName.contains(file.originalFilename)
    }

    void "addFileToDataTransfer, should fail when file is empty"() {
        given:
        setupData()
        DataTransfer transfer = createDataTransfer()
        MultipartFile file = createMultipartFile("demo", "demo", new byte[0])

        when:
        doWithAuth(OPERATOR) {
            dataTransferService.addFileToDataTransfer(transfer, file)
        }

        then:
        thrown(FileIsEmptyException)
    }

    void "addFilesToDataTransfer, should fail when transfer is not found"() {
        given:
        setupData()
        DataTransfer transfer = createDataTransfer([:], false)
        List<MultipartFile> files = [createMultipartFile(), createMultipartFile()]

        when:
        doWithAuth(OPERATOR) {
            dataTransferService.addFilesToDataTransfer(transfer, files)
        }

        then:
        thrown(DataTransferNotFoundException)
    }

    void "getDataTransferDocumentContent, should fail when no data transfer is given"() {
        given:
        setupData()
        DataTransferDocument dataTransferDocument = null

        when:
        doWithAuth(OPERATOR) {
            dataTransferService.getDataTransferDocumentContent(dataTransferDocument)
        }

        then:
        thrown(FileNotFoundException)
    }

    void "getDataTransferDocumentContent, should return the document content when it exists"() {
        given:
        setupData()
        MultipartFile inputFile = createMultipartFile()
        DataTransfer dataTransfer = createDataTransfer()
        dataTransfer = dataTransferService.addFileToDataTransfer(dataTransfer, inputFile)

        when:
        byte[] content

        content = doWithAuth(OPERATOR) {
            dataTransferService.getDataTransferDocumentContent(dataTransfer.dataTransferDocuments[0])
        }

        then:
        noExceptionThrown()
        content == inputFile.bytes
    }

    void "updateDataTransferComment, should update the comment"() {
        given:
        setupData()
        DataTransfer transfer = createDataTransfer([
                comment: "old comment text",
        ])

        String newComment = "new comment text"

        when:
        doWithAuth(OPERATOR) {
            dataTransferService.updateDataTransferComment(transfer, newComment)
        }

        then:
        noExceptionThrown()
        DataTransfer.get(transfer.id).comment == newComment
    }
}
