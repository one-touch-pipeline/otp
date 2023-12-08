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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class ExternalWorkflowConfigSelectorServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    @Autowired
    ExternalWorkflowConfigSelectorService service

    void "getNameAndConfigValueOfMoreSpecificSelectors, should return all overwriting and overwritten selectors"() {
        given:
        Workflow workflow1 = createWorkflow()
        Workflow workflow2 = createWorkflow()
        Workflow workflow3 = createWorkflow()
        WorkflowVersion workflowVersion1 = createWorkflowVersion(workflow: workflow1)
        WorkflowVersion workflowVersion2 = createWorkflowVersion(workflow: workflow1)
        WorkflowVersion workflowVersion3 = createWorkflowVersion(workflow: workflow1)
        LibraryPreparationKit lpk1 = createLibraryPreparationKit()
        LibraryPreparationKit lpk2 = createLibraryPreparationKit()
        LibraryPreparationKit lpk3 = createLibraryPreparationKit()
        ReferenceGenome rg1 = createReferenceGenome()
        ReferenceGenome rg2 = createReferenceGenome()
        ReferenceGenome rg3 = createReferenceGenome()
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()
        SeqType seqType3 = createSeqType()
        Project project1 = createProject()
        Project project2 = createProject()
        Project project3 = createProject()

        ExternalWorkflowConfigSelector selectorWithProjectsSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithProjectsSpecified",
                libraryPreparationKits: [],
                workflowVersions: [],
                projects: [project1, project2],
                seqTypes: [],
                workflows: [],
                referenceGenomes: [],
        )

        ExternalWorkflowConfigSelector selectorWithWorkflowVersionsSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithWorkflowVersionsSpecified",
                libraryPreparationKits: [],
                workflowVersions: [workflowVersion1, workflowVersion2],
                projects: [],
                seqTypes: [],
                workflows: [workflow1],
                referenceGenomes: [],
        )

        ExternalWorkflowConfigSelector selectorWithLibraryPreparationKitsSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithLibraryPreparationKitsSpecified",
                libraryPreparationKits: [lpk1, lpk3],
                workflowVersions: [],
                projects: [],
                seqTypes: [],
                workflows: [],
                referenceGenomes: [],
        )

        ExternalWorkflowConfigSelector selectorWithReferenceGenomeSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithReferenceGenomeSpecified",
                libraryPreparationKits: [],
                workflowVersions: [],
                projects: [],
                seqTypes: [],
                workflows: [],
                referenceGenomes: [rg1, rg2],
        )

        ExternalWorkflowConfigSelector selectorWithSeqTypeSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithSeqTypeSpecified",
                libraryPreparationKits: [],
                workflowVersions: [],
                projects: [],
                seqTypes: [seqType1, seqType2],
                workflows: [],
                referenceGenomes: [],
        )

        ExternalWorkflowConfigSelector selectorWithWorkflowSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithWorkflowSpecified",
                libraryPreparationKits: [],
                workflowVersions: [],
                projects: [],
                seqTypes: [],
                workflows: [workflow1, workflow2],
                referenceGenomes: [],
        )

        ExternalWorkflowConfigSelector selectorWithAllSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithAllSpecified",
                projects: [project1, project3],
                workflowVersions: [workflowVersion1, workflowVersion3],
                libraryPreparationKits: [lpk1, lpk3],
                referenceGenomes: [rg1, rg3],
                seqTypes: [seqType1, seqType3],
                workflows: [workflow1],
        )

        ExternalWorkflowConfigSelector selectorWithProjectsAndWorkflowsSpecified = createExternalWorkflowConfigSelector(
                name: "selectorWithProjectsAndWorkflowsSpecified",
                libraryPreparationKits: [],
                workflowVersions: [],
                projects: [project3],
                seqTypes: [],
                workflows: [workflow2, workflow3],
                referenceGenomes: [],
        )

        when:
        OverwritingSubstitutedAndConflictingSelectors resultForNoIdsSet = service.getOverwritingSubstitutedAndConflictingSelectors(
                [], [], [], [], [], [], 4096
        )

        OverwritingSubstitutedAndConflictingSelectors resultForMultipleWorkflowVersionIdsSet = service.getOverwritingSubstitutedAndConflictingSelectors(
                [workflow1.id], [workflowVersion1.id, workflowVersion2.id], [], [], [], [], 4102
        )

        OverwritingSubstitutedAndConflictingSelectors resultForAllIdsSet = service.getOverwritingSubstitutedAndConflictingSelectors(
                [workflow2.id], [], [project2.id], [rg2.id], [seqType2.id], [lpk2.id], 4726
        )

        then:
        TestCase.assertContainSame(resultForNoIdsSet.overwritingSelectors*.selectorName, [
                selectorWithProjectsSpecified,
                selectorWithWorkflowVersionsSpecified,
                selectorWithLibraryPreparationKitsSpecified,
                selectorWithReferenceGenomeSpecified,
                selectorWithSeqTypeSpecified,
                selectorWithWorkflowSpecified,
                selectorWithAllSpecified,
                selectorWithProjectsAndWorkflowsSpecified,
        ]*.name)
        TestCase.assertContainSame(resultForNoIdsSet.substitutedSelectors*.selectorName, [])
        TestCase.assertContainSame(resultForNoIdsSet.conflictingSelectors*.selectorName, [])

        and:
        TestCase.assertContainSame(resultForMultipleWorkflowVersionIdsSet.overwritingSelectors*.selectorName, [
                selectorWithProjectsSpecified,
                selectorWithLibraryPreparationKitsSpecified,
                selectorWithReferenceGenomeSpecified,
                selectorWithSeqTypeSpecified,
                selectorWithAllSpecified,
        ]*.name)
        TestCase.assertContainSame(resultForMultipleWorkflowVersionIdsSet.substitutedSelectors*.selectorName, [
                selectorWithWorkflowSpecified,
        ]*.name)
        TestCase.assertContainSame(resultForMultipleWorkflowVersionIdsSet.conflictingSelectors*.selectorName, [
                selectorWithWorkflowVersionsSpecified,
        ]*.name)

        and:
        TestCase.assertContainSame(resultForAllIdsSet.overwritingSelectors*.selectorName, [])
        TestCase.assertContainSame(resultForAllIdsSet.substitutedSelectors*.selectorName, [
                selectorWithProjectsSpecified,
                selectorWithReferenceGenomeSpecified,
                selectorWithSeqTypeSpecified,
                selectorWithWorkflowSpecified,
        ]*.name)
        TestCase.assertContainSame(resultForAllIdsSet.conflictingSelectors*.selectorName, [])
    }
}
