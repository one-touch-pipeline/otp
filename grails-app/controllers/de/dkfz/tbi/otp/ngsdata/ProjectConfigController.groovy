/*
 * Copyright 2011-2020 The OTP authors
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
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import org.springframework.http.HttpStatus

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldDefinition
import de.dkfz.tbi.otp.project.additionalField.ProjectPageType
import de.dkfz.tbi.otp.project.exception.unixGroup.UnixGroupIsSharedException
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriorityService
import de.dkfz.tbi.util.TimeFormats

import java.sql.Timestamp

@Secured("hasRole('ROLE_OPERATOR')")
class ProjectConfigController implements CheckAndCall {

    CommentService commentService
    ConfigService configService
    ProcessingPriorityService processingPriorityService
    ProjectRequestService projectRequestService
    ProjectSelectionService projectSelectionService
    ProjectService projectService

    static allowedMethods = [
            index                               : "GET",
            updateProjectField                  : "POST",
            updateAbstractField                 : "POST",
            updateProjectFieldDate              : "POST",
            updateProcessingPriority            : "POST",
            updateSpeciesWithStrain             : "POST",
            updateTumorEntity                   : "POST",
            updateProjectGroup                  : "POST",
            updateSampleIdentifierParserBeanName: "POST",
            updateQcThresholdHandling           : "POST",
            updateCopyFiles                     : "POST",
            updatePubliclyAvailable             : "POST",
            updateClosed                        : "POST",
            updateRequestAvailable              : "POST",
            saveProjectComment                  : "POST",
            updateUnixGroup                     : "POST",
            updateFingerPrinting                : "POST",
            updateProcessingNotification        : "POST",
            updateQcTrafficLightNotification    : "POST",
            updateCustomFinalNotification       : "POST",
    ]

    @Secured('isFullyAuthenticated()')
    Map index() {
        Project project = projectSelectionService.selectedProject
        String projectRequestComments = (SpringSecurityUtils.ifAllGranted(Role.ROLE_OPERATOR) ?
                projectRequestService.findProjectRequestByProject(project)?.comments : '')

        Map<String, String> abstractValues = projectRequestService.listAdditionalFieldValues(project)

        List<AbstractFieldDefinition> fieldDefinitions = projectRequestService
                .listAndFetchAbstractFields(project.projectType, ProjectPageType.PROJECT_CONFIG)

        return [
                creationDate                   : TimeFormats.DATE_TIME.getFormattedDate(project.dateCreated),
                lastReceivedDate               : getLastReceivedDate(project),
                projectRequestComments         : projectRequestComments,
                directory                      : project ? LsdfFilesService.getPath(configService.rootPath.path, project.dirName) : "",
                sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values()*.name(),
                tumorEntities                  : TumorEntity.list().sort(),
                projectTypes                   : Project.ProjectType.values(),
                processingPriority             : project?.processingPriority,
                processingPriorities           : processingPriorityService.allSortedByPriority(),
                qcThresholdHandlingDropdown    : QcThresholdHandling.values(),
                allSpeciesWithStrain           : SpeciesWithStrain.list().sort { it.toString() } ?: [],
                allProjectGroups               : ProjectGroup.list(),
                publiclyAvailable              : project?.publiclyAvailable,
                closed                         : project?.closed,
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
            projectService.updateProjectField(ProcessingPriority.get(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateSpeciesWithStrain(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SpeciesWithStrain.get(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateTumorEntity(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(TumorEntity.findByName(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateProjectGroup(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(
                    CollectionUtils.atMostOneElement(ProjectGroup.findAllByName(cmd.value)), cmd.fieldName, projectSelectionService.requestedProject
            )
        }
    }

    def updateSampleIdentifierParserBeanName(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SampleIdentifierParserBeanName.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateQcThresholdHandling(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(QcThresholdHandling.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
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

    def updateClosed(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    def updateRequestAvailable(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    @Secured('isFullyAuthenticated()')
    def saveProjectComment(CommentCommand cmd) {
        Project project = projectService.getProject(cmd.id)
        commentService.saveComment(project, cmd.comment)
        Map dataToRender = [date: TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(project.comment.modificationDate), author: project.comment.author]
        render dataToRender as JSON
    }

    def updateUnixGroup(UpdateUnixGroupCommand cmd) {
        try {
            projectService.updateUnixGroup(projectSelectionService.requestedProject, cmd.unixGroup, cmd.force)
            Map map = [unixGroup: cmd.unixGroup]
            render map as JSON
        } catch (UnixGroupIsSharedException sharedException) {
            return response.sendError(HttpStatus.CONFLICT.value(), sharedException.message)  // needs a different status for the UI to handle the modal
        } catch (OtpRuntimeException e) {
            return response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), e.message)
        }
    }

    def updateFingerPrinting(String value) {
        projectService.updateFingerPrinting(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    def updateProcessingNotification(String value) {
        projectService.updateProcessingNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    def updateQcTrafficLightNotification(String value) {
        projectService.updateQcTrafficLightNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    def updateCustomFinalNotification(String value) {
        projectService.updateCustomFinalNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    private String getLastReceivedDate(Project project) {
        Timestamp[] timestamps = SeqTrack.createCriteria().get {
            sample {
                individual {
                    eq("project", project)
                }
            }
            projections {
                max("dateCreated")
            }
        }
        return timestamps ? TimeFormats.DATE_TIME.getFormattedDate(timestamps[0] as Date) : ''
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
