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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.OtpFileSystemException
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldDefinition
import de.dkfz.tbi.otp.project.additionalField.ProjectPageType
import de.dkfz.tbi.otp.project.exception.unixGroup.UnixGroupIsSharedException
import de.dkfz.tbi.otp.project.projectRequest.ProjectRequestService
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriorityService
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.FileSystemException

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ProjectConfigController implements CheckAndCall {

    CommentService commentService
    ConfigService configService
    ProcessingPriorityService processingPriorityService
    ProjectGroupService projectGroupService
    ProjectRequestService projectRequestService
    ProjectSelectionService projectSelectionService
    ProjectService projectService
    SecurityService securityService
    SpeciesWithStrainService speciesWithStrainService
    TumorEntityService tumorEntityService

    static allowedMethods = [
            index                               : "GET",
            updateProjectField                  : "POST",
            updateAbstractField                 : "POST",
            updateProjectFieldDate              : "POST",
            updateProcessingPriority            : "POST",
            updateSpeciesWithStrains            : "POST",
            updateTumorEntity                   : "POST",
            updateProjectGroup                  : "POST",
            updateSampleIdentifierParserBeanName: "POST",
            updateCopyFiles                     : "POST",
            updatePubliclyAvailable             : "POST",
            updateState                         : "POST",
            updateRequestAvailable              : "POST",
            saveProjectComment                  : "POST",
            updateUnixGroup                     : "POST",
            updateAnalysisDir                   : "POST",
            updateFingerPrinting                : "POST",
            updateProcessingNotification        : "POST",
    ]

    @PreAuthorize("isFullyAuthenticated()")
    Map index() {
        Project project = projectSelectionService.selectedProject
        String projectRequestComment = (securityService.ifAllGranted(Role.ROLE_OPERATOR) ?
                projectRequestService.findProjectRequestByProject(project)?.requesterComment : '')

        Map<String, String> abstractValues = projectRequestService.listAdditionalFieldValues(project)

        List<AbstractFieldDefinition> fieldDefinitions = projectRequestService
                .listAndFetchAbstractFields(project.projectType, ProjectPageType.PROJECT_CONFIG)

        return [
                creationDate                   : TimeFormats.DATE_TIME.getFormattedDate(project.dateCreated),
                lastReceivedDate               : projectService.getLastReceivedDate(project),
                projectRequestComment          : projectRequestComment,
                directory                      : project ? LsdfFilesService.getPath(configService.rootPath.path, project.dirName) : "",
                sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values()*.name(),
                tumorEntities                  : tumorEntityService.list().sort(),
                projectTypes                   : Project.ProjectType.values(),
                states                         : Project.State.values(),
                processingPriority             : project?.processingPriority,
                processingPriorities           : processingPriorityService.allSortedByPriority(),
                allSpeciesWithStrain           : speciesWithStrainService.list().sort { it.toString() } ?: [],
                allProjectGroups               : projectGroupService.list(),
                publiclyAvailable              : project?.publiclyAvailable,
                state                          : project?.state,
                projectRequestAvailable        : project?.projectRequestAvailable,
                abstractFields                 : fieldDefinitions,
                abstractValues                 : abstractValues,
        ]
    }

    def updateProjectField(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(cmd.value, cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateAnalysisDir(UpdateAnalysisDirCommand cmd) {
        Project project = projectSelectionService.requestedProject
        try {
            projectService.updateAnalysisDirectory(project, cmd.analysisDir, cmd.force)
            Map map = [analysisDir: project.dirAnalysis]
            render(map as JSON)
        } catch (FileSystemException | OtpFileSystemException e) {
            return response.sendError(HttpStatus.I_AM_A_TEAPOT.value(), e.message)  // needs a different status for the UI to handle the modal
        } catch (AssertionError | OtpRuntimeException e) {
            return response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), e.message)
        }
    }

    def updateAbstractField(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateAbstractFieldValueForProject(cmd.value, cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateProjectFieldDate(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectFieldDate(cmd.value, cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateProcessingPriority(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(processingPriorityService.findByName(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateSpeciesWithStrains(UpdateProjectSpeciesCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            projectService.updateProjectField(cmd.selectedValuesList, cmd.fieldName, projectSelectionService.requestedProject)
            render([:] as JSON)
        }
    }

    def updateTumorEntity(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(tumorEntityService.findByName(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateProjectGroup(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(projectGroupService.findByName(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateSampleIdentifierParserBeanName(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SampleIdentifierParserBeanName.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateCopyFiles(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updatePubliclyAvailable(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateRequestAvailable(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    @PreAuthorize("isFullyAuthenticated()")
    def saveProjectComment(CommentCommand cmd) {
        Project project = projectService.getProject(cmd.id)
        commentService.saveComment(project, cmd.comment)
        Map dataToRender = [date: TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(project.comment.modificationDate), author: project.comment.author]
        render(dataToRender as JSON)
    }

    def updateUnixGroup(UpdateUnixGroupCommand cmd) {
        try {
            projectService.updateUnixGroup(projectSelectionService.requestedProject, cmd.unixGroup, cmd.force)
            Map map = [unixGroup: cmd.unixGroup]
            render(map as JSON)
        } catch (UnixGroupIsSharedException sharedException) {
            return response.sendError(HttpStatus.CONFLICT.value(), sharedException.message)  // needs a different status for the UI to handle the modal
        } catch (OtpRuntimeException e) {
            return response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), e.message)
        }
    }

    def updateFingerPrinting(String value) {
        projectService.updateFingerPrinting(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render(map as JSON)
    }

    def updateProcessingNotification(String value) {
        projectService.updateProcessingNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render(map as JSON)
    }

    def updateState(String value) {
        projectService.updateState(projectSelectionService.requestedProject, value as Project.State)
        Map map = [success: true]
        render(map as JSON)
    }
}

class UpdateProjectSpeciesCommand implements Validateable {
    String fieldName
    List<String> selectedValuesList

    void setSelectedValues(String value) {
        this.selectedValuesList = JSON.parse(value) as List<String>
    }

    static constraints = {
        fieldName(nullable: true)
    }
}

class UpdateProjectCommand implements Validateable {
    String value
    String fieldName

    static constraints = {
        fieldName(nullable: true)
    }
}

class UpdateUnixGroupCommand implements Validateable {
    String unixGroup
    boolean force = false
}

class UpdateAnalysisDirCommand implements Validateable {
    String analysisDir
    boolean force = false
}
