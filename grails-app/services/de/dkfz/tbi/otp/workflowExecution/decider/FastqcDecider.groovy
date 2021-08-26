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
package de.dkfz.tbi.otp.workflowExecution.decider

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.fastqc.FastqcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

@Component
class FastqcDecider implements Decider {

    @Autowired
    ConfigFragmentService configFragmentService
    @Autowired
    FastqcDataFilesService fastqcDataFilesService
    @Autowired
    ProcessingPriorityService processingPriorityService
    @Autowired
    WorkflowArtefactService workflowArtefactService
    @Autowired
    WorkflowRunService workflowRunService

    @Override
    Collection<WorkflowArtefact> decide(Collection<WorkflowArtefact> inputArtefacts, boolean forceRun = false, Map<String, String> userParams = [:]) {
        final Workflow workflow = Workflow.getExactlyOneWorkflow(FastqcWorkflow.WORKFLOW)

        return inputArtefacts.collectMany {
            decideEach(it, workflow)
        }.findAll()
    }

    private List<WorkflowArtefact> decideEach(WorkflowArtefact inputArtefact, Workflow workflow, boolean forceRun = false) {
        if (inputArtefact.artefactType != ArtefactType.FASTQ) {
            return []
        }

        if (!forceRun &&
                WorkflowRunInputArtefact.findAllByWorkflowArtefact(inputArtefact).any { it.workflowRun.workflow == workflow }) {
            return []
        }

        Optional<Artefact> optionalArtefact = inputArtefact.artefact
        if (!optionalArtefact.isPresent()) {
            return []
        }

        SeqTrack seqTrack = optionalArtefact.get() as SeqTrack

        List<String> runDisplayName = generateWorkflowRunDisplayName(seqTrack)
        List<String> artefactDisplayName = runDisplayName
        artefactDisplayName.remove(0)
        String shortName = "${FastqcWorkflow.WORKFLOW}: ${seqTrack.individual.pid} " +
                "${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflow,
                inputArtefact.producedBy.priority,
                fastqcDataFilesService.fastqcOutputDirectory(seqTrack),
                seqTrack.individual.project,
                runDisplayName,
                shortName,
                getConfigFragments(seqTrack, workflow),
        )

        new WorkflowRunInputArtefact(
                workflowRun: run,
                role: FastqcWorkflow.INPUT_FASTQ,
                workflowArtefact: inputArtefact,
        ).save(flush: true)

        List<WorkflowArtefact> result = []
        seqTrack.seqType.libraryLayout.mateCount.times { int i ->
            result.add(workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                    run,
                    "${FastqcWorkflow.OUTPUT_FASTQC}_${i + 1}",
                    ArtefactType.FASTQC,
                    artefactDisplayName,
            )).save(flush: true))
        }
        return result
    }

    List<ExternalWorkflowConfigFragment> getConfigFragments(SeqTrack seqTrack, Workflow workflow) {
        return configFragmentService.getSortedFragments(new SingleSelectSelectorExtendedCriteria(
                workflow,
                null, //workflowVersion, FastQC is not versioned
                seqTrack.project,
                seqTrack.seqType,
                null, //referenceGenome, not used for FastQC
                seqTrack.libraryPreparationKit,
        ))
    }

    /**
     * Generate display name for the workflowRun.
     *
     * @param seqTrack of the workflowRun
     * @return display name as list of strings
     */
    private List<String> generateWorkflowRunDisplayName(SeqTrack seqTrack) {
        List<String> runDisplayName = []
        runDisplayName.add("project: ${seqTrack.project.name}")
        runDisplayName.add("individual: ${seqTrack.individual.displayName}")
        runDisplayName.add("sampleType: ${seqTrack.sampleType.displayName}")
        runDisplayName.add("seqType: ${seqTrack.seqType.displayNameWithLibraryLayout}")
        runDisplayName.add("run: ${seqTrack.run.name}")
        runDisplayName.add("lane: ${seqTrack.laneId}")

        return runDisplayName
    }
}
