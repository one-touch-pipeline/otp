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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.rna

import grails.converters.JSON

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.AbstractWorkflowDecider
import de.dkfz.tbi.otp.workflowExecution.decider.RnaAlignmentDecider
import de.dkfz.tbi.otp.workflowTest.alignment.roddy.AbstractRoddyAlignmentWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37Phix
import de.dkfz.tbi.otp.workflowTest.roddy.RnaRoddyFileAssertHelper

import java.nio.file.Path

/**
 * base class for all PanCancer workflow tests
 */
abstract class AbstractRnaAlignmentWorkflowSpec extends AbstractRoddyAlignmentWorkflowSpec implements ReferenceGenomeHg37Phix, IsRoddy {

    RnaAlignmentDecider rnaAlignmentDecider

    RnaRoddyBamFileService rnaRoddyBamFileService

    RnaRoddyFileAssertHelper rnaRoddyFileAssertHelper

    Class<? extends OtpWorkflow> workflowComponentClass = RnaAlignmentWorkflow

    private String getReferenceGenomeConfigHuman() {
        return """
            {
              "RODDY": {
                "cvalues": {
                  "ARRIBA_BLACKLIST": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/indexes/arriba-blacklist/blacklist_hs37d5_gencode19_2017-01-09.tsv.gz"
                  },
                  "ARRIBA_KNOWN_FUSIONS": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/indexes/arriba-fusion/known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz"
                  },
                  "GENE_MODELS": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/gencode/gencode19/gencode.v19.annotation_plain.gtf"
                  },
                  "GENE_MODELS_DEXSEQ": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/gencode/gencode19/gencode.v19.annotation_plain.dexseq.gff"
                  },
                  "GENE_MODELS_EXCLUDE": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/gencode/gencode19/gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf"
                  },
                  "GENE_MODELS_GC": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/gencode/gencode19/gencode.v19.annotation_plain.transcripts.autosomal_transcriptTypeProteinCoding_nonPseudo.1KGRef.gc"
                  },
                  "GENOME_GATK_INDEX": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/indexes/gatk/hs37d5_PhiX.fa"
                  },
                  "GENOME_KALLISTO_INDEX": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/indexes/kallisto/kallisto-0.43.0_1KGRef_Gencode19_k31/kallisto-0.43.0_1KGRef_Gencode19_k31.noGenes.index"
                  },
                  "GENOME_STAR_INDEX": {
                    "type": "path",
                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenomeSpecificPath}/indexes/star_200/STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp"
                  }
                }
              }
            }
        """
    }

    private String getReferenceGenomeConfigMouse() {
        return """
            {
              "RODDY": {
                "cvalues": {
                  "RUN_ARRIBA": {
                    "type": "string",
                    "value": "false"
                  },
                  "RUN_FEATURE_COUNTS_DEXSEQ": {
                    "type": "string",
                    "value": "false"
                  }
                }
              }
            }
        """
    }

    @Override
    protected AbstractWorkflowDecider getDecider() {
        return rnaAlignmentDecider
    }

    @Override
    String getWorkflowName() {
        return RnaAlignmentWorkflow.WORKFLOW
    }

    @Override
    protected boolean isFastQcRequired() {
        return false
    }

    @Override
    Pipeline findOrCreatePipeline() {
        return findOrCreatePipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    void setupWorkflow4Human() {
        workflowAlignment.refresh()
        referenceGenome.refresh()
        createFragmentAndSelector("Human", referenceGenomeConfigHuman, [
                workflows       : [workflowAlignment],
                referenceGenomes: [referenceGenome],
        ])
    }

    void setupWorkflow4Mouse() {
        workflowAlignment.refresh()
        referenceGenome.refresh()
        createFragmentAndSelector("Mouse1", referenceGenomeConfigHuman, [
                workflows       : [workflowAlignment],
                referenceGenomes: [referenceGenome],
        ])
        createFragmentAndSelector("Mouse2", referenceGenomeConfigMouse, [
                workflows       : [workflowAlignment],
                referenceGenomes: [referenceGenome],
        ])
    }

    void "testAlignLanesOnly_OneLane_allFine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            setupWorkflow4Human()
            decide(1, 1)
        }

        when:
        execute(1, 1)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "testAlignLanesOnly_withoutArribaProcessing_OneLane_allFine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            setupWorkflow4Mouse()
            decide(1, 1)
        }

        when:
        execute(1, 1)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    @Override
    protected Path getWorkMergedQAJsonFile(RoddyBamFile bamFile) {
        return rnaRoddyBamFileService.getWorkMergedQAJsonFile(bamFile)
    }

    @Override
    protected void assertWorkflowFileSystemState(RoddyBamFile bamFile) {
        rnaRoddyFileAssertHelper.assertFileSystemState(bamFile, rnaRoddyBamFileService)
    }

    @Override
    protected void assertWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile) {
        rnaRoddyFileAssertHelper.assertWorkDirectoryFileSystemState(bamFile, rnaRoddyBamFileService, roddyConfigService)
    }

    @Override
    protected void checkQC(RoddyBamFile bamFile) {
        CollectionUtils.exactlyOneElement(RnaQualityAssessment.findAllByAbstractBamFile(bamFile))

        JSON.parse(bamFile.finalMergedQAJsonFile.text)
        assert bamFile.finalMergedQAJsonFile.text.trim() != ""

        assert bamFile.coverage == null

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.UNCHECKED
    }
}
