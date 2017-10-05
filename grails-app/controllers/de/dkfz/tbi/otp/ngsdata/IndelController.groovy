package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.*

class IndelController {

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

    Map plots(RenderIndelFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (analysisService.checkFile(cmd.indelCallingInstance)) {
            return [
                    id: cmd.indelCallingInstance.id,
                    pid: cmd.indelCallingInstance.individual.pid,
                    error: null
            ]
        }
        return [
                error: "File not found",
                pid: "no File",
        ]
    }

    def renderPDF(RenderIndelFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        File stream = analysisService.checkFile(cmd.indelCallingInstance)
        if (stream) {
            render file: stream , contentType: "application/pdf"
        } else {
            render status: 404
            return
        }
    }

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd hh:mm')
        List results = analysisService.getCallingInstancesForProject(IndelCallingInstance, cmd.project.name)
        List data = results.collect { Map properties ->
            IndelQualityControl qc = IndelQualityControl.findByIndelCallingInstance(IndelCallingInstance.get(properties.instanceId as long))
            IndelSampleSwapDetection sampleSwap = IndelSampleSwapDetection.findByIndelCallingInstance(IndelCallingInstance.get(properties.instanceId as long))
            properties.putAll([
                    numIndels: qc?.numIndels ?: "",
                    numIns: qc?.numIns ?: "",
                    numDels: qc?.numDels ?: "",
                    numSize1_3: qc?.numSize1_3 ?: "",
                    numSize4_10: qc?.numDelsSize4_10 ?: "",
                    somaticSmallVarsInTumor: sampleSwap?.somaticSmallVarsInTumor ?: "",
                    somaticSmallVarsInControl: sampleSwap?.somaticSmallVarsInControl ?: "",
                    somaticSmallVarsInTumorCommonInGnomad: sampleSwap?.somaticSmallVarsInTumorCommonInGnomad ?: "",
                    somaticSmallVarsInControlCommonInGnomad: sampleSwap?.somaticSmallVarsInControlCommonInGnomad ?: "",
                    somaticSmallVarsInTumorPass: sampleSwap?.somaticSmallVarsInTumorPass ?: "",
                    somaticSmallVarsInControlPass: sampleSwap?.somaticSmallVarsInControlPass ?: "",
                    tindaSomaticAfterRescue: sampleSwap?.tindaSomaticAfterRescue ?: "",
                    tindaSomaticAfterRescueMedianAlleleFreqInControl: sampleSwap ? FormatHelper.formatToTwoDecimalsNullSave(sampleSwap.tindaSomaticAfterRescueMedianAlleleFreqInControl) : "",
            ])
            Collection<String> libPrepKitShortNames
            if (SeqTypeNames.fromSeqTypeName(properties.seqTypeName)?.isWgbs()) {
                assert properties.libPrepKit1 == null && properties.libPrepKit2 == null
                libPrepKitShortNames = IndelCallingInstance.get(properties.instanceId).containedSeqTracks*.
                        libraryPreparationKit*.shortDisplayName
            } else {
                libPrepKitShortNames = [(String) properties.libPrepKit1, (String) properties.libPrepKit2]
            }
            properties.libPrepKits = libPrepKitShortNames.unique().collect { it ?: 'unknown' }.join(", <br>")
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
}

class RenderIndelFileCommand {
    IndelCallingInstance indelCallingInstance

    static constraints = {
        indelCallingInstance nullable: false
    }
}
