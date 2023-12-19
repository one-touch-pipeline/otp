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
package de.dkfz.tbi.otp.workflowExecution

import grails.converters.JSON
import grails.databinding.BindUsing
import grails.databinding.SimpleMapDataBindingSource
import grails.validation.Validateable
import groovy.transform.Canonical
import groovy.transform.Immutable
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class WorkflowSelectionController implements CheckAndCall {

    static allowedMethods = [
            index                       : "GET",
            updateVersion               : "POST",
            updateReferenceGenome       : "POST",
            updateMergingCriteriaLPK    : "POST",
            updateMergingCriteriaSPG    : "POST",
            possibleAlignmentOptions    : "POST",
            saveAlignmentConfiguration  : "POST",
            deleteAlignmentConfiguration: "POST",
    ]

    MergingCriteriaService mergingCriteriaService
    ProjectSelectionService projectSelectionService
    ReferenceGenomeSelectorService referenceGenomeSelectorService
    WorkflowVersionSelectorService workflowVersionSelectorService
    WorkflowSelectionService workflowSelectionService
    WorkflowService workflowService
    WorkflowVersionService workflowVersionService
    ReferenceGenomeService referenceGenomeService

    @PreAuthorize('isFullyAuthenticated()')
    def index() {
        List<Workflow> fastqcWorkflows = workflowService.findAllFastqcWorkflows()
        List<Workflow> alignmentWorkflows = workflowService.findAllAlignmentWorkflows()
        List<Workflow> analysisWorkflows = workflowService.findAllAnalysisWorkflows()

        Project project = projectSelectionService.selectedProject

        List<WorkflowVersionConfValue> analysisConf = []
        List<FastQcValues> fastqcVersions = []

        fastqcWorkflows.each { Workflow workflow ->
            WorkflowVersion version = atMostOneElement(WorkflowVersionSelector.findAllByProjectAndDeprecationDateIsNull(project).findAll {
                it.workflowVersion.workflow == workflow
            })?.workflowVersion
            List<Version> versions = workflowVersionService.findAllByWorkflow(workflow).sort { a, b ->
                new WorkflowVersionComparatorConsideringDefaultAndDeprecated(workflow.defaultVersion).compare(a, b)
            }.collect {
                Version.fromWorkflowVersion(it)
            }
            fastqcVersions.add(new FastQcValues(workflow, versions, version))
        }

        List<WorkflowVersionConfValue> alignmentConf = alignmentWorkflows.collectMany { workflow ->
            return workflowVersionSelectorService.findAllByProjectAndWorkflow(project, workflow).collectMany { wvSelector ->
                SeqType seqType = wvSelector.seqType
                List<ReferenceGenomeSelector> rgSelectors = referenceGenomeSelectorService.findAllBySeqTypeAndWorkflowAndProject(seqType, workflow, project)
                return rgSelectors.collect { rgSelector ->
                    RefGenConfValue refGen = new RefGenConfValue(
                            rgSelector.id,
                            rgSelector.species.sort { it.displayName },
                            rgSelector.referenceGenome,
                    )
                    new WorkflowVersionConfValue(wvSelector.id, workflow, seqType, [], wvSelector.workflowVersion, refGen)
                }
            }
        }

        analysisWorkflows.each { workflow ->
            workflowService.getSupportedSeqTypesOfVersions(workflow).sort { it.displayNameWithLibraryLayout }.each { seqType ->
                WorkflowVersionSelector wvSelector = workflowVersionSelectorService.findByProjectAndWorkflowAndSeqType(project, workflow, seqType)
                List<Version> versions = workflowVersionService.findAllByWorkflow(workflow).sort { a, b ->
                    new WorkflowVersionComparatorConsideringDefaultAndDeprecated(workflow.defaultVersion).compare(a, b)
                }.collect { Version.fromWorkflowVersion(it) }
                analysisConf.add(new WorkflowVersionConfValue(wvSelector.id, workflow, seqType, versions, wvSelector.workflowVersion, null))
            }
        }

        List<SeqType> seqTypes = workflowService.getSupportedSeqTypesOfVersions(alignmentWorkflows)

        List<MergingCriteria> mergingCriteria = MergingCriteria.findAllByProject(project)
        Map<SeqType, MergingCriteria> seqTypeMergingCriteria = seqTypes
                .sort { it.displayNameWithLibraryLayout }
                .collectEntries { SeqType seqType -> [(seqType): mergingCriteria.find { it.seqType == seqType }] }
                .findAll { it.value }

        List<WorkflowVersion> alignmentVersions = workflowVersionService.findAllByWorkflows(alignmentWorkflows)

        Set<ReferenceGenome> refGenomes = alignmentVersions.collectMany { it.allowedReferenceGenomes }
        Set<SpeciesWithStrain> species = referenceGenomeService.getAllSpeciesWithStrains(refGenomes)
        return [
                alignmentWorkflows    : alignmentWorkflows,
                alignmentVersions     : alignmentVersions.collect { Version.fromWorkflowVersion(it) },
                seqTypes              : seqTypes,
                species               : species,
                referenceGenomes      : refGenomes,
                alignmentConf         : alignmentConf,
                fastqcVersions        : fastqcVersions,
                analysisConf          : analysisConf,
                seqTypeMergingCriteria: seqTypeMergingCriteria,
        ]
    }

    def possibleAlignmentOptions(AlignmentConfigurationCommand cmd) {
        checkErrorAndCallMethodReturns(cmd) {
            WorkflowSelectionOptionsDTO options = workflowSelectionService.getPossibleAlignmentOptions(new WorkflowSelectionOptionDTO([
                    workflow       : cmd.workflow,
                    workflowVersion: cmd.workflowVersion,
                    seqType        : cmd.seqType,
                    species        : cmd.speciesWithStrains,
                    refGenome      : cmd.referenceGenome,
            ]))

            return render([
                    workflowIds : options.workflows*.id,
                    versionIds  : options.workflowVersions*.id,
                    seqTypeIds  : options.seqTypes*.id,
                    speciesIds  : options.species*.id,
                    refGenomeIds: options.refGenomes*.id,
            ] as JSON)
        }
    }

    def saveAlignmentConfiguration(CreateAlignmentConfigurationCommand cmd) {
        checkErrorAndCallMethodReturns(cmd) {
            Project project = projectSelectionService.requestedProject
            WorkflowSelectionSelectorDTO selectors = workflowSelectionService.createOrUpdate(
                    project, cmd.seqType, cmd.workflowVersion, cmd.speciesWithStrains, cmd.workflow, cmd.referenceGenome
            )
            return render([
                    workflow               : [id: selectors.rgSelector.workflow.id, displayName: selectors.rgSelector.workflow.displayName],
                    seqType                : [id: selectors.wvSelector.seqType.id, displayName: selectors.wvSelector.seqType.displayNameWithLibraryLayout],
                    version                : [id: selectors.wvSelector.workflowVersion.id, displayName: selectors.wvSelector.workflowVersion.workflowVersion],
                    species                : cmd.speciesWithStrains.collect { [id: it.id, displayName: it.displayName] },
                    refGenome              : [id: selectors.rgSelector.referenceGenome.id, displayName: selectors.rgSelector.referenceGenome.name],
                    workflowVersionSelector: [id: selectors.wvSelector.id, previousId: selectors.wvSelector.previous.id],
                    refGenSelectorId       : selectors.rgSelector.id,
            ] as JSON)
        }
    }

    def deleteAlignmentConfiguration(DeleteAlignmentConfigurationCommand cmd) {
        checkErrorAndCallMethodReturns(cmd) {
            workflowSelectionService.deleteAndDeprecateSelectors(cmd.workflowVersionSelector, cmd.referenceGenomeSelector)
            render([] as JSON)
        }
    }

    def updateVersion(UpdateWorkflowVersionCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            workflowVersionSelectorService.createOrUpdate(projectSelectionService.requestedProject, cmd.seqType, cmd.workflowVersion)
            render([success: true] as JSON)
        }
    }

    def updateReferenceGenome(UpdateReferenceGenomeCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            referenceGenomeSelectorService.createOrUpdate(projectSelectionService.requestedProject, cmd.seqType, cmd.speciesWithStrain, cmd.workflow,
                    cmd.referenceGenome)
            render([success: true] as JSON)
        }
    }

    def updateMergingCriteriaLPK(UpdateMergingCriteriaCommandLPK cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            mergingCriteriaService.updateMergingCriteria(cmd.mergingCriteria, cmd.value)
            render([success: true] as JSON)
        }
    }

    def updateMergingCriteriaSPG(UpdateMergingCriteriaCommandSPG cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            mergingCriteriaService.updateMergingCriteria(cmd.mergingCriteria, cmd.value)
            render([success: true] as JSON)
        }
    }
}

