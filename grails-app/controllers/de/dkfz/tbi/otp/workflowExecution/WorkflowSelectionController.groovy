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
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class WorkflowSelectionController implements CheckAndCall {

    static allowedMethods = [
            index                   : "GET",
            updateVersion           : "POST",
            updateReferenceGenome   : "POST",
            updateMergingCriteriaLPK: "POST",
            updateMergingCriteriaSPG: "POST",
    ]

    private static final Set<String> FASTQC_WORKFLOWS = [
            BashFastQcWorkflow.WORKFLOW,
            WesFastQcWorkflow.WORKFLOW,
    ]

    MergingCriteriaService mergingCriteriaService
    ProjectSelectionService projectSelectionService
    ReferenceGenomeSelectorService referenceGenomeSelectorService
    WorkflowVersionSelectorService workflowVersionSelectorService

    @PreAuthorize('isFullyAuthenticated()')
    def index() {
        Map<String, OtpWorkflow> workflowBeans = applicationContext.getBeansOfType(OtpWorkflow)
        List<String> fastqcWorkflowNames = FASTQC_WORKFLOWS.collect { Workflow.findAllByName(it).beanName }.unique()
        List<Workflow> fastqcWorkflows = Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(fastqcWorkflowNames).sort { it.beanName }
        List<String> alignmentWorkflowNames = workflowBeans.findAll { it.value.isAlignment() }*.key
        List<Workflow> alignmentWorkflows = alignmentWorkflowNames ?
                Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(alignmentWorkflowNames).sort { it.name } : []
        List<String> analysisWorkflowNames = workflowBeans.findAll { it.value.isAnalysis() }*.key
        List<Workflow> analysisWorkflows = analysisWorkflowNames ?
                Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(analysisWorkflowNames).sort { it.name } : []

        Project project = projectSelectionService.selectedProject

        Map<Set<SpeciesWithStrain>, Set<ReferenceGenome>> speciesReferenceGenomeMapping =
                referenceGenomeSelectorService.getMappingOfSpeciesCombinationsToReferenceGenomes(project)

        List<ConfValue> alignmentConf = []
        List<ConfValue> analysisConf = []
        List<FastQcValues> fastqcVersions = []
        fastqcWorkflows.each { Workflow workflow ->
            WorkflowVersion version = atMostOneElement(WorkflowVersionSelector.findAllByProjectAndDeprecationDateIsNull(project).findAll {
                it.workflowVersion.workflow == workflow
            })?.workflowVersion
            List<Version> versions = WorkflowVersion.findAllByWorkflow(workflow).sort { a, b ->
                new WorkflowVersionComparatorConsideringDefaultAndDeprecated(workflow.defaultVersion).compare(a, b)
            }.collect {
                new Version(it.id, it.workflowVersion, it == workflow.defaultVersion, it.deprecatedDate as boolean)
            }
            fastqcVersions.add(new FastQcValues(workflow, versions, version))
        }

        alignmentWorkflows.each { workflow ->
            workflow.supportedSeqTypes.sort { it.displayNameWithLibraryLayout }.each { seqType ->
                WorkflowVersion version = atMostOneElement(
                        WorkflowVersionSelector.findAllByProjectAndSeqTypeAndDeprecationDateIsNull(project, seqType).findAll {
                            it.workflowVersion.workflow == workflow
                        })?.workflowVersion
                List<Version> versions = WorkflowVersion.findAllByWorkflow(workflow).sort { a, b ->
                    new WorkflowVersionComparatorConsideringDefaultAndDeprecated(workflow.defaultVersion).compare(a, b)
                }.collect {
                    new Version(it.id, it.workflowVersion, it == workflow.defaultVersion, it.deprecatedDate as boolean)
                }

                List<RefGenConfValue> refGens = speciesReferenceGenomeMapping.collect { sp, rs ->
                    ReferenceGenome r = atMostOneElement(
                            ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(project, seqType, workflow)
                                    .findAll { it.species == sp }
                    )?.referenceGenome
                    return new RefGenConfValue(sp.sort { it.displayString }, rs.intersect(workflow.allowedReferenceGenomes).sort { it.name }, r)
                }
                alignmentConf.add(new ConfValue(workflow, seqType, versions, version, refGens))
            }
        }

        analysisWorkflows.each { workflow ->
            workflow.supportedSeqTypes.sort { it.displayNameWithLibraryLayout }.each { seqType ->
                WorkflowVersion version = atMostOneElement(
                        WorkflowVersionSelector.findAllByProjectAndSeqTypeAndDeprecationDateIsNull(project, seqType).findAll {
                            it.workflowVersion.workflow == workflow
                        })?.workflowVersion
                List<Version> versions = WorkflowVersion.findAllByWorkflow(workflow).sort { a, b ->
                    new WorkflowVersionComparatorConsideringDefaultAndDeprecated(workflow.defaultVersion).compare(a, b)
                }.collect {
                    new Version(it.id, it.workflowVersion, it == workflow.defaultVersion, it.deprecatedDate as boolean)
                }
                analysisConf.add(new ConfValue(workflow, seqType, versions, version, null))
            }
        }

        List<SeqType> seqTypes = alignmentWorkflows.collectMany { it.supportedSeqTypes }.unique()

        List<MergingCriteria> mergingCriteria = MergingCriteria.findAllByProject(project)
        Map<SeqType, MergingCriteria> seqTypeMergingCriteria = seqTypes.collectEntries { SeqType seqType ->
            [(seqType): mergingCriteria.find { it.seqType == seqType }]
        }.sort { Map.Entry<SeqType, MergingCriteria> it -> it.key.displayNameWithLibraryLayout }

        return [
                alignmentConf         : alignmentConf,
                fastqcVersions        : fastqcVersions,
                analysisConf          : analysisConf,
                seqTypeMergingCriteria: seqTypeMergingCriteria,
        ]
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
    boolean isDefault
    boolean isDeprecated

    String getNameWithDefault() {
        return "${name}${isDefault ? " (default)" : ""}${isDeprecated ? " (deprecated)" : ""}"
    }
}

@Canonical
class FastQcValues {
    Workflow workflow
    List<Version> versions
    WorkflowVersion version
}

@Canonical
class ConfValue {
    Workflow workflow
    SeqType seqType
    List<Version> versions
    WorkflowVersion version
    List<RefGenConfValue> refGens
}

@Canonical
class RefGenConfValue {
    List<SpeciesWithStrain> species
    List<ReferenceGenome> referenceGenomes
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

class UpdateReferenceGenomeCommand implements Validateable {
    SeqType seqType
    List<String> species

    Set<SpeciesWithStrain> getSpeciesWithStrain() {
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
