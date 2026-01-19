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

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow

import java.time.LocalDate

class WorkflowVersionSelectorServiceHibernateSpec extends HibernateSpec implements ServiceUnitTest<WorkflowVersionSelectorService>, DomainFactoryCore, WorkflowSystemDomainFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                ProcessingPriority,
                Project,
                SeqType,
                Workflow,
                WorkflowApiVersion,
                WorkflowVersion,
                WorkflowVersionSelector,
                ReferenceGenomeSelector,
        ]
    }

    @Unroll
    void "hasAlignmentConfigForProjectAndSeqType, when project is #projectName and seqType is #seqTypeName and alignmentWorkflow is #alignmentWorkflow, then return #expect"() {
        given:
        Project project = createProject(name: 'Project_A')
        SeqType seqType = createSeqTypePaired(name: 'SeqType_A')
        createProject(name: 'Project_B')
        createSeqTypePaired(name: 'SeqType_B')

        and:
        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector([
                project: CollectionUtils.exactlyOneElement(Project.findAllByName(projectName)),
                seqType: CollectionUtils.exactlyOneElement(SeqType.findAllByName(seqTypeName)),
        ])

        and:
        Map<String, OtpWorkflow> alignmentWorkflows = alignmentWorkflow ? [(workflowVersionSelector.workflowVersion.workflow.beanName): Mock(OtpWorkflow)] : [:]
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupAlignableOtpWorkflowBeans() >> alignmentWorkflows
        }

        expect:
        service.hasAlignmentConfigForProjectAndSeqType(project, seqType) == expect

        where:
        projectName | seqTypeName | alignmentWorkflow || expect
        'Project_A' | 'SeqType_A' | true              || true
        'Project_A' | 'SeqType_A' | false             || false
        'Project_A' | 'SeqType_B' | true              || false
        'Project_A' | 'SeqType_B' | false             || false
        'Project_B' | 'SeqType_A' | true              || false
        'Project_B' | 'SeqType_A' | false             || false
        'Project_B' | 'SeqType_B' | true              || false
        'Project_B' | 'SeqType_B' | false             || false
    }

    void "deprecateSelectorIfUnused, should deprecate workflow version selector, if not used by a reference genome selector"() {
        given:
        createReferenceGenomeSelector()
        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector()

        when:
        service.deprecateSelectorIfUnused(workflowVersionSelector)

        then:
        WorkflowVersionSelector.count == 1
        workflowVersionSelector.deprecationDate != null
    }

    void "deprecateSelectorIfUnused, should not deprecate workflow version selector, if used by a reference genome selector"() {
        given:
        WorkflowVersion workflowVersion = createWorkflowVersion()
        ReferenceGenomeSelector rgSelector = createReferenceGenomeSelector([workflow: workflowVersion.workflow])
        WorkflowVersionSelector wvSelector = createWorkflowVersionSelector([
                project        : rgSelector.project,
                seqType        : rgSelector.seqType,
                workflowVersion: workflowVersion,
        ])

        when:
        service.deprecateSelectorIfUnused(wvSelector)

        then:
        WorkflowVersionSelector.all == [wvSelector]
        wvSelector.deprecationDate == null
    }

    void "createOrUpdate, when project or version is null, should throw assert exception"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: createWorkflow())
        ])

        when:
        service.createOrUpdate(null, seqType, version)

        then:
        AssertionError e1 = thrown(AssertionError)
        e1.message.contains('Parameter project must not be null.')

        when:
        service.createOrUpdate(project, seqType, null)

        then:
        AssertionError e2 = thrown(AssertionError)
        e2.message.contains('Parameter version must not be null.')
    }

    void "createOrUpdate, when version is not null, should create new selector and deprecate the previous one if exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        Workflow workflow = createWorkflow()
        WorkflowVersion version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow)
        ])

        // no selector exists: create a new one
        when:
        WorkflowVersionSelector result = service.createOrUpdate(project, seqType, version)

        then:
        result != null
        result.project == project
        result.seqType == seqType
        result.workflowVersion == version
        result.deprecationDate == null
        WorkflowVersionSelector.count == 1

        // selector exists: deprecate the previous one and create a new one
        when:
        WorkflowVersion newVersion = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow)
        ])
        WorkflowVersionSelector result2 = service.createOrUpdate(project, seqType, newVersion)

        then:
        result2 != null
        result2.project == project
        result2.seqType == seqType
        result2.workflowVersion == newVersion
        result2.deprecationDate == null
        result2.previous == result
        // check that the previous is deprecated
        result.deprecationDate != null
        WorkflowVersionSelector.count == 2

        // selector exists with the same version: return that one
        when:
        WorkflowVersionSelector result3 = service.createOrUpdate(project, seqType, newVersion)

        then:
        result3 == result2
        WorkflowVersionSelector.count == 2
    }

    void "createOrUpdate, should not affect selectors of other projects"() {
        given:
        Project project1 = createProject([name: 'Project1'])
        Project project2 = createProject([name: 'Project2'])
        SeqType seqType = createSeqType()

        Workflow workflow = createWorkflow()
        WorkflowApiVersion apiVersion = createWorkflowApiVersion([workflow: workflow])
        WorkflowVersion oldVersion = createWorkflowVersion([apiVersion: apiVersion])
        WorkflowVersion newVersion = createWorkflowVersion([apiVersion: apiVersion])

        WorkflowVersionSelector project1Selector = createWorkflowVersionSelector([
                project: project1,
                seqType: seqType,
                workflowVersion: oldVersion,
        ])

        WorkflowVersionSelector project2Selector = createWorkflowVersionSelector([
                project: project2,
                seqType: seqType,
                workflowVersion: oldVersion,
        ])

        when:
        // Update project1 to use new version
        WorkflowVersionSelector newSelector = service.createOrUpdate(project1, seqType, newVersion)

        then:
        newSelector != null
        newSelector.project == project1

        // Project1's old selector should be deprecated
        project1Selector.refresh()
        project1Selector.deprecationDate != null

        // Project2's selector should remain active
        project2Selector.refresh()
        project2Selector.deprecationDate == null
    }

    void "deprecateOtherFastqcWorkflowSelectors deprecates other FastQC selectors"() {
        given:
        Project project = createProject()

        Workflow fastqcWorkflow = createWorkflow(name: BashFastQcWorkflow.WORKFLOW)
        Workflow otherFastqcWorkflow = createWorkflow(name: WesFastQcWorkflow.WORKFLOW)
        Workflow nonFastqcWorkflow = createWorkflow(name: "ALIGNMENT")

        // Other FastQC workflow version and selector to be deprecated
        WorkflowVersion otherFastqcVersion = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: otherFastqcWorkflow)
        ])
        WorkflowVersionSelector otherFastqcSelector = createWorkflowVersionSelector(
                project: project,
                workflowVersion: otherFastqcVersion,
        )

        // Current FastQC workflow version and selector shouldn't be deprecated
        WorkflowVersion currentFastqcVersion = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: fastqcWorkflow)
        ])
        WorkflowVersionSelector currentFastqcSelector = createWorkflowVersionSelector(
                project: project,
                workflowVersion: currentFastqcVersion,
        )

        // NonFastqc workflow version and selector to ensure they are not affected
        WorkflowVersion nonFastqcVersion = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: nonFastqcWorkflow)
        ])
        WorkflowVersionSelector nonFastqcSelector = createWorkflowVersionSelector(
                project: project,
                workflowVersion: nonFastqcVersion,
        )

        service.workflowService = new WorkflowService()

        when:
        service.deprecateOtherFastqcWorkflowSelectors(project, fastqcWorkflow)

        then:
        currentFastqcSelector.refresh()
        currentFastqcSelector.deprecationDate == null
        nonFastqcSelector.refresh()
        nonFastqcSelector.deprecationDate == null
        otherFastqcSelector.refresh()
        otherFastqcSelector.deprecationDate != null
    }

    void "updateFastqcVersion returns null when workflow is not FastQC"() {
        given:
        Project project = createProject()
        Workflow nonFastqcWorkflow = createWorkflow(name: "ALIGNMENT")
        WorkflowVersion version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: nonFastqcWorkflow)
        ])

        service.workflowService = Mock(WorkflowService) {
            isFastqc(nonFastqcWorkflow) >> false
        }

        when:
        WorkflowVersionSelector result = service.updateFastqcVersion(project, nonFastqcWorkflow, version)

        then:
        result == null
    }

    void "updateFastqcVersion creates new selector when version is provided"() {
        given:
        Project project = createProject()
        Workflow fastqcWorkflow = createWorkflow(name: BashFastQcWorkflow.WORKFLOW)
        WorkflowApiVersion apiVersion = createWorkflowApiVersion(workflow: fastqcWorkflow)
        WorkflowVersion version = createWorkflowVersion(apiVersion: apiVersion)

        service.workflowService = new WorkflowService()

        and:
        // Create selector for the other FastQC workflow
        Workflow otherFastqc = createWorkflow(name: WesFastQcWorkflow.WORKFLOW)
        WorkflowVersionSelector otherFastqcSelector = createWorkflowVersionSelector(
                project: project,
                workflowVersion: createWorkflowVersion([
                        apiVersion: createWorkflowApiVersion(workflow: otherFastqc)
                ]),
        )

        when:
        WorkflowVersionSelector result = service.updateFastqcVersion(project, fastqcWorkflow, version)

        then:
        result != null
        result.workflowVersion == version
        result.project == project
        otherFastqcSelector.refresh()
        otherFastqcSelector.deprecationDate != null

        WorkflowVersionSelector.findAllByProjectAndDeprecationDateIsNull(project) == [result]

        when:
        WorkflowVersion newVersion = createWorkflowVersion([
                apiVersion: apiVersion
        ])
        WorkflowVersionSelector result2 = service.updateFastqcVersion(project, fastqcWorkflow, newVersion)

        then:
        result2 != null
        result2.workflowVersion == newVersion
        result2.project == project
        result2.previous == result
        WorkflowVersionSelector.findAllByProjectAndDeprecationDateIsNull(project) == [result2]
        TestCase.assertContainSame(
                WorkflowVersionSelector.findAllByProjectAndDeprecationDateIsNotNull(project),
                [result, otherFastqcSelector]
        )
    }

    void "updateFastqcVersion deprecates existing selector when version is null"() {
        given:
        Project project = createProject()
        Workflow workflow = createWorkflow(name: BashFastQcWorkflow.WORKFLOW)
        WorkflowVersion version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow),
                deprecatedDate: null,
        ])
        WorkflowVersionSelector existingSelector = createWorkflowVersionSelector(
                project: project,
                workflowVersion: version,
                deprecationDate: null,
        )

        service.workflowService = new WorkflowService()

        when:
        WorkflowVersionSelector result = service.updateFastqcVersion(project, workflow, null)

        then:
        result == null
        existingSelector.refresh()
        WorkflowVersionSelector.findAllByProjectAndDeprecationDateIsNotNull(project) == [existingSelector]
        existingSelector.deprecationDate != null
    }

    @Unroll
    void "findByProjectSeqTypeWorkflow returns matched selectors and ignore unmatched ones #condition"() {
        given:
        Project project = createProject()
        Workflow workflow = createWorkflow()
        WorkflowVersion version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion([
                        workflow: workflow
                ])
        ])

        and:
        SeqType seqType = setSeqType()

        and:
        WorkflowVersionSelector matchedSelector = createWorkflowVersionSelector([project: project, seqType: seqType, workflowVersion: version])

        and:
        createWorkflowVersionSelector([project: createProject(), seqType: seqType, workflowVersion: version])
        createWorkflowVersionSelector([project: project, seqType: createSeqType(), workflowVersion: version])
        createWorkflowVersionSelector([project: project, seqType: seqType, workflowVersion: createWorkflowVersion([
                apiVersion: createWorkflowApiVersion([
                        workflow: createWorkflow()
                ])
            ]),
        ])

        expect:
        service.findByProjectSeqTypeWorkflow(project, seqType, workflow) == matchedSelector

        where:
        condition           || setSeqType
        "with seqType"      || { createSeqType() }
        "with null seqType" || { null }
    }

    void "findByProjectSeqTypeWorkflow does not return deprecated selector #condition"() {
        given:
        Project project = createProject()
        Workflow workflow = createWorkflow()
        WorkflowVersion version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow)
        ])

        and:
        SeqType seqType = setSeqType()

        and:
        // Create a deprecated selector
        createWorkflowVersionSelector([
                project: project,
                seqType: seqType,
                workflowVersion: version,
                deprecationDate: LocalDate.now(),
        ])

        expect:
        service.findByProjectSeqTypeWorkflow(project, seqType, workflow) == null

        where:
        condition           || setSeqType
        "with seqType"      || { createSeqType() }
        "with null seqType" || { null }
    }

    void "findAllByProjectAndWorkflow returns all non-deprecated selectors for project and workflow"() {
        given:
        Project project1 = createProject([name: "Project1"])
        Project project2 = createProject([name: "Project2"])
        Workflow workflow1 = createWorkflow([name: "Workflow1"])
        Workflow workflow2 = createWorkflow([name: "Workflow2"])
        SeqType seqType1 = createSeqType([name: "SeqType1"])
        SeqType seqType2 = createSeqType([name: "SeqType2"])

        // Create selectors for project1 + workflow1
        WorkflowVersionSelector selector1 = createWorkflowVersionSelector([
                project: project1,
                seqType: seqType1,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)]),
        ])
        WorkflowVersionSelector selector2 = createWorkflowVersionSelector([
                project: project1,
                seqType: seqType2,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)]),
        ])

        // Create selector for project1 + workflow2 (should not be returned)
        createWorkflowVersionSelector([
                project: project1,
                seqType: seqType1,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow2)]),
        ])

        // Create selector for project2 + workflow1 (should not be returned)
        createWorkflowVersionSelector([
                project: project2,
                seqType: seqType1,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)]),
        ])

        // Create deprecated selector for project1 + workflow1 (should not be returned)
        createWorkflowVersionSelector([
                project: project1,
                seqType: seqType1,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)]),
                deprecationDate: LocalDate.now(),
        ])

        when:
        List<WorkflowVersionSelector> result = service.findAllByProjectAndWorkflow(project1, workflow1)

        then:
        result.size() == 2
        result.contains(selector1)
        result.contains(selector2)
    }

    void "findAllByProjectAndWorkflow returns empty list when no matching selectors exist"() {
        given:
        Project project = createProject()
        Workflow workflow = createWorkflow()

        // Create selector for different project
        createWorkflowVersionSelector([
                project: createProject([name: "DifferentProject"]),
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)]),
        ])

        // Create selector for different workflow
        createWorkflowVersionSelector([
                project: project,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: createWorkflow([name: "DifferentWorkflow"]))]),
        ])

        when:
        List<WorkflowVersionSelector> result = service.findAllByProjectAndWorkflow(project, workflow)

        then:
        result.isEmpty()
    }

    void "findAllByProjectAndWorkflow excludes deprecated selectors"() {
        given:
        Project project = createProject()
        Workflow workflow = createWorkflow()

        // Create active selector
        WorkflowVersionSelector activeSelector = createWorkflowVersionSelector([
                project: project,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)]),
        ])

        // Create deprecated selector
        createWorkflowVersionSelector([
                project: project,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)]),
                deprecationDate: LocalDate.now(),
        ])

        when:
        List<WorkflowVersionSelector> result = service.findAllByProjectAndWorkflow(project, workflow)

        then:
        result.size() == 1
        result.contains(activeSelector)
    }

    void "findAllByProjectAndWorkflow includes selectors with null seqType"() {
        given:
        Project project = createProject()
        Workflow workflow = createWorkflow()

        // Create selector with null seqType
        WorkflowVersionSelector selectorWithNullSeqType = createWorkflowVersionSelector([
                project: project,
                seqType: null,
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)]),
        ])

        // Create selector with seqType
        WorkflowVersionSelector selectorWithSeqType = createWorkflowVersionSelector([
                project: project,
                seqType: createSeqType(),
                workflowVersion: createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)]),
        ])

        when:
        List<WorkflowVersionSelector> result = service.findAllByProjectAndWorkflow(project, workflow)

        then:
        result.size() == 2
        result.contains(selectorWithNullSeqType)
        result.contains(selectorWithSeqType)
    }
}
