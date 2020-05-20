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
package de.dkfz.tbi.otp.administration

import grails.converters.JSON
import grails.validation.Validateable
import grails.validation.ValidationException
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.ProjectService
import de.dkfz.tbi.otp.utils.StringUtils

import java.text.SimpleDateFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class ProjectInfoController implements CheckAndCall {

    static allowedMethods = [
            list                     : 'GET',
            addProjectInfo           : 'POST',
            addTransfer              : 'POST',
            deleteProjectInfo        : 'POST',
            markTransferAsCompleted  : 'POST',
            download                 : 'GET',
            updateProjectInfoComment : 'POST',
            updateDataTransferComment: 'POST',
    ]

    ProjectSelectionService projectSelectionService
    ProjectInfoService projectInfoService

    def list() {
        Project project = projectSelectionService.selectedProject
        project = atMostOneElement(Project.findAllByName(project?.name, [fetch: [projectInfos: 'join']]))

        return [
                project            : project,
                projectInfos       : projectInfoService.getAllProjectInfosSortedByDateDescAndGroupedByDta(project),

                transferModes      : DataTransfer.TransferMode.values(),
                defaultTransferMode: DataTransfer.TransferMode.ASPERA,
                directions         : DataTransfer.Direction.values(),
                defaultDirection   : DataTransfer.Direction.OUTGOING,
                legalBases         : ProjectInfo.LegalBasis.values(),
                defaultLegalBasis  : ProjectInfo.LegalBasis.DTA,

                docCmd             : flash.docCmd as AddProjectInfoCommand,
                xferCmd            : flash.xferCmd as AddTransferCommand,
        ]
    }

    @SuppressWarnings('CatchException')
    def addProjectInfo(AddProjectInfoCommand cmd) {
        Map redirectParams = [:]
        ProjectInfo projectInfo
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.storage") as String, cmd.errors)
                flash.docCmd = cmd
            } else {
                try {
                    flash.message = new FlashMessage(g.message(code: "projectInfo.message.success.storage") as String)
                    projectInfo = projectInfoService.createProjectInfoAndUploadFile(projectSelectionService.requestedProject, cmd)
                    redirectParams["fragment"] = "doc${projectInfo.id}"
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.exception") as String, e.toString())
                    flash.docCmd = cmd
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect([action: "list"] + redirectParams)
    }

    @SuppressWarnings('CatchException')
    def addTransfer(AddTransferCommand cmd) {
        Map redirectParams = [:]
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.transfer") as String, cmd.errors)
                flash.xferCmd = cmd
            } else {
                try {
                    flash.message = new FlashMessage(g.message(code: "projectInfo.message.success.storage") as String)
                    projectInfoService.addTransferToProjectInfo(cmd)
                    redirectParams["fragment"] = "doc${cmd.parentDocument.id}"
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.exception") as String, e.toString())
                    flash.xferCmd = cmd
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect([action: "list"] + redirectParams)
    }

    @SuppressWarnings('CatchException')
    def deleteProjectInfo(ProjectInfoCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.deleteFile") as String, cmd.errors)
            } else {
                try {
                    projectInfoService.deleteProjectInfo(cmd)
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.exception") as String, e.toString())
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect(action: "list")
    }

    @SuppressWarnings('CatchException')
    def markTransferAsCompleted(DataTransferCommand cmd) {
        Map redirectParams = [:]
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.deleteFile") as String, cmd.errors)
            } else {
                try {
                    projectInfoService.markTransferAsCompleted(cmd.dataTransfer)
                    redirectParams["fragment"] = "doc${cmd.dataTransfer.projectInfo.id}"
                } catch (AssertionError | ValidationException e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.message.error.exception") as String, e.toString())
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect([action: "list"] + redirectParams)
    }

    def download(ProjectInfoCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }

        byte[] outputFile = projectInfoService.getProjectInfoContent(cmd.projectInfo)

        if (outputFile) {
            render(file: outputFile, contentType: "application/octet-stream", fileName: cmd.projectInfo.fileName)
        } else {
            flash.message = new FlashMessage("File Not Found", "No file '${cmd.projectInfo.fileName}' found.")
            redirect(action: "list", fragment: "doc${cmd.projectInfo.id}")
        }
    }

    JSON updateProjectInfoComment(UpdateProjectInfoCommentCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectInfoService.updateProjectInfoComment(cmd.projectInfo, cmd.value)
        }
    }

    JSON updateDataTransferComment(UpdateDataTransferCommentCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectInfoService.updateDataTransferComment(cmd.dataTransfer, cmd.value)
        }
    }
}

class ProjectInfoCommand implements Validateable {
    ProjectInfo projectInfo
}

class DataTransferCommand implements Validateable {
    DataTransfer dataTransfer
}

class UpdateProjectInfoCommentCommand extends ProjectInfoCommand {
    String value

    static constraints = {
        value nullable: true
    }

    void setValue(String s) {
        value = StringUtils.blankToNull(s)
    }
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

class AddProjectInfoCommand implements Validateable {
    ProjectSelectionService projectSelectionService
    Project project

    MultipartFile projectInfoFile
    String comment

    // Optional fields, only for DTA documents
    String dtaId
    ProjectInfo.LegalBasis legalBasis
    String peerInstitution
    Date validityDate

    static constraints = {
        project nullable: true
        projectSelectionService nullable: true

        comment nullable: true

        dtaId nullable: true
        legalBasis nullable: true
        peerInstitution nullable: true
        validityDate nullable: true

        projectInfoFile(validator: { val, obj ->
            if (val.empty) {
                return "empty"
            }
            if (!OtpPath.isValidPathComponent(val.originalFilename)) {
                return "invalid.name"
            }
            if (ProjectInfo.findAllByProjectAndFileName(obj.project ?: obj.projectSelectionService.requestedProject, val.originalFilename).size() != 0) {
                return "duplicate"
            }
            if (val.size > ProjectService.PROJECT_INFO_MAX_SIZE) {
                return "size"
            }
        })
    }

    void setComment(String s) {
        comment = StringUtils.blankToNull(s)
    }

    void setDtaId(String s) {
        dtaId = StringUtils.blankToNull(s)
    }

    void setValidityDateInput(String validityDate) {
        if (validityDate) {
            this.validityDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(validityDate)
        }
    }
}

class AddTransferCommand implements Validateable {
    ProjectInfo parentDocument

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
            this.transferDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(s)
        }
    }

    void setCompletionDateInput(String s) {
        if (completionDate) {
            this.completionDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(s)
        }
    }

    static constraints = {
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
