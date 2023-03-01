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

import grails.converters.JSON
import grails.validation.Validateable
import grails.validation.ValidationException
import org.apache.http.entity.ContentType
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.TimeFormats

import java.text.SimpleDateFormat

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class DataTransferController implements CheckAndCall {

    static allowedMethods = [
            index                                   : 'GET',
            addDataTransferAgreement                : 'POST',
            addFilesToTransferAgreement             : 'POST',
            addFilesToTransfer                      : 'POST',
            addTransfer                             : 'POST',
            markTransferAsCompleted                 : 'POST',
            downloadDataTransferAgreementDocument   : 'GET',
            downloadDataTransferDocument            : 'GET',
            updateDataTransferAgreementComment      : 'POST',
            updateDataTransferComment               : 'POST',
            deleteDataTransferAgreement             : 'POST',
    ]

    DataTransferAgreementService dataTransferAgreementService
    ProjectSelectionService projectSelectionService
    DataTransferService dataTransferService

    def index() {
        Project project = projectSelectionService.selectedProject
        List<DataTransferAgreement> dataTransferAgreements = DataTransferAgreement.findAllByProject(project, [sort: "dateCreated", order: "desc"])

        return [
                dataTransferAgreements: dataTransferAgreements,
                transferModes         : DataTransfer.TransferMode.values(),
                selectedTransferMode  : (flash.transferCmd as AddTransferCommand)?.transferMode ?: DataTransfer.TransferMode.ASPERA,
                directions            : DataTransfer.Direction.values(),
                selectedDirection     : (flash.transferCmd as AddTransferCommand)?.direction ?: DataTransfer.Direction.OUTGOING,
                legalBases            : DataTransferAgreement.LegalBasis.values(),
                selectedLegalBasis    : (flash.docDtaCmd as AddDataTransferAgreementCommand)?.legalBasis ?: DataTransferAgreement.LegalBasis.DTA,
                docDtaCmd             : flash.docDtaCmd as AddDataTransferAgreementCommand,
                cachedTransferCmd     : flash.transferCmd as AddTransferCommand,
        ]
    }

    def addDataTransferAgreement(AddDataTransferAgreementCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.storage") as String, cmd.errors)
            } else {
                try {
                    DataTransferAgreement dta = new DataTransferAgreement([
                            project:         cmd.projectSelectionService.requestedProject,
                            comment:         cmd.comment,
                            dtaId:           cmd.dtaId,
                            legalBasis:      cmd.legalBasis,
                            peerInstitution: cmd.peerInstitution,
                            validityDate:    cmd.validityDate,
                    ])

                    dataTransferAgreementService.persistDtaWithDtaDocuments(dta, cmd.files)
                } catch (AssertionError | OtpException e) {
                    flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.exception") as String, e.message)
                    flash.docCmd = cmd
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect([action: "index"])
    }

    def addFilesToTransferAgreement(AddDataTransferAgreementDocumentsCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                DataTransferAgreement dta = dataTransferAgreementService.addFilesToDta(cmd.dataTransferAgreement, cmd.files)
                render(dta.dataTransferAgreementDocuments as JSON)
            } catch (OtpException otpException) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), otpException.message)
            }
        }
    }

    def addFilesToTransfer(AddDataTransferDocumentsCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                DataTransfer transfer = dataTransferService.addFilesToDataTransfer(cmd.dataTransfer, cmd.files)
                render(transfer.dataTransferDocuments as JSON)
            } catch (OtpException otpException) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), otpException.message)
            }
        }
    }

    def addTransfer(AddTransferCommand cmd) {
        Map redirectParams = [:]
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.transfer") as String, cmd.errors)
                flash.transferCmd = cmd
            } else {
                try {
                    flash.message = new FlashMessage(g.message(code: "dataTransfer.message.success.storage") as String)
                    DataTransfer dataTransfer = new DataTransfer([
                            dataTransferAgreement: cmd.dataTransferAgreement,
                            requester: cmd.requester,
                            ticketID: cmd.ticketID,
                            direction: cmd.direction,
                            transferMode: cmd.transferMode,
                            peerPerson: cmd.peerPerson,
                            peerAccount: cmd.peerAccount,
                            transferDate: cmd.transferDate,
                            completionDate: cmd.completionDate,
                            comment: cmd.comment,
                    ])
                    dataTransferService.persistDataTransferWithTransferDocuments(dataTransfer, cmd.files)
                    redirectParams["fragment"] = "doc${cmd.dataTransferAgreement.id}"
                } catch (OtpRuntimeException | OtpException e) {
                    flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.exception") as String, e.message)
                    flash.transferCmd = cmd
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect([action: "index"] + redirectParams)
    }

    def markTransferAsCompleted(DataTransferCommand cmd) {
        Map redirectParams = [:]
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.deleteFile") as String, cmd.errors)
            } else {
                try {
                    dataTransferService.markTransferAsCompleted(cmd.dataTransfer)
                    redirectParams["fragment"] = "doc${cmd.dataTransfer.dataTransferAgreement.id}"
                } catch (AssertionError | ValidationException e) {
                    flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.exception") as String, e.toString())
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect([action: "index"] + redirectParams)
    }

    def downloadDataTransferAgreementDocument(DataTransferAgreementDocumentCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        try {
            byte[] outputFile = dataTransferAgreementService.getDataTransferAgreementDocumentContent(cmd.dataTransferAgreementDocument)
            render(file: outputFile, contentType: ContentType.APPLICATION_OCTET_STREAM, fileName: cmd.dataTransferAgreementDocument.fileName)
        } catch (FileNotFoundException exception) {
            return response.sendError(HttpStatus.NOT_FOUND.value(), exception.message)
        }
    }

    def downloadDataTransferDocument(DataTransferDocumentCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        try {
            byte[] outputFile = dataTransferService.getDataTransferDocumentContent(cmd.dataTransferDocument)
            render(file: outputFile, contentType: ContentType.APPLICATION_OCTET_STREAM, fileName: cmd.dataTransferDocument.fileName)
        } catch (FileNotFoundException exception) {
            return response.sendError(HttpStatus.NOT_FOUND.value(), exception.message)
        }
    }

    def deleteDataTransferAgreement(DataTransferAgreementCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.deleteFile") as String, cmd.errors)
            } else {
                try {
                    dataTransferAgreementService.deleteDataTransferAgreement(cmd.dataTransferAgreement)
                } catch (IOException | AssertionError e) {
                    flash.message = new FlashMessage(g.message(code: "dataTransfer.message.error.exception") as String, e.toString())
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect(action: "index")
    }

    JSON updateDataTransferAgreementComment(UpdateDataTransferAgreementCommentCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            dataTransferAgreementService.updateDataTransferAgreementComment(cmd.dataTransferAgreement, cmd.value)
        }
        return [:] as JSON
    }

    JSON updateDataTransferComment(UpdateDataTransferCommentCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            dataTransferService.updateDataTransferComment(cmd.dataTransfer, cmd.value)
        }
        return [:] as JSON
    }
}

