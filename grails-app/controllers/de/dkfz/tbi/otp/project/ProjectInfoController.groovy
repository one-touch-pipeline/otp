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
package de.dkfz.tbi.otp.project

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Secured("hasRole('ROLE_OPERATOR')")
class ProjectInfoController implements CheckAndCall {

    static allowedMethods = [
            list                                    : 'GET',
            addProjectInfo                          : 'POST',
            deleteProjectInfo                       : 'POST',
            downloadProjectInfoDocument             : 'GET',
            updateProjectInfoComment                : 'POST',
    ]

    ProjectInfoService projectInfoService
    ProjectRequestService projectRequestService
    ProjectSelectionService projectSelectionService

    def list() {
        Project project = projectSelectionService.selectedProject
        project = atMostOneElement(Project.findAllByName(project?.name, [fetch: [projectInfos: 'join']]))
        ProjectRequest projectRequest = projectRequestService.findProjectRequestByProject(project)

        return [
                project            : project,
                projectInfos       : projectInfoService.getAllProjectInfosSortedByDateDesc(project),
                projectRequest     : projectRequest,
                docCmd             : flash.docCmd as AddProjectInfoCommand,
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

    def downloadProjectInfoDocument(ProjectInfoCommand cmd) {
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
}

class ProjectInfoCommand implements Validateable {
    ProjectInfo projectInfo
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

class AddProjectInfoCommand implements Validateable {
    ProjectSelectionService projectSelectionService
    Project project

    MultipartFile projectInfoFile
    String comment

    static constraints = {
        project nullable: true
        projectSelectionService nullable: true

        comment nullable: true

        projectInfoFile(validator: { val, obj ->
            if (val.empty) {
                return "empty"
            }
            if (!OtpPathValidator.isValidPathComponent(val.originalFilename)) {
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
}
