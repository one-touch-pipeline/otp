package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.*


class AceseqController {
    AnalysisService analysisService
    ProjectService projectService
    ProjectSelectionService projectSelectionService

    Map results() {
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
        List results = analysisService.getCallingInstancesForProject(AceseqInstance, cmd.project.name)
        List data = results.collect { Map properties ->
            def qc = AceseqQc.findByAceseqInstanceAndNumber(AceseqInstance.get(properties.aceseqInstanceId as long), 1)
            properties.putAll([
                    normalContamination: qc.normalContamination,
                    ploidy: qc.ploidy,
                    ploidyFactor: qc.ploidyFactor,
                    goodnessOfFit: qc.goodnessOfFit,
                    gender: qc.gender,
                    solutionPossible: qc.solutionPossible,
            ])
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            properties.dateCreated = sdf.format(properties.dateCreated)
            if (properties.aceseqProcessingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('aceseqInstanceId')
            }
            return properties
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }


    Map plots(AceseqInstanceCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        analysisService.checkFile(cmd.aceseqInstance)

        Map<AceseqInstance.AceseqPlots, Integer> plotNumber = AceseqInstance.AceseqPlots.values().collectEntries() { AceseqInstance.AceseqPlots it ->
            [(it): cmd.aceseqInstance.getPlots(it).size()]
        }

        return [
                aceseqInstance: cmd.aceseqInstance,
                plot: AceseqInstance.AceseqPlot.values(),
                plotNumber: plotNumber,
                error: null,
        ]
    }

    Map plotImage(AceseqPlotCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        analysisService.checkFile(cmd.aceseqInstance)

        File file = cmd.aceseqInstance.getPlot(cmd.aceseqPlot)
        if (file.exists()) {
            render file: file , contentType: "image/png"
        }
        return [
                error: "File not found",
                pid: "no File",
        ]
    }

    Map plotImages(AceseqPlotsCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        analysisService.checkFile(cmd.aceseqInstance)

        List<File> files = cmd.aceseqInstance.getPlots(cmd.aceseqPlot)
        render file: files[cmd.index] , contentType: "image/png"
        return [
                error: "File not found",
                pid: "no File",
        ]
    }
}

class ResultTableCommand extends DataTableCommand {
    Project project
}

class AceseqInstanceCommand {
    AceseqInstance aceseqInstance
}
class AceseqPlotCommand extends AceseqInstanceCommand {
    AceseqInstance.AceseqPlot aceseqPlot
}
class AceseqPlotsCommand extends AceseqInstanceCommand {
    AceseqInstance.AceseqPlots aceseqPlot
    int index
}
