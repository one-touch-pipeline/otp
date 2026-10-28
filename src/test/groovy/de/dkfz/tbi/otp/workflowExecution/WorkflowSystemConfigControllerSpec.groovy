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

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesCommonName
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

import java.time.LocalDate
import java.time.ZoneId

import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE
import static javax.servlet.http.HttpServletResponse.SC_OK

class WorkflowSystemConfigControllerSpec extends Specification implements ControllerUnitTest<WorkflowSystemConfigController>, DataTest,
        DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Comment,
                ProcessingPriority,
                Project,
                ReferenceGenome,
                SeqType,
                Species,
                SpeciesCommonName,
                SpeciesWithStrain,
                Workflow,
                WorkflowApiVersion,
                WorkflowDefaultGroup,
                WorkflowVersion,
                WorkflowVersionSelectorDefault,
        ]
    }

    void setupData() {
        controller.workflowDefaultGroupService = Mock(WorkflowDefaultGroupService)
    }

    void "getWorkflowVersions, should include isDefault and displayName for each version"() {
        given:
        setupData()
        Workflow workflow = createWorkflow()
        WorkflowVersion defaultVersion = createWorkflowVersion(
                apiVersion: createWorkflowApiVersion(workflow: workflow), allowedReferenceGenomes: [] as Set, supportedSeqTypes: [] as Set
        )
        WorkflowVersion otherVersion = createWorkflowVersion(
                apiVersion: createWorkflowApiVersion(workflow: workflow), allowedReferenceGenomes: [] as Set, supportedSeqTypes: [] as Set
        )
        workflow.defaultVersion = defaultVersion
        workflow.save(flush: true)
        controller.workflowVersionService = new WorkflowVersionService()

        when:
        controller.getWorkflowVersions(workflow.id)

        then:
        Map defaultVersionJson = response.json.find { it.id == defaultVersion.id }
        defaultVersionJson.isDefault
        defaultVersionJson.displayName.contains("(default)")

        Map otherVersionJson = response.json.find { it.id == otherVersion.id }
        !otherVersionJson.isDefault
        !otherVersionJson.displayName.contains("(default)")
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "getWorkflowDefaultGroups, should render the flattened rows from the service"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault entry = createWorkflowVersionSelectorDefault(group: group)

        when:
        controller.getWorkflowDefaultGroups()

        then:
        1 * controller.workflowDefaultGroupService.list() >> [group]
        1 * controller.workflowDefaultGroupService.findAllEntries() >> [entry]
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.size() == 1
        response.json[0].groupId == group.id
        response.json[0].name == group.name
        response.json[0].workflow.id == entry.workflowVersion.workflow.id
        response.json[0].workflowVersion.id == entry.workflowVersion.id
    }

    void "saveWorkflowDefaultGroup, when group is absent, should create a new group with all given workflow versions"() {
        given:
        setupData()
        SeqType seqType = createSeqType()
        WorkflowVersion version1 = createWorkflowVersion()
        WorkflowVersion version2 = createWorkflowVersion()
        WorkflowDefaultGroup createdGroup = createWorkflowDefaultGroup(name: "someName", seqType: seqType)

        when:
        request.method = 'POST'
        params['name'] = "someName"
        params['seqType.id'] = seqType.id
        params['workflowVersion'] = [version1.id.toString(), version2.id.toString()]
        controller.saveWorkflowDefaultGroup()

        then:
        1 * controller.workflowDefaultGroupService.saveGroup(null, "someName", seqType, [version1, version2] as Set) >> createdGroup
        1 * controller.workflowDefaultGroupService.findEntries(createdGroup) >> []
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.name == "someName"
        response.json.seqType.id == seqType.id
    }

    void "saveWorkflowDefaultGroup, when group is given, should update the existing group"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        SeqType newSeqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion()

        when:
        request.method = 'POST'
        params['group.id'] = group.id
        params['name'] = "newName"
        params['seqType.id'] = newSeqType.id
        params['workflowVersion'] = [version.id.toString()]
        controller.saveWorkflowDefaultGroup()

        then:
        1 * controller.workflowDefaultGroupService.saveGroup(group, "newName", newSeqType, [version] as Set) >> group
        1 * controller.workflowDefaultGroupService.findEntries(group) >> []
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.id == group.id
    }

    void "saveWorkflowDefaultGroup, when required fields are missing, should respond 406"() {
        given:
        setupData()

        when:
        request.method = 'POST'
        controller.saveWorkflowDefaultGroup()

        then:
        0 * controller.workflowDefaultGroupService._
        response.status == 406
    }

    void "saveWorkflowDefaultGroup, when a workflowVersion id is unknown, should respond 406"() {
        given:
        setupData()
        SeqType seqType = createSeqType()

        when:
        request.method = 'POST'
        params['name'] = "someName"
        params['seqType.id'] = seqType.id
        params['workflowVersion'] = ["-1"]
        controller.saveWorkflowDefaultGroup()

        then:
        0 * controller.workflowDefaultGroupService._
        response.status == 406
    }

    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "deleteWorkflowDefaultGroup, should call the service and render an empty response"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()

        when:
        request.method = 'POST'
        params['group.id'] = group.id
        controller.deleteWorkflowDefaultGroup()

        then:
        1 * controller.workflowDefaultGroupService.delete(group)
        0 * controller.workflowDefaultGroupService._
        response.status == 200
    }

    void "index, returns the reference genomes with the legacy ones last and the seq types sorted by name, both ignoring the case"() {
        given:
        ReferenceGenome legacy = buildReferenceGenome('aLegacy', true)
        ReferenceGenome upperCase = buildReferenceGenome('B')
        ReferenceGenome lowerCase = buildReferenceGenome('a')
        SeqType seqTypeUpperCase = buildSeqType('B')
        SeqType seqTypeLowerCase = buildSeqType('a')
        SeqType seqTypeC = buildSeqType('c')

        controller.referenceGenomeService = Mock(ReferenceGenomeService) {
            1 * list() >> [legacy, upperCase, lowerCase]
        }
        controller.seqTypeService = Mock(SeqTypeService) {
            1 * list() >> [seqTypeC, seqTypeUpperCase, seqTypeLowerCase]
        }
        controller.workflowService = Mock(WorkflowService) {
            1 * findAllAnalysisWorkflows() >> []
        }

        when:
        Map model = controller.index()

        then:
        model.refGenomes == [lowerCase, upperCase, legacy]
        model.seqTypes == [seqTypeLowerCase, seqTypeUpperCase, seqTypeC]
        model.analysisWorkflows == []
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "getWorkflows, renders the enabled workflows first, each group sorted by name ignoring the case"() {
        given:
        Workflow disabledUpperCase = buildWorkflow('B disabled', false)
        Workflow disabledLowerCase = buildWorkflow('a disabled', false)
        Workflow enabledUpperCase = buildWorkflow('B enabled')
        Workflow enabledLowerCase = buildWorkflow('a enabled')

        controller.workflowService = Mock(WorkflowService) {
            1 * list() >> [disabledUpperCase, disabledLowerCase, enabledUpperCase, enabledLowerCase]
        }
        controller.workflowVersionService = Mock(WorkflowVersionService) {
            4 * findAllByWorkflow(_) >> []
        }

        when:
        controller.getWorkflows()

        then:
        response.status == SC_OK
        response.json*.name == ['a enabled', 'B enabled', 'a disabled', 'B disabled']
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "getWorkflows, renders the data of a workflow with its versions, default seq types and default reference genomes"() {
        given:
        Workflow workflow = buildWorkflow('workflow')
        workflow.priority = 5 as short
        workflow.maxParallelWorkflows = 3 as short
        workflow.deprecatedDate = LocalDate.of(2026, 1, 2)
        workflow.defaultSeqTypesForWorkflowVersions = [
                buildSeqType('c'),
                buildSeqType('a'),
                buildSeqType('B'),
                buildSeqType('d'),
        ] as Set
        workflow.defaultReferenceGenomesForWorkflowVersions = [
                buildReferenceGenome('legacy a', true),
                buildReferenceGenome('refGen a'),
                buildReferenceGenome('refGen c'),
                buildReferenceGenome('legacy B', true),
                buildReferenceGenome('refGen D'),
                buildReferenceGenome('refGen B'),
        ] as Set
        WorkflowVersion workflowVersion = buildWorkflowVersion(workflow, '1.0')

        controller.workflowService = Mock(WorkflowService) {
            1 * list() >> [workflow]
        }
        controller.workflowVersionService = Mock(WorkflowVersionService) {
            1 * findAllByWorkflow(workflow) >> [workflowVersion]
        }

        when:
        controller.getWorkflows()

        then:
        response.status == SC_OK
        response.json.size() == 1
        with(response.json[0]) {
            name == 'workflow'
            priority == 5
            enabled == true
            maxParallelWorkflows == 3
            deprecationDate == '2026-01-02'
            versions*.name == ['1.0']
            supportedSeqTypes*.displayName == [
                    'a PAIRED bulk',
                    'B PAIRED bulk',
                    'c PAIRED bulk',
                    'd PAIRED bulk',
            ]
            allowedRefGenomes*.displayName == [
                    'refGen a',
                    'refGen B',
                    'refGen c',
                    'refGen D',
                    'legacy a (legacy)',
                    'legacy B (legacy)',
            ]
        }
    }

    void "getWorkflowVersions, renders the versions of the workflow with comment, deprecation date, reference genomes, and seq types"() {
        given:
        Workflow workflow = buildWorkflow('workflow')
        WorkflowVersion workflowVersion = buildWorkflowVersion(workflow, '1.0')
        workflowVersion.deprecatedDate = LocalDate.of(2026, 3, 4)
        workflowVersion.comment = new Comment(comment: 'a comment', author: 'author', modificationDate: Date.from(LocalDate.of(2026, 3, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()))
        workflowVersion.allowedReferenceGenomes = [
                buildReferenceGenome('B'),
                buildReferenceGenome('d'),
                buildReferenceGenome('a'),
                buildReferenceGenome('C'),
        ] as Set
        workflowVersion.supportedSeqTypes = [
                buildSeqType('B'),
                buildSeqType('C'),
                buildSeqType('a'),
                buildSeqType('d'),
        ] as Set

        controller.workflowVersionService = Mock(WorkflowVersionService) {
            1 * findAllByWorkflowId(1) >> [workflowVersion]
        }

        when:
        controller.getWorkflowVersions(1)

        then:
        response.status == SC_OK
        response.json.size() == 1
        with(response.json[0]) {
            name == '1.0'
            comment == 'a comment'
            commentData.author == 'author'
            commentData.date == '2026-03-01'
            deprecateDate == '2026-03-04'
            allowedRefGenomes*.displayName == [
                    'a',
                    'B',
                    'C',
                    'd',
            ]
            supportedSeqTypes*.displayName == [
                    'a PAIRED bulk',
                    'B PAIRED bulk',
                    'C PAIRED bulk',
                    'd PAIRED bulk',
            ]
        }
    }

    void "getWorkflowVersions, when a version has no comment and is not deprecated, then render the defaults"() {
        given:
        WorkflowVersion workflowVersion = buildWorkflowVersion(buildWorkflow('workflow'), '1.0')

        controller.workflowVersionService = Mock(WorkflowVersionService) {
            1 * findAllByWorkflowId(1) >> [workflowVersion]
        }

        when:
        controller.getWorkflowVersions(1)

        then:
        response.status == SC_OK
        with(response.json[0]) {
            comment == ''
            commentData.author == ''
            deprecateDate == 'N/A'
            allowedRefGenomes == []
            supportedSeqTypes == []
        }
    }

    void "updateWorkflowVersion, when the input is valid, then pass it to the service and render the updated version"() {
        given:
        WorkflowVersion workflowVersion = buildWorkflowVersion(buildWorkflow('workflow'), '1.0')
        workflowVersion.comment = new Comment(comment: 'new comment', author: 'author', modificationDate: new Date())

        controller.workflowVersionService = Mock(WorkflowVersionService) {
            1 * updateWorkflowVersion { UpdateWorkflowVersionDto dto ->
                dto.workflowVersionId == 2 &&
                        dto.comment == 'new comment' &&
                        dto.deprecate &&
                        dto.allowedRefGenomes == [3] &&
                        dto.supportedSeqTypes == [4]
            } >> workflowVersion
        }

        when:
        request.method = 'PATCH'
        request.json = [
                workflowVersionId: 2,
                comment          : 'new comment',
                deprecate        : true,
                allowedRefGenomes: [3],
                supportedSeqTypes: [4],
        ]
        controller.updateWorkflowVersion()

        then:
        response.status == SC_OK
        response.json.name == '1.0'
        response.json.comment == 'new comment'
    }

    void "updateWorkflowVersion, when the version is missing, then respond with the status 'not acceptable' and do not update"() {
        given:
        controller.workflowVersionService = Mock(WorkflowVersionService) {
            0 * updateWorkflowVersion(_)
        }

        when:
        request.method = 'PATCH'
        request.json = [
                comment          : 'comment',
                deprecate        : false,
                allowedRefGenomes: [],
                supportedSeqTypes: [],
        ]
        controller.updateWorkflowVersion()

        then:
        response.status == SC_NOT_ACCEPTABLE
    }

    void "updateWorkflow, when the input is valid, then pass it to the service and render the updated workflow"() {
        given:
        Workflow workflow = buildWorkflow('workflow')
        workflow.defaultSeqTypesForWorkflowVersions = [buildSeqType('b'), buildSeqType('a')] as Set
        workflow.defaultReferenceGenomesForWorkflowVersions = [buildReferenceGenome('b'), buildReferenceGenome('a')] as Set

        controller.workflowService = Mock(WorkflowService) {
            1 * updateWorkflow { UpdateWorkflowDto dto ->
                dto.id == 1 &&
                        dto.priority == 5 &&
                        dto.maxParallelWorkflows == 3 &&
                        dto.enabled &&
                        dto.allowedRefGenomes == [6] &&
                        dto.supportedSeqTypes == [7]
            } >> workflow
        }
        controller.workflowVersionService = Mock(WorkflowVersionService) {
            1 * findAllByWorkflow(workflow) >> []
        }

        when:
        request.method = 'POST'
        request.json = [
                id                  : 1,
                priority            : 5,
                maxParallelWorkflows: 3,
                enabled             : true,
                allowedRefGenomes   : [6],
                supportedSeqTypes   : [7],
        ]
        controller.updateWorkflow()

        then:
        response.status == SC_OK
        response.json.name == 'workflow'
        response.json.allowedRefGenomes*.displayName == ['a', 'b']
        response.json.supportedSeqTypes*.displayName == ['a PAIRED bulk', 'b PAIRED bulk']
    }

    void "updateWorkflow, when the updated workflow has no default reference genomes and seq types, then render them as empty lists"() {
        given:
        Workflow workflow = buildWorkflow('workflow')

        controller.workflowService = Mock(WorkflowService) {
            1 * updateWorkflow(_) >> workflow
        }
        controller.workflowVersionService = Mock(WorkflowVersionService) {
            1 * findAllByWorkflow(workflow) >> []
        }

        when:
        request.method = 'POST'
        request.json = [
                id                  : 1,
                priority            : 1,
                maxParallelWorkflows: 2,
                enabled             : true,
                allowedRefGenomes   : [],
                supportedSeqTypes   : [],
        ]
        controller.updateWorkflow()

        then:
        response.status == SC_OK
        response.json.name == 'workflow'
        response.json.allowedRefGenomes == []
        response.json.supportedSeqTypes == []
    }

    void "updateWorkflow, when the priority is not a number, then respond with the status 'not acceptable' and do not update"() {
        given:
        controller.workflowService = Mock(WorkflowService) {
            0 * updateWorkflow(_)
        }

        when:
        request.method = 'POST'
        request.json = [
                id                  : 1,
                priority            : 'high',
                maxParallelWorkflows: 2,
                enabled             : true,
                allowedRefGenomes   : [],
                supportedSeqTypes   : [],
        ]
        controller.updateWorkflow()

        then:
        response.status == SC_NOT_ACCEPTABLE
    }

    private static Workflow buildWorkflow(String name, boolean enabled = true) {
        return new Workflow(name: name, enabled: enabled, priority: 1 as short, maxParallelWorkflows: 1 as short)
    }

    /**
     * The id is set, since the rendering of a version needs it, and a version handed out by the service is always saved.
     */
    private static WorkflowVersion buildWorkflowVersion(Workflow workflow, String version) {
        WorkflowVersion workflowVersion = new WorkflowVersion(apiVersion: new WorkflowApiVersion(workflow: workflow), workflowVersion: version)
        workflowVersion.id = 1
        return workflowVersion
    }

    private static ReferenceGenome buildReferenceGenome(String name, boolean legacy = false) {
        return new ReferenceGenome(name: name, legacy: legacy)
    }

    private static SeqType buildSeqType(String displayName) {
        return new SeqType(displayName: displayName, libraryLayout: SequencingReadType.PAIRED, singleCell: false)
    }
}