@Immutable
class Version {
    long id
    String name
    String workflowName
    boolean isDefault
    boolean isDeprecated

    String getNameWithDefault() {
        return "${name}${this.defaultAndDeprecatedText}"
    }

    String getNameWithDefaultAndWorkflow() {
        return "${workflowName}: ${name}${this.defaultAndDeprecatedText}"
    }

    private getDefaultAndDeprecatedText() {
        return "${isDefault ? " (default)" : ""}${isDeprecated ? " (deprecated)" : ""}"
    }

    static Version fromWorkflowVersion(WorkflowVersion wv) {
        return new Version(wv.id, wv.workflowVersion, wv.workflow.name, wv == wv.workflow.defaultVersion, wv.deprecatedDate as boolean)
    }
}

@Canonical
class FastQcValues {
    Workflow workflow
    List<Version> versions
    WorkflowVersion version
}

@Canonical
class WorkflowVersionConfValue {
    Long workflowVersionSelectorId
    Workflow workflow
    SeqType seqType
    List<Version> versions
    WorkflowVersion version
    RefGenConfValue refGen
}

@Canonical
class RefGenConfValue {
    Long id
    List<SpeciesWithStrain> species
    ReferenceGenome referenceGenome
}

class UpdateWorkflowVersionCommand implements Validateable {
    SeqType seqType
    Workflow workflow
    String value

