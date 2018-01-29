package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.*


class AceseqController {
    AnalysisService analysisService
    ProjectService projectService
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

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        return [
                projects: projects,
                project: project,
        ]
    }

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm')
        List results = analysisService.getCallingInstancesForProject(AceseqInstance, cmd.project?.name)
        List data = results.collect { Map properties ->
            AceseqQc qc = AceseqQc.findByAceseqInstanceAndNumber(AceseqInstance.get(properties.instanceId as long), 1)
            properties.putAll([
                    tcc: FormatHelper.formatToTwoDecimalsNullSave(qc?.tcc),
                    ploidy: FormatHelper.formatToTwoDecimalsNullSave(qc?.ploidy),
                    ploidyFactor: qc?.ploidyFactor,
                    goodnessOfFit: FormatHelper.formatToTwoDecimalsNullSave(qc?.goodnessOfFit),
                    gender: qc?.gender,
                    solutionPossible: qc?.solutionPossible,
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


    Map plots(AceseqInstanceCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        analysisService.checkFile(cmd.aceseqInstance)

        Map<AceseqInstance.AceseqPlots, Integer> plotNumber = AceseqInstance.AceseqPlots.values().collectEntries() { AceseqInstance.AceseqPlots it ->
            int count = cmd.aceseqInstance.getPlots(it).size()
            [(it): count ? (0..count-1) : []]
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

        List<File> files = cmd.aceseqInstance.getPlots(cmd.aceseqPlots)
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
    AceseqInstance.AceseqPlots aceseqPlots
    int index
}
