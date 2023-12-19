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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

@Transactional
class WorkflowSelectionService {

    WorkflowVersionSelectorService workflowVersionSelectorService
    ReferenceGenomeSelectorService referenceGenomeSelectorService
    WorkflowService workflowService
    WorkflowVersionService workflowVersionService
    ReferenceGenomeService referenceGenomeService

    WorkflowSelectionSelectorDTO createOrUpdate(Project project, SeqType seqType, WorkflowVersion version,
                                                List<SpeciesWithStrain> species, Workflow workflow, ReferenceGenome refGenome) {
        WorkflowVersionSelector wvSelector = workflowVersionSelectorService.createOrUpdate(project, seqType, version)
        ReferenceGenomeSelector rgSelector = referenceGenomeSelectorService.createOrUpdate(project, seqType, species, workflow, refGenome)
        return new WorkflowSelectionSelectorDTO([wvSelector: wvSelector, rgSelector: rgSelector])
    }

    void deleteAndDeprecateSelectors(WorkflowVersionSelector wvSelector, ReferenceGenomeSelector rgSelector) {
        workflowVersionSelectorService.deprecateSelectorIfUnused(wvSelector)
        referenceGenomeSelectorService.deleteSelector(rgSelector)
    }

    WorkflowSelectionOptionsDTO getPossibleAlignmentOptions(WorkflowSelectionOptionDTO option) {
        List<Workflow> workflows = (option.workflow && option.workflowVersion) ? [option.workflowVersion.workflow] :
                workflowService.findAllAlignmentWorkflows()
        List<WorkflowVersion> workflowVersions = option.workflow ? workflowVersionService.findAllByWorkflow(option.workflow) :
                workflowVersionService.findAllByWorkflows(workflows)
        if (option.workflowVersion) {
            workflows = [option.workflowVersion.workflow]
        }

        Set<ReferenceGenome> refGenomes = option.workflowVersion?.allowedReferenceGenomes ?:
                workflowVersions.collectMany { it.allowedReferenceGenomes } as Set
        Set<SeqType> seqTypes = option.workflowVersion?.supportedSeqTypes ?: workflowVersions.collectMany { it.supportedSeqTypes } as Set

        if (option.seqType) {
            workflowVersions = workflowVersions.findAll { it.supportedSeqTypes.contains(option.seqType) }
            workflows = workflows.findAll { workflowVersions*.apiVersion*.workflow.contains(it) }
            refGenomes = refGenomes.findAll { refGenome -> workflowVersions.any { wv -> wv.allowedReferenceGenomes.contains(refGenome) } }
        }

        if (option.refGenome) {
            workflowVersions = workflowVersions.findAll { it.allowedReferenceGenomes.contains(option.refGenome) }
            workflows = workflows.findAll { workflowVersions*.apiVersion*.workflow.contains(it) }
            seqTypes = seqTypes.findAll { seqType -> workflowVersions.any { version -> version.supportedSeqTypes.contains(seqType) } }
        }

        Set<SpeciesWithStrain> species = option.refGenome ?
                referenceGenomeService.getSpeciesWithStrainOptions(option.refGenome, option.species) as Set :
                referenceGenomeService.getAllSpeciesWithStrains(refGenomes) as Set

        if (option.species) {
            refGenomes = refGenomes.findAll {
                List<List<SpeciesWithStrain>> combinationList = referenceGenomeService.getSpeciesWithStrainCombinations(it)
                return combinationList.every { speciesList -> option.species.any { speciesList.contains(it) } } &&
                        option.species.every { specOpt -> combinationList.any { combination -> combination.contains(specOpt) } }
            }
            // Add currently selected reference genome option, if the selected species can be extended to a fitting combination for that reference genome
            if (option.refGenome) {
                List<List<SpeciesWithStrain>> combinationList = referenceGenomeService.getSpeciesWithStrainCombinations(option.refGenome)
                if (option.species.every { it -> combinationList.any { combination -> combination.contains(it) } }) {
                    refGenomes.add(option.refGenome)
                }
            }

            if (!option.refGenome) {
                workflowVersions = workflowVersions.findAll { wv -> refGenomes.any { wv.allowedReferenceGenomes.contains(it) } }
                seqTypes = seqTypes.findAll { seqType -> workflowVersions.any { version -> version.supportedSeqTypes.contains(seqType) } }
                workflows = workflows.findAll { workflowVersions*.apiVersion*.workflow.contains(it) }
            }
            species = species.findAll { option.species.contains(it) || !option.species*.species.contains(it.species) }
        }

        return new WorkflowSelectionOptionsDTO([
                workflows       : workflows,
                workflowVersions: workflowVersions,
                seqTypes        : seqTypes,
                species         : species,
                refGenomes      : refGenomes,
        ])
    }
}

@TupleConstructor
class WorkflowSelectionSelectorDTO {
    WorkflowVersionSelector wvSelector
    ReferenceGenomeSelector rgSelector
}

@TupleConstructor
class WorkflowSelectionOptionsDTO {
    List<Workflow> workflows
    List<WorkflowVersion> workflowVersions
    Set<SeqType> seqTypes
    Set<SpeciesWithStrain> species
    Set<ReferenceGenome> refGenomes
}

@TupleConstructor
class WorkflowSelectionOptionDTO {
    Workflow workflow
    WorkflowVersion workflowVersion
    SeqType seqType
    List<SpeciesWithStrain> species
    ReferenceGenome refGenome
}
