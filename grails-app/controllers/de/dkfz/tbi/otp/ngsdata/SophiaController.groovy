package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import grails.converters.*

import java.text.*

class SophiaController {

    ProjectService projectService
    AnalysisService analysisService
    ProjectSelectionService projectSelectionService

    Map results() {
        String projectName = params.project ?: params.projectName
        if (projectName) {
            Project project
            if ((project =  projectService.getProjectByName(projectName))) {
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: controllerName, action: actionName)
                return
            }
        }

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        return [
                projects: projects,
                project: project,
        ]
    }

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd hh:mm')
        List results = analysisService.getCallingInstancesForProject(SophiaInstance, cmd.project.name)
        List data = results.collect { Map properties ->
            SophiaQc qc = SophiaQc.findBySophiaInstance(SophiaInstance.get(properties.instanceId as long))
            properties.putAll([
                    controlMassiveInvPrefilteringLevel: qc?.controlMassiveInvPrefilteringLevel,
                    tumorMassiveInvFilteringLevel: qc?.tumorMassiveInvFilteringLevel,
                    rnaContaminatedGenesMoreThanTwoIntron: qc?.rnaContaminatedGenesMoreThanTwoIntron,
                    rnaContaminatedGenesCount: qc?.rnaContaminatedGenesCount,
                    rnaDecontaminationApplied: qc?.rnaDecontaminationApplied,
            ])
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            properties.dateCreated = sdf.format(properties.dateCreated)
            if (properties.processingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('instanceId')
            }
            return properties
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }

    Map plots(RenderSophiaFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (analysisService.checkFile(cmd.sophiaInstance)) {
            return [
                    id: cmd.sophiaInstance.id,
                    pid: cmd.sophiaInstance.individual.pid,
                    error: null
            ]
        }
        return [
                error: "File not found",
                pid: "no File",
        ]
    }

    def renderPDF(RenderSophiaFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        File stream = analysisService.checkFile(cmd.sophiaInstance)
        if (stream) {
            render file: stream , contentType: "application/pdf"
        } else {
            render status: 404
            return
        }
    }
}

class RenderSophiaFileCommand {
    SophiaInstance sophiaInstance

    static constraints = {
        sophiaInstance nullable: false
    }
}
