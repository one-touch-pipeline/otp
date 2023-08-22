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
package de.dkfz.tbi.otp.project.dta

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.validation.ValidationException
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.ProcessOutput

import java.nio.file.*

@Rollback
@Integration
class DataTransferAgreementServiceIntegrationSpec extends Specification implements DocumentFactory, UserAndRoles {

    DataTransferAgreementService dataTransferAgreementService
    TestConfigService configService
    ProjectService projectService

    @TempDir
    Path tempDir

    void setup() {
        dataTransferAgreementService = new DataTransferAgreementService(
                executionHelperService: Mock(ExecutionHelperService),
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService: new FileService([
                        remoteShellHelper: Mock(RemoteShellHelper) {
                            _ * executeCommandReturnProcessOutput(_) >> { String command ->
                                return new ProcessOutput(command, '', 0)
                            }
                        }
                ]),
                projectService: projectService,
        )
        configService.addOtpProperties(tempDir)
    }

    void cleanup() {
        configService.clean()
    }

    void "createDta, should create a dta with file when file is given"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement([:], false)
        List<MultipartFile> files = [createMultipartFile()]

        when:
        DataTransferAgreement dataTransferAgreement = null

        dataTransferAgreement = doWithAuth(OPERATOR) {
            dataTransferAgreementService.persistDtaWithDtaDocuments(dta, files)
        }

        then:
        noExceptionThrown()
        DataTransferAgreement.get(dataTransferAgreement.id) == dataTransferAgreement
        dataTransferAgreement.dataTransferAgreementDocuments.size() == 1
    }

    void "createDta, should fail with validation error when project is null"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement([project: null], false)
        List<MultipartFile> files = [createMultipartFile()]

        when:
        doWithAuth(OPERATOR) {
            dataTransferAgreementService.persistDtaWithDtaDocuments(dta, files)
        }

        then:
        thrown(AssertionError)
    }

    void "addFileToDta, should add a file to a dta when file and dta is given"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement()

        String fileName = "demoFileName"
        String fileEnding = ".sample"

        MultipartFile file = createMultipartFile("demo", fileName + fileEnding)

        when:
        dta = doWithAuth(OPERATOR) {
            dataTransferAgreementService.addFileToDta(dta, file)
        }

        then:
        noExceptionThrown()
        dta.dataTransferAgreementDocuments.size() > 0
        DataTransferAgreement.get(dta.id).dataTransferAgreementDocuments[0].fileName.contains(fileName)
    }

    void "addFileToDta, should fail when file is empty"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement()

        MultipartFile file = createMultipartFile("demo", "demo", new byte[0])

        when:
        dta = doWithAuth(OPERATOR) {
            dataTransferAgreementService.addFileToDta(dta, file)
        }

        then:
        thrown(FileIsEmptyException)
    }

    void "addFileToDta, should create the file on the file system"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement()
        MultipartFile inputFile = createMultipartFile("demo", "demo", "content".bytes)
        Path resultFile = null

        when:
        resultFile = doWithAuth(OPERATOR) {
            dta = dataTransferAgreementService.addFileToDta(dta, inputFile)
            Paths.get(dataTransferAgreementService.getPathOnRemoteFileSystem(dta.dataTransferAgreementDocuments[0]).toString())
        }

        then:
        Files.exists(resultFile)
        resultFile.bytes == inputFile.bytes
    }

    void "addFilesToDta, should fail when DTA is not found"() {
        given:
        createUserAndRoles()
        List<MultipartFile> inputFiles = [
                createMultipartFile("demo1", "demo1", "content".bytes),
                createMultipartFile("demo2", "demo2", "content".bytes),
        ]
        DataTransferAgreement unsavedDta = createDataTransferAgreement([:], false)

        when:
        doWithAuth(OPERATOR) {
            dataTransferAgreementService.addFilesToDta(unsavedDta, inputFiles)
        }

        then:
        thrown(OtpException)
    }

    void "getDataTransferAgreementDocumentContent, should fail when no DTA document is given"() {
        given:
        createUserAndRoles()
        DataTransferAgreementDocument dtaDoc = null

        when:
        doWithAuth(OPERATOR) {
            dataTransferAgreementService.getDataTransferAgreementDocumentContent(dtaDoc)
        }

        then:
        thrown(FileNotFoundException)
    }

    void "getDataTransferAgreementDocumentContent, should return document content when DTA document exists"() {
        given:
        createUserAndRoles()
        MultipartFile inputFile = createMultipartFile("demo", "demo", "content".bytes)
        DataTransferAgreement dta = createDataTransferAgreement()
        dta = dataTransferAgreementService.addFileToDta(dta, inputFile)
        byte[] content

        when:
        content = doWithAuth(OPERATOR) {
            dataTransferAgreementService.getDataTransferAgreementDocumentContent(dta.dataTransferAgreementDocuments[0])
        }

        then:
        noExceptionThrown()
        content == inputFile.bytes
    }

    void "updateDataTransferAgreementComment, should update comment"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement([
                comment: "old comment text",
        ])

        String newComment = "new comment text"

        when:
        dta = doWithAuth(OPERATOR) {
            dataTransferAgreementService.updateDataTransferAgreementComment(dta, newComment)
        }

        then:
        noExceptionThrown()
        dta.comment == newComment
    }

    void "updateDataTransferAgreementComment, should fail when comment is blank"() {
        given:
        createUserAndRoles()
        DataTransferAgreement dta = createDataTransferAgreement([
                comment: "old comment text",
        ])

        String newComment = ""

        when:
        doWithAuth(OPERATOR) {
            dataTransferAgreementService.updateDataTransferAgreementComment(dta, newComment)
        }

        then:
        thrown(ValidationException)
    }

    void "deleteDataTransferAgreement, should delete the DTA completely in the database"() {
        given:
        createUserAndRoles()
        MultipartFile inputFile = createMultipartFile("demo", "demo", "content".bytes)
        DataTransferAgreement dta = createDataTransferAgreement()
        dta = dataTransferAgreementService.addFileToDta(dta, inputFile)

        createDataTransfer([
                dataTransferAgreement: dta,
        ])

        dta.refresh()

        when:
        doWithAuth(OPERATOR) {
            dataTransferAgreementService.deleteDataTransferAgreement(dta)
        }

        then:
        noExceptionThrown()
        !DataTransferAgreement.get(dta.id)
        !DataTransferAgreementDocument.get(dta.dataTransferAgreementDocuments[0].id)
        DataTransfer.findAllByDataTransferAgreement(dta).isEmpty()
    }

    void "deleteDataTransferAgreement, should delete all DTA documents on the filesystem"() {
        given:
        createUserAndRoles()
        MultipartFile inputFile = createMultipartFile("demo", "demo", "content".bytes)
        DataTransferAgreement dta = createDataTransferAgreement()
        dta = dataTransferAgreementService.addFileToDta(dta, inputFile)
        Path resultFile = Paths.get(dataTransferAgreementService.getPathOnRemoteFileSystem(dta.dataTransferAgreementDocuments[0]).toString())

        when:
        dta = doWithAuth(OPERATOR) {
            dataTransferAgreementService.deleteDataTransferAgreement(dta)
        }

        then:
        noExceptionThrown()
        !Files.exists(resultFile)
    }
}
