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

import grails.converters.JSON
import grails.validation.Validateable

class StatisticController {

    StatisticService statisticService

    ProjectGroupService projectGroupService

    ProjectService projectService

    JSON projectCountPerDate(ProjectGroupCommand command) {
        ProjectGroup projectGroup = null
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }

        List data = statisticService.projectDateSortAfterDate(projectGroup)
        render statisticService.projectCountPerDate(data) as JSON
    }

    JSON laneCountPerDate(ProjectGroupCommand command) {
        List<Project> projects = null
        if (command.projectGroupName && command.projectGroupName != "OTP") {
            ProjectGroup projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
            projects = projectService.projectByProjectGroup(projectGroup)
        }

        List data = statisticService.laneCountPerDay(projects)
        render statisticService.dataPerDate(data) as JSON
    }

    JSON gigaBasesPerDay(ProjectGroupCommand command) {
        List<Project> projects = null
        if (command.projectGroupName && command.projectGroupName != "OTP") {
            ProjectGroup projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
            projects = projectService.projectByProjectGroup(projectGroup)
        }

        List data = statisticService.gigaBasesPerDay(projects)
        render statisticService.dataPerDate(data) as JSON
    }

    JSON sampleCountPerSequenceType(ProjectGroupCommand command) {
        ProjectGroup projectGroup
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }
        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []

        List sampleCountBySeqType = statisticService.sampleCountPerSequenceType(projectGroup)
        int totalCount = sampleCountBySeqType.collect { it[1] }.sum()

        // cut-off value for naming a seqtype in the graph: 2% of total
        int otherCutOff = (totalCount ?: 0) / 100 * 2
        int otherCount = 0

        sampleCountBySeqType.each {
            if (it[1] < otherCutOff) {
                // seqtype represent tiny fraction of total, group into "other"
                otherCount += it[1]
                return
            }
            labels << it[0]
            values << it[1]
            labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / totalCount)} %"
        }

        if (otherCount > 0) {
            labels << "Other"
            values << otherCount
            labelsPercentage << "Other ${Math.round(otherCount * 100 / totalCount)} %"
        }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size(),
        ]
        render dataToRender as JSON
    }

    JSON patientsCountPerSequenceType(ProjectGroupCommand command) {
        ProjectGroup projectGroup
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }
        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List patientsCountBySeqType = statisticService.patientsCountPerSequenceType(projectGroup)

        int filterCount = ((patientsCountBySeqType.collect { it[1] }.sum()) ?: 0) / 100
        int filtered = 0

        patientsCountBySeqType.each {
            if (it[1] < filterCount) {
                filtered += it[1]
                return
            }
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        if (filtered > 0) {
            labels << "Other"
            values << filtered
            projectSequenceCount += filtered
        }

        patientsCountBySeqType.each {
            if (it[1] < filterCount) {
                return
            }
            labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %"
        }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size(),
        ]
        render dataToRender as JSON
    }

    JSON projectCountPerSequenceType(ProjectGroupCommand command) {
        ProjectGroup projectGroup
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }
        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List projectCountPerSequenceType = statisticService.projectCountPerSequenceType(projectGroup)

        int filterCount = ((projectCountPerSequenceType.collect { it[1] }.sum()) ?: 0) / 100
        int filtered = 0

        projectCountPerSequenceType.each {
            if (it[1] < filterCount) {
                filtered += it[1]
                return
            }
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        if (filtered > 0) {
            labels << "Other"
            values << filtered
            projectSequenceCount += filtered
        }

        projectCountPerSequenceType.each {
            if (it[1] < filterCount) {
                return
            }
            labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %"
        }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size(),
        ]
        render dataToRender as JSON
    }

    JSON sampleTypeCountBySeqType(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List sampleTypeCount = statisticService.sampleTypeCountBySeqType(project)

        sampleTypeCount.each {
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        sampleTypeCount.each { labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %" }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size(),
        ]
        render dataToRender as JSON
    }

    JSON sampleTypeCountByPatient(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<Integer> values = []

        statisticService.sampleTypeCountByPatient(project).each {
            labels << it[0]
            values << it[1]
        }

        Map dataToRender = [
                projectId: project?.id,
                project  : project?.name,
                labels   : labels,
                data     : values,
                count    : values.size(),
        ]
        render dataToRender as JSON
    }

    JSON laneCountPerDateByProject(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)
        List data = statisticService.laneCountPerDay([project])
        render statisticService.dataPerDate(data) as JSON
    }
}

class ProjectCommand implements Validateable {

    ProjectService projectService

    String projectName

    static constraints = {
        projectName(nullable: false, validator: { val, ProjectCommand obj ->
            return val && (obj.projectService.getProjectByName(val) != null)
        })
    }
}

class ProjectGroupCommand implements Validateable {

    ProjectGroupService projectGroupService
    String projectGroupName

    static constraints = {
        projectGroupName(nullable: false, validator: { val, ProjectGroupCommand obj ->
            return val && (val == "OTP" || (obj.projectGroupService.projectGroupByName(val) != null))
        })
    }
}