class DataTransferCommand implements Validateable {
    DataTransfer dataTransfer
}

class UpdateDataTransferCommentCommand extends DataTransferCommand {
    String value

    static constraints = {
        value nullable: true
    }

    void setValue(String s) {
        value = StringUtils.blankToNull(s)
    }
}

class AddTransferCommand implements MultipartFilesCommand {
    DataTransferAgreement dataTransferAgreement
    String peerPerson
    String peerAccount
    DataTransfer.TransferMode transferMode
    DataTransfer.Direction direction
    String requester
    String ticketID
    Date completionDate
    Date transferDate
    String comment

    void setTransferDateInput(String s) {
        if (s) {
            this.transferDate = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH).parse(s)
        }
    }

    void setCompletionDateInput(String s) {
        if (s) {
            this.completionDate = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH).parse(s)
        }
    }

    static constraints = {
        dataTransferAgreement nullable: false
        peerPerson blank: false
        peerAccount nullable: true
        requester blank: false
        ticketID blank: false, nullable: true
        completionDate nullable: true
        comment nullable: true
    }

    void setComment(String s) {
        comment = StringUtils.blankToNull(s)
    }

    void setPeerAccount(String s) {
        peerAccount = StringUtils.blankToNull(s)
    }
}

class DataTransferDocumentCommand implements Validateable {
    DataTransferDocument dataTransferDocument
}

class AddDataTransferDocumentsCommand extends DataTransferCommand implements MultipartFilesCommand {
}

class DataTransferAgreementCommand implements Validateable {
    DataTransferAgreement dataTransferAgreement
}

class DataTransferAgreementDocumentCommand implements Validateable {
    DataTransferAgreementDocument dataTransferAgreementDocument
}

class AddDataTransferAgreementDocumentsCommand extends DataTransferAgreementCommand implements MultipartFilesCommand {
}

class UpdateDataTransferAgreementCommentCommand extends DataTransferAgreementCommand {
    String value

    static constraints = {
        value nullable: true
    }

    void setValue(String s) {
        value = StringUtils.blankToNull(s)
    }
}

class AddDataTransferAgreementCommand implements MultipartFilesCommand {
    ProjectSelectionService projectSelectionService
    String comment
    String dtaId
    DataTransferAgreement.LegalBasis legalBasis
    String peerInstitution
    Date validityDate

    static constraints = {
        projectSelectionService nullable: true
        comment nullable: true
        dtaId nullable: true
        legalBasis nullable: true
        peerInstitution nullable: false
        validityDate nullable: true
    }

    void setComment(String s) {
        comment = StringUtils.blankToNull(s)
    }

    void setDtaId(String s) {
        dtaId = StringUtils.blankToNull(s)
    }

    void setValidityDateInput(String validityDate) {
        if (validityDate) {
            this.validityDate = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH).parse(validityDate)
        }
    }
}

trait MultipartFilesCommand implements Validateable {
    List<MultipartFile> files

    static constraints = {
        files validator: { val, obj ->
            val && val.every { file ->
                if (file.empty) {
                    return "empty"
                }
                if (!OtpPathValidator.isValidPathComponent(file.originalFilename)) {
                    return "invalid.name"
                }
                return (file.size > ProjectService.PROJECT_INFO_MAX_SIZE) ? 'size' : true
            }
        }
    }
}
