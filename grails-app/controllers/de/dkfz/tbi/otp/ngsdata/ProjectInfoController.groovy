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
package de.dkfz.tbi.otp.ngsdata

import grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath

import java.text.SimpleDateFormat

class ProjectInfoController {

    ProjectSelectionService projectSelectionService
    ProjectService projectService
    ProjectInfoService projectInfoService

    def list() {
        List<Project> projects = projectService.allProjects
        if (!projects) {
            return [
                    projects: projects,
            ]
        }

        ProjectSelection selection = projectSelectionService.selectedProject

        // we need to reload it to get proper access to all properties
        Project project = Project.get(projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection).id)

        return [
                projects       : projects,
                project        : project,
                addProjectInfos: flash?.addProjectInfos,
                transferModes  : ProjectInfo.TransferMode.values(),
                legalBasis     : ProjectInfo.LegalBasis.values(),
        ]
    }

    @SuppressWarnings('CatchException')
    def addProjectInfo(AddProjectInfoCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.upload.errorMessage") as String, cmd.errors)
            } else {
                try {
                    projectInfoService.createProjectInfoAndUploadFile(cmd)
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.upload.exceptionMessage") as String, e.toString())
                    flash.addProjectInfos = cmd.values()
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect(action: "list")
    }

    @SuppressWarnings('CatchException')
    def addProjectDta(AddProjectDtaCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.upload.errorMessage") as String, cmd.errors)
                flash.addProjectInfos = cmd.values()
            } else {
                try {
                    projectInfoService.createProjectDtaInfoAndUploadFile(cmd)
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.upload.exceptionMessage") as String, e.toString())
                    flash.addProjectInfos = cmd.values()
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect(action: "list")
    }

    @SuppressWarnings('CatchException')
    def markDtaDataAsDeleted(ProjectInfoCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.upload.errorMessage") as String, cmd.errors)
            } else {
                try {
                    projectInfoService.markDtaDataAsDeleted(cmd)
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.upload.exceptionMessage") as String, e.toString())
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect(action: "list")
    }

    @SuppressWarnings('CatchException')
    def deleteProjectInfo(ProjectInfoCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectInfo.upload.errorDeleteFile") as String, cmd.errors)
            } else {
                try {
                    projectInfoService.deleteProjectInfo(cmd)
                } catch (Exception e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectInfo.upload.exceptionMessage") as String, e.toString())
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
        }
        redirect(action: "list")
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
            flash.message = new FlashMessage("No file '${cmd.projectInfo.fileName}' found.")
            redirect(action: "list")
        }
    }
}

class ProjectInfoCommand implements Validateable {
    ProjectInfo projectInfo
}

class AddProjectInfoCommand implements Validateable {
    MultipartFile projectInfoFile

    Project project

    static constraints = {
        project nullable: false
        projectInfoFile(validator: { val, obj ->
            if (val.empty) {
                return "empty"
            }
            if (!OtpPath.isValidPathComponent(val.originalFilename)) {
                return "invalid.name"
            }
            if (ProjectInfo.findAllByProjectAndFileName(obj.project, val.originalFilename).size() != 0) {
                return "duplicate"
            }
            if (val.size > ProjectService.PROJECT_INFO_MAX_SIZE) {
                return "size"
            }
        })
    }
}

class AddProjectDtaCommand extends AddProjectInfoCommand {
    String recipientInstitution
    String recipientPerson
    String recipientAccount

    Date transferDate
    Date validityDate

    ProjectInfo.TransferMode transferMode
    ProjectInfo.LegalBasis legalBasis
    String dtaId

    String requester
    String ticketID
    String comment

    static constraints = {
        recipientInstitution blank: false
        recipientPerson blank: false
        recipientAccount nullable: true

        validityDate nullable: true

        dtaId nullable: true
        requester blank: false
        ticketID nullable: true
        comment nullable: true
    }

    void setTransferDateInput(String transferDate) {
        if (transferDate) {
            this.transferDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(transferDate)
        }
    }

    void setValidityDateInput(String validityDate) {
        if (validityDate) {
            this.validityDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(validityDate)
        }
    }

    Map<String, String> values() {
        return [
                recipientInstitution: recipientInstitution,
                recipientPerson     : recipientPerson,
                recipientAccount    : recipientAccount,
                transferDate        : transferDate,
                validityDate        : validityDate,
                transferMode        : transferMode,
                legalBasis          : legalBasis,
                dtaId               : dtaId,
                requester           : requester,
                ticketID            : ticketID,
                comment             : comment,
        ]
    }
}
