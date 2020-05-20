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
package de.dkfz.tbi.otp.sampleswap

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class SampleSwapController {

    CommentService commentService
    ProjectSelectionService projectSelectionService
    IndividualService individualService
    SampleSwapService sampleSwapService

    def index(SampleSwapIndexCommand cmd) {
        if (cmd.submit == "Cancel") {
            redirect(controller: "projectOverview", action: "index")
        }

        Project project = projectSelectionService.selectedProject

        List<Individual> individuals = Individual.findAllByProject(project, [sort: "pid", order: "asc"])

        Individual individual

        if (cmd.individual && individualService.getIndividual(cmd.individual).project == project) {
            individual = individualService.getIndividual(cmd.individual)
        } else if (!individuals.isEmpty()) {
            individual = individuals.first()
        }

        return [
                individuals    : individuals,
                sampleTypes    : SampleType.list(sort: "name", order: "asc").collect { it.name },
                seqTypes       : SeqType.list(sort: "displayName", order: "asc").collect { it.displayName }.unique(),
                libPrepKits    : [""] + LibraryPreparationKit.list(sort: "name", order: "asc").collect { it.name },
                libraryLayouts : LibraryLayout.findAll()*.toString(),
                antibodyTargets: [""] + AntibodyTarget.list(sort: "name", order: "asc").collect { it.name },
                holdProcessing : cmd.submit == "Submit" ? cmd.holdProcessing : true,
                comment        : cmd.submit == "Submit" ? cmd.comment : "",
        ] + ((cmd.submit == "Submit") ? sampleSwapService.validateInput(
                handleSubmit(params, individual) + [comment: cmd.comment]) : getOriginalData(individual))
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
