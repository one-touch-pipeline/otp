/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector
import de.dkfz.tbi.otp.workflowExecution.SelectorType
import de.dkfz.tbi.otp.workflowExecution.Workflow

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

Map<ReferenceGenome, String> conf = [:]
conf[exactlyOneElement(ReferenceGenome.findAllByName('1KGRef_PhiX'))] = """\
{
  "RODDY": {
    "cvalues": {
      "ARRIBA_BLACKLIST": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/indexes/ariba_blacklist/blacklist_hs37d5_gencode19_2017-01-09.tsv.gz"
      },
      "ARRIBA_KNOWN_FUSIONS": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/indexes/ariba_known_fusions/known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz"
      },
      "GENE_MODELS": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/gencode/gencode19/gencode.v19.annotation_plain.gtf"
      },
      "GENE_MODELS_DEXSEQ": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/gencode/gencode19/gencode.v19.annotation_plain.dexseq.gff"
      },
      "GENE_MODELS_EXCLUDE": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/gencode/gencode19/gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf"
      },
      "GENE_MODELS_GC": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/gencode/gencode19/gencode.v19.annotation_plain.transcripts.autosomal_transcriptTypeProteinCoding_nonPseudo.1KGRef.gc"
      },
      "GENOME_GATK_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/indexes/gatk/hs37d5_PhiX.fa"
      },
      "GENOME_KALLISTO_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/indexes/kallisto/kallisto-0.43.0_1KGRef_Gencode19_k31/kallisto-0.43.0_1KGRef_Gencode19_k31.noGenes.index"
      },
      "GENOME_STAR_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/indexes/star_200/STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp"
      }
    }
  }
}
"""

conf[exactlyOneElement(ReferenceGenome.findAllByName('GRCm38mm10_PhiX'))] = """\
{
  "RODDY": {
    "cvalues": {
      "GENE_MODELS": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX/gencode/gencodeM12/gencode.vM12.annotation_plain.w_GeneTranscriptID_MT.gtf"
      },
      "GENE_MODELS_EXCLUDE": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX/gencode/gencodeM12/gencode.vM12.annotation_plain.chrXYMT.rRNA.tRNA.gtf"
      },
      "GENOME_GATK_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX/indexes/gatk/GRCm38mm10_PhiX.fa"
      },
      "GENOME_KALLISTO_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX/indexes/kallisto/kallisto-0.43.0_GRCm38mm10_GencodevM12_k31.index"
      },
      "GENOME_STAR_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX/indexes/star_200/STAR_2.5.2b_GRCm38mm10_gencodevM12_PhiX_200bp"
      },
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

conf[exactlyOneElement(ReferenceGenome.findAllByName('hs37d5_GRCm38mm_PhiX'))] = """\
{
  "RODDY": {
    "cvalues": {
      "ARRIBA_BLACKLIST": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/indexes/ariba_blacklist/blacklist_hs37d5_gencode19_2017-01-09.tsv.gz"
      },
      "ARRIBA_KNOWN_FUSIONS": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/indexes/ariba_known_fusions/known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz"
      },
      "GENE_MODELS": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/gencode/gencode19/gencode.v19.annotation_plain.gtf"
      },
      "GENE_MODELS_DEXSEQ": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/gencode/gencode19/gencode.v19.annotation_plain.dexseq.gff"
      },
      "GENE_MODELS_EXCLUDE": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/gencode/gencode19/gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf"
      },
      "GENOME_GATK_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/indexes/gatk/hs37d5_GRCm38mm10_PhiX.fa"
      },
      "GENOME_KALLISTO_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/indexes/kallisto/kallisto-0.43.0_hs37d5_gencodev19_GRCm38mm_gencodevM12_k31.index"
      },
      "GENOME_STAR_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_hs37d5_GRCm38mm_PhiX/indexes/star_200/STAR_2.5.2b_hs37d5_gencodev19_GRCm38mm_gencodevM12_PhiX_200bp"
      }
    }
  }
}
"""

List<Workflow> workflows = Workflow.findAllByName(RnaAlignmentWorkflow.WORKFLOW)

ReferenceGenome.withTransaction {
    conf.each { referenceGenome, configValues ->
        ExternalWorkflowConfigFragment fragment = new ExternalWorkflowConfigFragment(
                name: "Gene models and reference genome indices for ${referenceGenome.name}",
                configValues: configValues,
        ).save(flush: true)

        new ExternalWorkflowConfigSelector(
                name: "Gene models and reference genome indices for ${referenceGenome.name}",
                workflows: workflows as Set,
                workflowVersions: [] as Set,
                projects: [] as Set,
                seqTypes: [] as Set,
                referenceGenomes: [referenceGenome] as Set,
                libraryPreparationKits: [] as Set,
                externalWorkflowConfigFragment: fragment,
                selectorType: SelectorType.GENERIC,
                priority: 4130,
        ).save(flush: true)
    }
}

[]
