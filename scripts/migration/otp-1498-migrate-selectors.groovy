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

import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.workflowExecution.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

// input
Pipeline.Name oldPipelineName = Pipeline.Name.RODDY_RNA_ALIGNMENT
String newWorkflowName = de.dkfz.tbi.otp.workflow.rna.RnaAlignmentWorkflow.WORKFLOW
List<SeqType> seqTypes = [
        SeqTypeService.rnaPairedSeqType,
        SeqTypeService.rnaSingleSeqType,
]

/** Variable to enable or disable a dry run, that will cause no changes except creating default mering criteria **/
Boolean dryRun = true

// script
MergingCriteriaService mergingCriteriaService = ctx.mergingCriteriaService
WorkflowVersionService workflowVersionService = ctx.workflowVersionService

Pipeline pipeline = exactlyOneElement(Pipeline.findAllByName(oldPipelineName))
Workflow workflow = exactlyOneElement(Workflow.findAllByName(newWorkflowName))
List<WorkflowVersion> workflowVersions = WorkflowVersion.findAllByWorkflow(workflow)
List<ReferenceGenome> allUsedReferenceGenomes = []

WorkflowVersionSelector.withTransaction {
    RoddyWorkflowConfig.findAllWhere(
            pipeline: pipeline,
            obsoleteDate: null,
            individual: null,
    ).each { RoddyWorkflowConfig rwc ->

        String version = rwc.programVersion.split(":").last()
        WorkflowVersion workflowVersion = workflowVersions.find { it.workflowVersion == version }
        if (!workflowVersion) {
            println "WARNING: Workflow version '${version}' for '${rwc.project} ${rwc.seqType}' could not be found, skipping."
            return
        }

        new WorkflowVersionSelector(
                project: rwc.project,
                seqType: rwc.seqType,
                workflowVersion: workflowVersion,
        ).save(flush: true)
    }

    ReferenceGenomeProjectSeqType.findAllBySeqTypeInListAndDeprecatedDateIsNull(seqTypes)
            .groupBy {
                [
                        it.project,
                        it.seqType,
                        it.referenceGenome.species,
                        it.referenceGenome.speciesWithStrain,
                ]
            }.each { _, List<ReferenceGenomeProjectSeqType> rgpsts ->

        ReferenceGenomeProjectSeqType rgpst = rgpsts.first()
        if (rgpsts*.referenceGenome.unique().size() > 1) {
            println "WARNING: multiple reference genomes found for '${rgpst.project} ${rgpst.seqType} ${rgpst.referenceGenome.speciesWithStrain.join("+")}', using first one:"
            rgpsts*.referenceGenome.each {
                println "    ${it}"
            }
        }

        Set<Species> species = rgpst.referenceGenome.species
        switch (species.size()) {
            case 0:
                new ReferenceGenomeSelector(
                        project: rgpst.project,
                        seqType: rgpst.seqType,
                        species: new HashSet(rgpst.referenceGenome.speciesWithStrain),
                        workflow: workflow,
                        referenceGenome: rgpst.referenceGenome,
                ).save(flush: true)
                break
            case 1:
                SpeciesWithStrain.findAllBySpecies(species.first()).each {
                    new ReferenceGenomeSelector(
                            project: rgpst.project,
                            seqType: rgpst.seqType,
                            species: new HashSet([it] + rgpst.referenceGenome.speciesWithStrain),
                            workflow: workflow,
                            referenceGenome: rgpst.referenceGenome,
                    ).save(flush: true)
                }
            default:
                assert "Unsupported count: ${rgpst.referenceGenome.species.size()}"
        }
        allUsedReferenceGenomes.add(rgpst.referenceGenome)
    }

    workflow.defaultSeqTypesForWorkflowVersions = seqTypes as Set
    workflow.defaultReferenceGenomesForWorkflowVersions = allUsedReferenceGenomes as Set
    workflow.save(flush: true)

    workflowVersionService.findAllByWorkflow(workflow).collect { wv ->
        wv.supportedSeqTypes = seqTypes as Set
        wv.allowedReferenceGenomes = allUsedReferenceGenomes as Set
        wv.save(flush: true)
    }

    assert !dryRun: "This is a dry run."
}
seqTypes.each {
    mergingCriteriaService.createDefaultMergingCriteria(it)
}
[]