    WorkflowVersion getWorkflowVersion() {
        return WorkflowVersion.get(value ?: null as Long)
    }

    static constraints = {
        workflowVersion nullable: true
        seqType nullable: true
    }
}

class DeleteAlignmentConfigurationCommand implements Validateable {
    WorkflowVersionSelector workflowVersionSelector
    ReferenceGenomeSelector referenceGenomeSelector
}

class AlignmentConfigurationCommand implements Validateable {
    Workflow workflow
    SeqType seqType
    WorkflowVersion workflowVersion

    @BindUsing({ AlignmentConfigurationCommand obj, SimpleMapDataBindingSource source ->
        Object input = source['speciesWithStrains']
        return input ? SpeciesWithStrain.getAll(input) : null
    })
    List<SpeciesWithStrain> speciesWithStrains = []
    ReferenceGenome referenceGenome

    static constraints = {
        workflow nullable: true
        seqType nullable: true
        workflowVersion nullable: true
        speciesWithStrains nullable: true
        referenceGenome nullable: true
    }
}

class CreateAlignmentConfigurationCommand extends AlignmentConfigurationCommand {
    static constraints = {
        workflow nullable: false
        seqType nullable: false
        workflowVersion nullable: false
        speciesWithStrains nullable: false
        referenceGenome nullable: false
    }
}

class UpdateReferenceGenomeCommand implements Validateable {
    SeqType seqType
    List<String> species

    List<SpeciesWithStrain> getSpeciesWithStrain() {
        return species.collect { SpeciesWithStrain.get(it) } as Set
    }
    Workflow workflow
    String value

    ReferenceGenome getReferenceGenome() {
        return ReferenceGenome.get(value ?: null as Long)
    }

    static constraints = {
        referenceGenome nullable: true
    }
}

class UpdateMergingCriteriaCommandLPK implements Validateable {
    MergingCriteria mergingCriteria
    boolean value
}

class UpdateMergingCriteriaCommandSPG implements Validateable {
    MergingCriteria mergingCriteria
    MergingCriteria.SpecificSeqPlatformGroups value
}

class WorkflowVersionComparatorConsideringDefaultAndDeprecated implements Comparator<WorkflowVersion> {
    WorkflowVersion defaultVersion

    WorkflowVersionComparatorConsideringDefaultAndDeprecated(WorkflowVersion defaultVersion) {
        this.defaultVersion = defaultVersion
    }

    @Override
    int compare(WorkflowVersion a, WorkflowVersion b) {
        if (a == defaultVersion) {
            return -1
        } else if (b == defaultVersion) {
            return 1
        } else if (a.deprecatedDate && !b.deprecatedDate) {
            return 1
        } else if (b.deprecatedDate && !a.deprecatedDate) {
            return -1
        }
        return a <=> b
    }
}
