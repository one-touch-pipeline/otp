/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.analysis.roddy.aceseq

import de.dkfz.tbi.otp.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div8
import de.dkfz.tbi.otp.dataprocessing.aceseq.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.decider.Decider
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.AceseqDecider
import de.dkfz.tbi.otp.workflowTest.analysis.roddy.AbstractRoddyAnalysisWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37

import java.nio.file.Path

abstract class AbstractAceseqWorkflowSpec extends AbstractRoddyAnalysisWorkflowSpec<AceseqInstance> implements ReferenceGenomeHg37, SeqTypeAndInputBamFilesHCC1187Div8 {

    AceseqDecider aceseqDecider
    AceseqLinkFileService aceseqLinkFileService
    AceseqWorkFileService aceseqWorkFileService
    SophiaLinkFileService sophiaLinkFileService

    @Override
    void setupData() {
        super.setupData()
        createSophiaInput()
        adaptReferenceGenome()
    }

    List<Path> filesToCheck(AceseqInstance instance) {
        return aceseqLinkFileService.getAllFiles(instance) +
                aceseqWorkFileService.getAllFiles(instance)
    }

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }

    @Override
    String getWorkflowName() {
        return AceseqWorkflow.WORKFLOW
    }

    @Override
    protected Decider getDecider() {
        return aceseqDecider
    }

    @Override
    void checkQc(AceseqInstance instance) {
        CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumberAndAceseqInstance(1, instance))
    }

    private void createSophiaInput() {
        Path sourceSophiaInputFile = referenceDataDirectory.resolve("aceseq").resolve("svs_stds_filtered_somatic_minEventScore3.tsv")

        SophiaInstance sophiaInstance = SophiaDomainFactory.INSTANCE.createInstance(samplePair,
                [
                        workflowArtefact: createWorkflowArtefact([
                                artefactType: ArtefactType.SOPHIA,
                                state       : WorkflowArtefact.State.SUCCESS,
                        ]),
                ])
        Path sophiaInput = sophiaLinkFileService.getFinalAceseqInputFile(sophiaInstance)
        fileService.createLink(sophiaInput, sourceSophiaInputFile)
    }

    private void adaptReferenceGenome() {
        String basePath = "${referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)}/databases"

        referenceGenome.with {
            gcContentFile = 'hg19_GRch37_100genomes_gc_content_10kb.txt'
            geneticMapFile = "${basePath}/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/genetic_map_chr\${CHR_NAME}_combined_b37.txt"
            geneticMapFileX = "${basePath}/IMPUTE/ALL_1000G_phase1integrated_v3_impute/genetic_map_chrX_nonPAR_combined_b37.txt"
            knownHaplotypesFile = "${basePath}/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr\${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.haplotypes.gz"
            knownHaplotypesFileX = "${basePath}/IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.hap.gz"
            knownHaplotypesLegendFile = "${basePath}/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr\${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.legend.gz"
            knownHaplotypesLegendFileX = "${basePath}/IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.legend.gz"
            mappabilityFile = "${basePath}/UCSC/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz"
            replicationTimeFile = "${basePath}/ENCODE/ReplicationTime_10cellines_mean_10KB.Rda"
        }
        referenceGenome.save(flush: true)
    }

    Class<AceseqWorkflow> workflowComponentClass = AceseqWorkflow

    int expectedExistingWorkflowArtefactCount = 3
}
