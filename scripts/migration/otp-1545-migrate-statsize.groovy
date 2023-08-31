import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.StatSizeFileName
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector
import de.dkfz.tbi.otp.workflowExecution.SelectorType
import de.dkfz.tbi.otp.workflowExecution.Workflow

/*
 * Copyright 2011-2022 The OTP authors
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

List<Workflow> workflows = Workflow.findAllByNameInList([PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WGBS_WORKFLOW])

ReferenceGenome.withTransaction {
    Map defaultFileName = [:]

    ReferenceGenome.all.each { ReferenceGenome referenceGenome ->
        List<StatSizeFileName> statSizeFileNames = StatSizeFileName.findAllByReferenceGenome(referenceGenome)
        String statSizeFileName
        if (statSizeFileNames.empty) {
            return
        } else if (statSizeFileNames.size() > 1) {
            statSizeFileName = statSizeFileNames*.name.find { it.contains("real") }
        } else {
            statSizeFileName = statSizeFileNames.first().name
        }

        String conf = """
                   {
                        "RODDY": {
                            "cvalues": {
                                "CHROM_SIZES_FILE": {
                                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenome.path}/stats/${statSizeFileName}",
                                    "type": "path"
                                }
                            }
                        }
                    }
""".stripIndent()

        ExternalWorkflowConfigFragment fragment = new ExternalWorkflowConfigFragment(
                name: "Default chromosome stat size file for ${referenceGenome.name}",
                configValues: conf,
        ).save(flush: true)

        new ExternalWorkflowConfigSelector(
                name: "Default chromosome stat size file for ${referenceGenome.name}",
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

        defaultFileName[referenceGenome] = statSizeFileName
    }

    ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNullAndStatSizeFileNameIsNotNullAndSampleTypeIsNull().each { ReferenceGenomeProjectSeqType rgpst ->
        if (rgpst.statSizeFileName == defaultFileName[rgpst.referenceGenome]) {
            return // don't create config if default is used
        } else {
            Workflow workflow = workflows.find { rgpst.seqType in it.supportedSeqTypes }
            if (!workflow) {
                return
            }

            String conf = """
                   {
                        "RODDY": {
                            "cvalues": {
                                "CHROM_SIZES_FILE": {
                                    "value": "\${BASE_REFERENCE_GENOME}/${rgpst.referenceGenome.path}/stats/${rgpst.statSizeFileName}",
                                    "type": "path"
                                }
                            }
                        }
                    }
""".stripIndent()

            ExternalWorkflowConfigFragment fragment = new ExternalWorkflowConfigFragment(
                    name: "Chromosome stat size file for ${rgpst.project} ${rgpst.seqType} ${rgpst.referenceGenome.name}",
                    configValues: conf,
            ).save(flush: true)

            new ExternalWorkflowConfigSelector(
                    name: "Chromosome stat size file for ${rgpst.project} ${rgpst.seqType} ${rgpst.referenceGenome.name}",
                    workflows: [workflow] as Set,
                    workflowVersions: [] as Set,
                    projects: [rgpst.project] as Set,
                    seqTypes: [rgpst.seqType] as Set,
                    referenceGenomes: [rgpst.referenceGenome] as Set,
                    libraryPreparationKits: [] as Set,
                    externalWorkflowConfigFragment: fragment,
                    selectorType: SelectorType.GENERIC,
                    priority: 4642,
            ).save(flush: true)
        }
    }

    ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNullAndStatSizeFileNameIsNotNullAndSampleTypeIsNotNull().groupBy { rgpst -> [rgpst.project, rgpst.seqType] }.each {
        ReferenceGenomeProjectSeqType rgpst = it.value.first()

        if (it.value*.referenceGenome.unique().size() > 1) {
            println "Different reference genomes for ${rgpst.project} ${rgpst.seqType}!"
            return
        }

        if (rgpst.statSizeFileName == defaultFileName[rgpst.referenceGenome]) {
            return // don't create config if default is used
        } else {
            Workflow workflow = workflows.find { rgpst.seqType in it.supportedSeqTypes }
            if (!workflow) {
                return
            }

            String conf = """
                   {
                        "RODDY": {
                            "cvalues": {
                                "CHROM_SIZES_FILE": {
                                    "value": "\${BASE_REFERENCE_GENOME}/${rgpst.referenceGenome.path}/stats/${rgpst.statSizeFileName}",
                                    "type": "path"
                                }
                            }
                        }
                    }
""".stripIndent()

            ExternalWorkflowConfigFragment fragment = new ExternalWorkflowConfigFragment(
                    name: "Chromosome stat size file for ${rgpst.project} ${rgpst.seqType} ${rgpst.referenceGenome.name}",
                    configValues: conf,
            ).save(flush: true)

            new ExternalWorkflowConfigSelector(
                    name: "Chromosome stat size file for ${rgpst.project} ${rgpst.seqType} ${rgpst.referenceGenome.name}",
                    workflows: [workflow] as Set,
                    workflowVersions: [] as Set,
                    projects: [rgpst.project] as Set,
                    seqTypes: [rgpst.seqType] as Set,
                    referenceGenomes: [rgpst.referenceGenome] as Set,
                    libraryPreparationKits: [] as Set,
                    externalWorkflowConfigFragment: fragment,
                    selectorType: SelectorType.GENERIC,
                    priority: 4642,
            ).save(flush: true)
        }
    }
}
[]
