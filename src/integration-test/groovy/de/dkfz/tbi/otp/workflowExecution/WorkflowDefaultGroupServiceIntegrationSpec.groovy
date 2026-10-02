/*
 * Copyright 2011-2026 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class WorkflowDefaultGroupServiceIntegrationSpec extends Specification implements DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    WorkflowDefaultGroupService workflowDefaultGroupService

    void "applyToProject, should create a WorkflowVersionSelector for an analysis-only entry"() {
        given:
        Project project = createProject()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault entry = createWorkflowVersionSelectorDefault(group: group, referenceGenome: null, species: [] as Set)

        when:
        List<AppliedDefaultEntry> result = workflowDefaultGroupService.applyToProject(project, group)

        then:
        result.size() == 1
        AppliedDefaultEntry applied = result.first()
        applied.referenceGenomeSelector == null
        applied.workflowVersionSelector.project == project
        applied.workflowVersionSelector.seqType == group.seqType
        applied.workflowVersionSelector.workflowVersion == entry.workflowVersion

        WorkflowVersionSelector persisted = exactlyOneElement(WorkflowVersionSelector.findAllByProjectAndSeqType(project, group.seqType))
        persisted == applied.workflowVersionSelector
    }

    void "applyToProject, should also create a ReferenceGenomeSelector for an entry with a referenceGenome"() {
        given:
        Project project = createProject()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        SpeciesWithStrain species = createSpeciesWithStrain()
        ReferenceGenome referenceGenome = createReferenceGenome(species: [] as Set, speciesWithStrain: [species] as Set)
        WorkflowVersionSelectorDefault entry = createWorkflowVersionSelectorDefault(
                group: group, referenceGenome: referenceGenome, species: [species] as Set
        )

        when:
        List<AppliedDefaultEntry> result = workflowDefaultGroupService.applyToProject(project, group)

        then:
        result.size() == 1
        AppliedDefaultEntry applied = result.first()
        applied.workflowVersionSelector.workflowVersion == entry.workflowVersion
        applied.referenceGenomeSelector.referenceGenome == referenceGenome
        applied.referenceGenomeSelector.species == [species] as Set
        applied.referenceGenomeSelector.workflow == entry.workflowVersion.workflow

        ReferenceGenomeSelector persisted = exactlyOneElement(
                ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(project, group.seqType, entry.workflowVersion.workflow)
        )
        persisted == applied.referenceGenomeSelector
    }

    void "applyToProject, should apply every entry of a multi-entry group"() {
        given:
        Project project = createProject()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault entry1 = createWorkflowVersionSelectorDefault(group: group, referenceGenome: null, species: [] as Set)
        WorkflowVersionSelectorDefault entry2 = createWorkflowVersionSelectorDefault(group: group, referenceGenome: null, species: [] as Set)
        WorkflowVersionSelectorDefault entry3 = createWorkflowVersionSelectorDefault(group: group, referenceGenome: null, species: [] as Set)

        when:
        List<AppliedDefaultEntry> result = workflowDefaultGroupService.applyToProject(project, group)

        then:
        result*.workflowVersionSelector*.workflowVersion as Set == [entry1, entry2, entry3]*.workflowVersion as Set
        WorkflowVersionSelector.findAllByProjectAndSeqType(project, group.seqType).size() == 3
    }
}
