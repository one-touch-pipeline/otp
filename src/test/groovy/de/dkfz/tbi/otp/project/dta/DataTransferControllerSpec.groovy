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
package de.dkfz.tbi.otp.project.dta

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import de.dkfz.tbi.otp.OtpException

import static javax.servlet.http.HttpServletResponse.*

class DataTransferControllerSpec extends Specification implements ControllerUnitTest<DataTransferController>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataTransfer,
                DataTransferAgreement,
                DataTransferAgreementDocument,
                DataTransferDocument,
        ]
    }

    private static List<MultipartFile> validFiles() {
        return [new MockMultipartFile('files', 'document.pdf', 'application/pdf', 'content'.bytes)]
    }

    void "addFilesToTransferAgreement, when command is valid, then call service and respond with OK"() {
        given:
        DataTransferAgreement dta = new DataTransferAgreement(dataTransferAgreementDocuments: [] as Set)
        AddDataTransferAgreementDocumentsCommand cmd = new AddDataTransferAgreementDocumentsCommand(dataTransferAgreement: dta, files: validFiles())
        cmd.validate()
        controller.dataTransferAgreementService = Mock(DataTransferAgreementService)

        when:
        request.method = 'POST'
        controller.addFilesToTransferAgreement(cmd)

        then:
        1 * controller.dataTransferAgreementService.addFilesToDta(dta, cmd.files) >> dta
        response.status == SC_OK
    }

    void "addFilesToTransferAgreement, when command has errors, then respond with NOT_ACCEPTABLE"() {
        given:
        AddDataTransferAgreementDocumentsCommand cmd = new AddDataTransferAgreementDocumentsCommand(dataTransferAgreement: new DataTransferAgreement(), files: [])
        cmd.validate()
        controller.dataTransferAgreementService = Mock(DataTransferAgreementService)

        when:
        request.method = 'POST'
        controller.addFilesToTransferAgreement(cmd)

        then:
        0 * controller.dataTransferAgreementService._
        response.status == SC_NOT_ACCEPTABLE
    }

    void "addFilesToTransferAgreement, when service throws OtpException, then respond with BAD_REQUEST"() {
        given:
        AddDataTransferAgreementDocumentsCommand cmd = new AddDataTransferAgreementDocumentsCommand(dataTransferAgreement: new DataTransferAgreement(), files: validFiles())
        cmd.validate()
        controller.dataTransferAgreementService = Mock(DataTransferAgreementService) {
            1 * addFilesToDta(_, _) >> { throw new OtpException('failure') }
        }

        when:
        request.method = 'POST'
        controller.addFilesToTransferAgreement(cmd)

        then:
        response.status == SC_BAD_REQUEST
    }

    void "addFilesToTransfer, when command is valid, then call service and respond with OK"() {
        given:
        DataTransfer transfer = new DataTransfer(dataTransferDocuments: [] as Set)
        AddDataTransferDocumentsCommand cmd = new AddDataTransferDocumentsCommand(dataTransfer: transfer, files: validFiles())
        cmd.validate()
        controller.dataTransferService = Mock(DataTransferService)

        when:
        request.method = 'POST'
        controller.addFilesToTransfer(cmd)

        then:
        1 * controller.dataTransferService.addFilesToDataTransfer(transfer, cmd.files) >> transfer
        response.status == SC_OK
    }

    void "addFilesToTransfer, when command has errors, then respond with NOT_ACCEPTABLE"() {
        given:
        AddDataTransferDocumentsCommand cmd = new AddDataTransferDocumentsCommand(dataTransfer: new DataTransfer(), files: [])
        cmd.validate()
        controller.dataTransferService = Mock(DataTransferService)

        when:
        request.method = 'POST'
        controller.addFilesToTransfer(cmd)

        then:
        0 * controller.dataTransferService._
        response.status == SC_NOT_ACCEPTABLE
    }

    void "addFilesToTransfer, when service throws OtpException, then respond with BAD_REQUEST"() {
        given:
        AddDataTransferDocumentsCommand cmd = new AddDataTransferDocumentsCommand(dataTransfer: new DataTransfer(), files: validFiles())
        cmd.validate()
        controller.dataTransferService = Mock(DataTransferService) {
            1 * addFilesToDataTransfer(_, _) >> { throw new OtpException('failure') }
        }

        when:
        request.method = 'POST'
        controller.addFilesToTransfer(cmd)

        then:
        response.status == SC_BAD_REQUEST
    }

    void "updateDataTransferAgreementComment, when command is valid, then call service and respond with OK"() {
        given:
        DataTransferAgreement dta = new DataTransferAgreement()
        UpdateDataTransferAgreementCommentCommand cmd = new UpdateDataTransferAgreementCommentCommand(dataTransferAgreement: dta, value: 'comment')
        cmd.validate()
        controller.dataTransferAgreementService = Mock(DataTransferAgreementService)

        when:
        request.method = 'POST'
        controller.updateDataTransferAgreementComment(cmd)

        then:
        1 * controller.dataTransferAgreementService.updateDataTransferAgreementComment(dta, 'comment')
        response.status == SC_OK
        response.json == [:]
    }

    void "updateDataTransferComment, when command is valid, then call service and respond with OK"() {
        given:
        DataTransfer transfer = new DataTransfer()
        UpdateDataTransferCommentCommand cmd = new UpdateDataTransferCommentCommand(dataTransfer: transfer, value: 'comment')
        cmd.validate()
        controller.dataTransferService = Mock(DataTransferService)

        when:
        request.method = 'POST'
        controller.updateDataTransferComment(cmd)

        then:
        1 * controller.dataTransferService.updateDataTransferComment(transfer, 'comment')
        response.status == SC_OK
        response.json == [:]
    }
}
