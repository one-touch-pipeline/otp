/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService

@PreAuthorize('isFullyAuthenticated()')
class AnalysisConfigurationOverviewController {

    static allowedMethods = [
            index: "GET",
    ]

    ConfigPerProjectAndSeqTypeService configPerProjectAndSeqTypeService
    PipelineService pipelineService
    ProjectSelectionService projectSelectionService
    ProjectService projectService

    def index() {
        Project project = projectSelectionService.selectedProject

        Pipeline snv = pipelineService.findByPipelineName(Pipeline.Name.RODDY_SNV)
        Pipeline indel = pipelineService.findByPipelineName(Pipeline.Name.RODDY_INDEL)
        Pipeline sophia = pipelineService.findByPipelineName(Pipeline.Name.RODDY_SOPHIA)
        Pipeline aceseq = pipelineService.findByPipelineName(Pipeline.Name.RODDY_ACESEQ)
        Pipeline runYapsa = pipelineService.findByPipelineName(Pipeline.Name.RUN_YAPSA)

        List snvConfigTable = createAnalysisConfigTable(project, snv)
        List indelConfigTable = createAnalysisConfigTable(project, indel)
        List sophiaConfigTable = createAnalysisConfigTable(project, sophia)
        List aceseqConfigTable = createAnalysisConfigTable(project, aceseq)
        List runYapsaConfigTable = createAnalysisConfigTable(project, runYapsa)

        Map<SeqType, String> checkSophiaReferenceGenome = sophia.seqTypes.collectEntries {
            [(it): projectService.checkReferenceGenomeForSophia(project, it).error]
        }
        Map<SeqType, String> checkAceseqReferenceGenome = aceseq.seqTypes.collectEntries {
            [(it): projectService.checkReferenceGenomeForAceseq(project, it).error]
        }

        return [
                snvSeqTypes                    : snv.seqTypes,
                indelSeqTypes                  : indel.seqTypes,
                sophiaSeqTypes                 : sophia.seqTypes,
                aceseqSeqTypes                 : aceseq.seqTypes,
                runYapsaSeqTypes               : runYapsa.seqTypes,
                snvConfigTable                 : snvConfigTable,
                indelConfigTable               : indelConfigTable,
                sophiaConfigTable              : sophiaConfigTable,
                aceseqConfigTable              : aceseqConfigTable,
                runYapsaConfigTable            : runYapsaConfigTable,
                checkSophiaReferenceGenome     : checkSophiaReferenceGenome,
                checkAceseqReferenceGenome     : checkAceseqReferenceGenome,
        ]
    }

    private List<List<String>> createAnalysisConfigTable(Project project, Pipeline pipeline) {
        List<List<String>> table = []
        table.add(["", "Config created", "Version"])
        pipeline.seqTypes.each { SeqType seqType ->
            List<String> row = []
            row.add(seqType.displayNameWithLibraryLayout)
            SnvConfig snvConfig = configPerProjectAndSeqTypeService.findSnvConfigByProjectAndSeqType(project, seqType)
            RunYapsaConfig runYapsaConfig = configPerProjectAndSeqTypeService.findRunYapsaConfigByProjectAndSeqType(project, seqType)
            RoddyWorkflowConfig roddyWorkflowConfig = configPerProjectAndSeqTypeService.findRoddyWorkflowConfigByProjectAndSeqTypeAndPipeline(project,
                    seqType, pipeline)
            if (pipeline.type == Pipeline.Type.SNV && snvConfig) {
                row.add("Yes")
                row.add(snvConfig.programVersion)
            } else if (pipeline.name == Pipeline.Name.RUN_YAPSA && runYapsaConfig) {
                row.add("Yes")
                row.add(runYapsaConfig.programVersion)
            } else if (pipeline.usesRoddy() && roddyWorkflowConfig) {
                row.add("Yes")
                row.add(roddyWorkflowConfig.programVersion)
            } else {
                row.add("No")
                row.add("-")
            }
            table.add(row)
        }
        return table.transpose()
    }
}
