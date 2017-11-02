package de.dkfz.tbi.otp.sampleswap

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import org.springframework.validation.*

class SampleSwapController {

    ProjectService projectService
    SeqTrackService seqTrackService
    CommentService commentService
    ProjectSelectionService projectSelectionService
    IndividualService individualService
    SampleSwapService sampleSwapService

    def index(SampleSwapIndexCommand cmd) {
        if (cmd.submit == "Cancel") {
            redirect(controller: "projectOverview", action: "index")
        }

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects?.first()
        }

        List<Individual> individuals = Individual.findAllByProject(project, [sort: "pid", order: "asc"])

        Individual individual

        if (cmd.individual && individualService.getIndividual(cmd.individual).project == project) {
            individual = individualService.getIndividual(cmd.individual)
        } else if (!individuals.isEmpty()) {
            individual = individuals.first()
        }

        return [
                project        : project,
                projects       : projects,
                individuals    : individuals,
                sampleTypes    : SampleType.list(sort: "name", order: "asc").collect { it.name },
                seqTypes       : SeqType.list(sort: "displayName", order: "asc").collect { it.displayName }.unique(),
                libPrepKits    : [""] + LibraryPreparationKit.list(sort: "name", order: "asc").collect { it.name },
                libraryLayouts : LibraryLayout.findAll()*.toString(),
                antibodyTargets: [""] + AntibodyTarget.list(sort: "name", order: "asc").collect { it.name },
                holdProcessing : cmd.submit != "Submit" ? true : cmd.holdProcessing,
                comment        : cmd.submit != "Submit" ? "" : cmd.comment,
        ] + ((cmd.submit == "Submit") ? sampleSwapService.validateInput(handleSubmit(params, individual) + [comment: cmd.comment]) : getOriginalData(individual))
    }

    private Map handleSubmit(Map params, Individual individual) {
        List data = []
        boolean chipSeq = false
        int numberOfFiles = 0
        int rowNumber = 1
        SeqTrack.createCriteria().list {
            sample {
                eq('individual', individual)
            }
            order('id', 'asc')
        }.each { seqTrack ->
            if (seqTrack.seqType.name == "ChIP Seq" || (params["${seqTrack.id}!seqType"] == "ChIP")) {
                chipSeq = true
            }

            Map dataFiles = [:]
            DataFile.findAllBySeqTrack(seqTrack, [sort: "fileName", order: "asc"]).each {
                dataFiles.put("${it.id}", params["${seqTrack.id}!files!${it.id}"])
            }

            if (dataFiles.size() > numberOfFiles) {
                numberOfFiles = dataFiles.size()
            }

            data << new SampleSwapData(sampleSwapService.getPropertiesForSampleSwap(seqTrack),
                    [
                            project       : params["${seqTrack.id}!project"],
                            pid           : params["${seqTrack.id}!pid"],
                            sampleType    : params["${seqTrack.id}!sampleType"],
                            seqType       : params["${seqTrack.id}!seqType"],
                            libPrepKit    : params["${seqTrack.id}!libPrepKit"],
                            antibodyTarget: params["${seqTrack.id}!antibodyTarget"],
                            antibody      : params["${seqTrack.id}!antibody"],
                            libraryLayout : params["${seqTrack.id}!libraryLayout"],
                            run           : seqTrack.run.name,
                            lane          : seqTrack.laneId,
                            ilse          : seqTrack.ilseId ? Integer.toString(seqTrack.ilseId) : "",
                            files         : dataFiles,
                    ], Long.toString(seqTrack.id), rowNumber++)
        }

        Map output = [
                chipSeq      : chipSeq,
                numberOfFiles: numberOfFiles,
                data         : data,
                individual   : individual,
        ]
        return output
    }

    private Map getOriginalData(Individual individual) {
        List data = []
        boolean chipSeq = false
        int numberOfFiles = 0
        int rowNumber = 1

        SeqTrack.createCriteria().list {
            sample {
                eq('individual', individual)
            }
            order('id', 'asc')
        }.each { seqTrack ->
            if (seqTrack.seqType.name == "ChIP Seq") {
                chipSeq = true
            }
            Map seqTrackData = sampleSwapService.getPropertiesForSampleSwap(seqTrack)
            if (seqTrackData.files.size() > numberOfFiles) {
                numberOfFiles = seqTrackData.files.size()
            }
            data << new SampleSwapData(seqTrackData, seqTrackData, Long.toString(seqTrack.id), rowNumber++)
        }

        Map output = [
                chipSeq      : chipSeq,
                numberOfFiles: numberOfFiles,
                data         : data,
                individual   : individual,
        ]
        return output
    }

}

class SampleSwapIndexCommand {
    String individual
    String submit
    String comment
    boolean holdProcessing
}
