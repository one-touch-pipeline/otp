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

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector
import de.dkfz.tbi.otp.workflowExecution.SelectorType
import de.dkfz.tbi.otp.workflowExecution.Workflow

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

Map<ReferenceGenome, String> conf = [:]

conf[exactlyOneElement(ReferenceGenome.findAllByName('bwa06_GRCm38mm10_PhiX_hD3A'))] = """\
{
  "RODDY": {
    "cvalues": {
      "GENE_MODELS": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX_hD3A/gencode/gencodeM12/gencode.vM12.annotation_plain.w_GeneTranscriptID_MT_v2_with_hd3a.gtf"
      },
      "GENE_MODELS_EXCLUDE": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX_hD3A/gencode/gencodeM12/gencode.vM12.annotation_plain.chrXYMT.rRNA.tRNA.gtf"
      },
      "GENOME_GATK_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX_hD3A/indexes/gatk/GRCm38mm10_PhiX_hD3A.fa"
      },
      "GENOME_KALLISTO_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX_hD3A/indexes/kallisto/kallisto-0.43.0_GRCm38mm10_GencodevM12_k31.index"
      },
      "GENOME_STAR_INDEX": {
        "type": "path",
        "value": "\${BASE_REFERENCE_GENOME}/bwa06_GRCm38mm10_PhiX_hD3A/indexes/star_200/STAR_2.5.2b_GRCm38mm10_PhiX_hD3A_gencode-v27_hd3a_125bp"
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
