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
import grails.validation.Validateable

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectRequestService
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriorityService

import java.sql.Timestamp
import java.text.SimpleDateFormat

class ProjectConfigController implements CheckAndCall {

    ProjectService projectService
    ProjectRequestService projectRequestService
    ProjectOverviewService projectOverviewService
    ProcessingThresholdsService processingThresholdsService
    CommentService commentService
    ProjectSelectionService projectSelectionService
    SampleTypeService sampleTypeService
    ConfigService configService
    ProcessingPriorityService processingPriorityService

    Map index() {
        Project project = projectSelectionService.selectedProject
        String projectRequestComments = (SpringSecurityUtils.ifAllGranted(Role.ROLE_OPERATOR) ?
                projectRequestService.findProjectRequestByProject(project)?.comments : '')

        Map<String, String> dates = getDates(project)

        return [
                creationDate                   : dates.creationDate,
                lastReceivedDate               : dates.lastReceivedDate,
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
        ]
    }

    JSON updateProjectField(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(cmd.value, cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateProjectFieldDate(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectFieldDate(cmd.value, cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateProcessingPriority(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(ProcessingPriority.get(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateSpeciesWithStrain(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SpeciesWithStrain.get(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateTumorEntity(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(TumorEntity.findByName(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateProjectGroup(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(CollectionUtils.atMostOneElement(ProjectGroup.findAllByName(cmd.value)), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateSampleIdentifierParserBeanName(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SampleIdentifierParserBeanName.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateQcThresholdHandling(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(QcThresholdHandling.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateCopyFiles(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updatePubliclyAvailable(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON updateClosed(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, projectSelectionService.requestedProject)
        }
    }

    JSON saveProjectComment(CommentCommand cmd) {
        Project project = projectService.getProject(cmd.id)
        commentService.saveComment(project, cmd.comment)
        Map dataToRender = [date: project.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: project.comment.author]
        render dataToRender as JSON
    }

    JSON updateFingerPrinting(String value) {
        projectService.updateFingerPrinting(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    JSON updateProcessingNotification(String value) {
        projectService.updateProcessingNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    JSON updateQcTrafficLightNotification(String value) {
        projectService.updateQcTrafficLightNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    JSON updateCustomFinalNotification(String value) {
        projectService.updateCustomFinalNotification(projectSelectionService.requestedProject, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    Map<String, String> getDates(Project project) {
        Timestamp[] timestamps = Sequence.createCriteria().get {
            eq("projectId", project?.id)
            projections {
                min("dateCreated")
                max("dateCreated")
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        return [
                creationDate    : timestamps[0] ? simpleDateFormat.format(timestamps[0]) : null,
                lastReceivedDate: timestamps[0] ? simpleDateFormat.format(timestamps[1]) : null,
        ]
    }
}

class UpdateProjectCommand implements Validateable {
    String value
    String fieldName

    static constraints = {
        fieldName(nullable: true)
    }
}
