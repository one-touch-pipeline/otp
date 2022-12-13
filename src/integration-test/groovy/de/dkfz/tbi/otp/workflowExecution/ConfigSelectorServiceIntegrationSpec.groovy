/*
 * Copyright 2011-2020 The OTP authors
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
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class ConfigSelectorServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, DomainFactoryProcessingPriority, UserAndRoles {

    @SuppressWarnings("ParameterCount")
    ExternalWorkflowConfigSelector createEWCSHelperExtendedCriteria(String name,
                                                                    Set<Project> projects,
                                                                    Set<WorkflowVersion> workflowVersions,
                                                                    Set<Workflow> workflows,
                                                                    Set<SeqType> seqTypes,
                                                                    Set<ReferenceGenome> referenceGenomes,
                                                                    Set<LibraryPreparationKit> libraryPreparationKits) {
        return createExternalWorkflowConfigSelector([
                name                  : name,
                projects              : projects,
                workflowVersions      : workflowVersions,
                workflows             : workflows,
                seqTypes              : seqTypes,
                referenceGenomes      : referenceGenomes,
                libraryPreparationKits: libraryPreparationKits,
        ])
    }

    @Autowired
    ConfigSelectorService service

    void setupData() {
        Project project1 = createProject(name: "p1")
        Project project2 = createProject(name: "p2")
        createProject(name: "p3")

        Workflow workflow1 = createWorkflow(name: "w1")
        Workflow workflow2 = createWorkflow(name: "w2")
        createWorkflow(name: "w3")

        WorkflowVersion workflowVersion1 = createWorkflowVersion([workflow: workflow1, workflowVersion: "v1"])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([workflow: workflow2, workflowVersion: "v2"])

        SeqType seqType1 = createSeqType(name: "s1")
        SeqType seqType2 = createSeqType(name: "s2")

        ReferenceGenome referenceGenome1 = createReferenceGenome(name: "r1")
        ReferenceGenome referenceGenome2 = createReferenceGenome(name: "r2")

        LibraryPreparationKit libraryPreparationKit1 = createLibraryPreparationKit(name: "l1")
        LibraryPreparationKit libraryPreparationKit2 = createLibraryPreparationKit(name: "l2")

        //priority : 0b01111111 = 127
        createEWCSHelperExtendedCriteria(
                "ewcs1",
                [project1] as Set<Project>,
                [workflowVersion1] as Set<WorkflowVersion>,
                [workflow1] as Set<Workflow>,
                [seqType1] as Set<SeqType>,
                [referenceGenome1] as Set<ReferenceGenome>,
                [libraryPreparationKit1] as Set<LibraryPreparationKit>
        )
        //priority : 0b01011011 = 91
        createEWCSHelperExtendedCriteria(
                "ewcs2",
                [project2] as Set<Project>,
                [] as Set<WorkflowVersion>,
                [workflow2] as Set<Workflow>,
                [seqType2] as Set<SeqType>,
                [referenceGenome2] as Set<ReferenceGenome>,
                [] as Set<LibraryPreparationKit>
        )
        //priority : 0b01111111 = 127
        createEWCSHelperExtendedCriteria(
                "ewcs3",
                [project1, project2] as Set<Project>,
                [workflowVersion1, workflowVersion2] as Set<WorkflowVersion>,
                [workflow1, workflow2] as Set<Workflow>,
                [seqType1, seqType2] as Set<SeqType>,
                [referenceGenome1, referenceGenome2] as Set<ReferenceGenome>,
                [libraryPreparationKit1, libraryPreparationKit2] as Set<LibraryPreparationKit>
        )
        //priority : 0b01110111 = 119
        createEWCSHelperExtendedCriteria(
                "ewcs4",
                [project2] as Set<Project>,
                [workflowVersion2] as Set<WorkflowVersion>,
                [workflow2] as Set<Workflow>,
                [seqType1] as Set<SeqType>,
                [] as Set<ReferenceGenome>,
                [libraryPreparationKit1] as Set<LibraryPreparationKit>
        )
        //priority : 0b00000001 = 1
        createEWCSHelperExtendedCriteria(
                "ewcs5",
                [] as Set<Project>,
                [] as Set<WorkflowVersion>,
                [] as Set<Workflow>,
                [] as Set<SeqType>,
                [] as Set<ReferenceGenome>,
                [] as Set<LibraryPreparationKit>
        )
    }

    void "test findExactSelectors, anyValueSet"() {
        given:
        Workflow workflow1 = createWorkflow(name: "w1")
        Workflow workflow2 = createWorkflow(name: "w2")

        WorkflowVersion workflowVersion1 = createWorkflowVersion([workflow: workflow1, workflowVersion: "v1"])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([workflow: workflow2, workflowVersion: "v2"])

        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector([
                workflows       : [workflow1, workflow2],
                workflowVersions: [workflowVersion1, workflowVersion2],
        ])

        MultiSelectSelectorExtendedCriteria multiSelectSelectorExtendedCriteria = new MultiSelectSelectorExtendedCriteria(
                workflows: selector.workflows,
                workflowVersions: selector.workflowVersions,
                seqTypes: selector.seqTypes,
                projects: selector.projects,
                referenceGenomes: selector.referenceGenomes,
                libraryPreparationKits: selector.libraryPreparationKits
        )

        when:
        List<ExternalWorkflowConfigSelector> foundSelectors = service.findExactSelectors(multiSelectSelectorExtendedCriteria)

        then:
        foundSelectors.size() == 1
        foundSelectors[0] == selector
    }

    @Unroll
    @SuppressWarnings(["AvoidFindWithoutAll", "LineLength"])
    void "findAllSelectors #x"() {
        setupData()

        SingleSelectSelectorExtendedCriteria selectSelectorExtendedCriteria = new SingleSelectSelectorExtendedCriteria([
                project              : projects(),
                workflowVersion      : workflowVersions(),
                workflow             : workflows(),
                seqType              : seqTypes(),
                referenceGenome      : referenceGenomes(),
                libraryPreparationKit: libraryPreparationKits(),
        ])

        expect:
        CollectionUtils.containSame(result, service.findAllSelectors(selectSelectorExtendedCriteria)*.name)

        where:
        x | projects                                                          | workflowVersions                                                                     | workflows                                                          | seqTypes                                                          | referenceGenomes                                                          | libraryPreparationKits                                                          | result
        1 | { CollectionUtils.atMostOneElement(Project.findAllByName("p1")) } | { CollectionUtils.atMostOneElement(WorkflowVersion.findAllByWorkflowVersion("v1")) } | { CollectionUtils.atMostOneElement(Workflow.findAllByName("w1")) } | { CollectionUtils.atMostOneElement(SeqType.findAllByName("s1")) } | { CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName("r1")) } | { CollectionUtils.atMostOneElement(LibraryPreparationKit.findAllByName("l1")) } | ["ewcs1", "ewcs3", "ewcs5"]
        2 | { null }                                                          | { null }                                                                             | { CollectionUtils.atMostOneElement(Workflow.findAllByName("w2")) } | { null }                                                          | { null }                                                                  | { null }                                                                        | ["ewcs5"]
        3 | { CollectionUtils.atMostOneElement(Project.findAllByName("p1")) } | { null }                                                                             | { null }                                                           | { null }                                                          | { null }                                                                  | { null }                                                                        | ["ewcs5"]
        4 | { CollectionUtils.atMostOneElement(Project.findAllByName("p1")) } | { null }                                                                             | { null }                                                           | { CollectionUtils.atMostOneElement(SeqType.findAllByName("s2")) } | { null }                                                                  | { null }                                                                        | ["ewcs5"]
        5 | { null }                                                          | { null }                                                                             | { CollectionUtils.atMostOneElement(Workflow.findAllByName("w3")) } | { null }                                                          | { null }                                                                  | { null }                                                                        | ["ewcs5"]
        6 | { CollectionUtils.atMostOneElement(Project.findAllByName("p3")) } | { null }                                                                             | { null }                                                           | { null }                                                          | { null }                                                                  | { null }                                                                        | ["ewcs5"]
        7 | { null }                                                          | { null }                                                                             | { null }                                                           | { null }                                                          | { null }                                                                  | { null }                                                                        | []
    }

    void "findAllSelectorsSortedByPriority, assure correct sorting"() {
        given:
        setupData()

        expect:
        service.findAllSelectorsSortedByPriority(new SingleSelectSelectorExtendedCriteria(
                project              : CollectionUtils.atMostOneElement(Project.findAllByName("p1")),
                workflowVersion      : CollectionUtils.atMostOneElement(WorkflowVersion.findAllByWorkflowVersion("v1")),
                workflow             : CollectionUtils.atMostOneElement(Workflow.findAllByName("w1")),
                seqType              : CollectionUtils.atMostOneElement(SeqType.findAllByName("s1")),
                referenceGenome      : CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName("r1")),
                libraryPreparationKit: CollectionUtils.atMostOneElement(LibraryPreparationKit.findAllByName("l1")),
        ))*.name == ["ewcs1", "ewcs3", "ewcs5"]
    }

    @Unroll
    @SuppressWarnings("LineLength")
    void "test findAllRelatedSelectors #x"() {
        given:
        setupData()

        MultiSelectSelectorExtendedCriteria selectorExtendedCriteria = new MultiSelectSelectorExtendedCriteria([
                projects              : projects(),
                workflowVersions      : workflowVersions(),
                workflows             : workflows(),
                seqTypes              : seqTypes(),
                referenceGenomes      : referenceGenomes(),
                libraryPreparationKits: libraryPreparationKits(),
        ])
        expect:
        CollectionUtils.containSame(result, service.findAllRelatedSelectors(selectorExtendedCriteria)*.name)

        where:
        x | projects                        | workflowVersions                                   | workflows                        | seqTypes                        | referenceGenomes                        | libraryPreparationKits                        | result
        1 | { Project.findAllByName("p1") } | { WorkflowVersion.findAllByWorkflowVersion("v1") } | { Workflow.findAllByName("w1") } | { SeqType.findAllByName("s1") } | { ReferenceGenome.findAllByName("r1") } | { LibraryPreparationKit.findAllByName("l1") } | ["ewcs1", "ewcs3"]
        2 | { null }                        | { null }                                           | { Workflow.findAllByName("w2") } | { null }                        | { null }                                | { null }                                      | ["ewcs2", "ewcs3", "ewcs4"]
        3 | { Project.findAllByName("p1") } | { null }                                           | { null }                         | { SeqType.findAllByName("s2") } | { null }                                | { null }                                      | ["ewcs3"]
        4 | { Project.findAll() }           | { null }                                           | { null }                         | { null }                        | { null }                                | { null }                                      | ["ewcs1", "ewcs2", "ewcs3", "ewcs4"]
        5 | { null }                        | { null }                                           | { null }                         | { null }                        | { ReferenceGenome.findAll() }           | { null }                                      | ["ewcs1", "ewcs2", "ewcs3"]
        6 | { null }                        | { null }                                           | { Workflow.findAllByName("w3") } | { null }                        | { null }                                | { null }                                      | []
        7 | { Project.findAllByName("p3") } | { null }                                           | { null }                         | { null }                        | { null }                                | { null }                                      | []
        8 | { null }                        | { null }                                           | { null }                         | { null }                        | { null }                                | { null }                                      | []
    }

    @Unroll
    void "test findRelatedSelectorsByName #x"() {
        given:
        createExternalWorkflowConfigSelector([name: "ewcs1"])
        createExternalWorkflowConfigSelector([name: "ewcs2"])
        createExternalWorkflowConfigSelector([name: "ewcs3"])
        createExternalWorkflowConfigSelector([name: "ewcs4"])

        expect:
        CollectionUtils.containSame(result, service.findRelatedSelectorsByName(name())*.name)

        where:
        x | name        | result
        1 | { "ewcs1" } | ["ewcs1"]
        2 | { "wcs" }   | ["ewcs1", "ewcs2", "ewcs3", "ewcs4"]
        3 | { "" }      | []
        4 | { null }    | []
    }

    void "create, should create a new selector and a new fragment"() {
        given:
        createUserAndRoles()
        CreateCommand cmd = new CreateCommand(
                workflows: [],
                workflowVersions: [],
                projects: [],
                seqTypes: [],
                referenceGenomes: [],
                libraryPreparationKits: [],

                selectorName: "selectorName",
                type: SelectorType.GENERIC,
                value: '{"OTP_CLUSTER": {"MEMORY": "1"}}',
        )

        when:
        doWithAuth(ADMIN) {
            service.create(cmd)
        }

        then:
        ExternalWorkflowConfigSelector.all.size() == 1
        ExternalWorkflowConfigSelector selector = ExternalWorkflowConfigSelector.all.first()
        selector.name == "selectorName"

        ExternalWorkflowConfigFragment.all.size() == 1
        ExternalWorkflowConfigFragment fragment = ExternalWorkflowConfigFragment.all.first()
        fragment.name.contains(selector.name)
        fragment.deprecationDate == null

        selector.externalWorkflowConfigFragment == fragment
        fragment.findSelector().get() == selector
    }

    private UpdateCommand getUpdateCommand() {
        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector()
        return new UpdateCommand(
                selector: selector,

                workflows: selector.workflows.toList(),
                workflowVersions: selector.workflowVersions.toList(),
                projects: selector.projects.toList(),
                seqTypes: selector.seqTypes.toList(),
                referenceGenomes: selector.referenceGenomes.toList(),
                libraryPreparationKits: selector.libraryPreparationKits.toList(),

                selectorName: selector.name,
                type: selector.selectorType,
                value: selector.externalWorkflowConfigFragment.configValues,
        )
    }

    void "update, should update the selector when values are changed"() {
        given:
        createUserAndRoles()
        UpdateCommand cmd = updateCommand
        ExternalWorkflowConfigSelector selector = cmd.selector
        ExternalWorkflowConfigFragment fragment = cmd.selector.externalWorkflowConfigFragment
        cmd.selectorName = "new name"

        when:
        doWithAuth(ADMIN) {
            service.update(cmd)
        }

        then:
        ExternalWorkflowConfigSelector.all.size() == 1
        selector.name == "new name"
        ExternalWorkflowConfigFragment.all.size() == 1
        selector.externalWorkflowConfigFragment == fragment
    }

    void "update, should create a new fragment with the same name, when the value is changed"() {
        given:
        createUserAndRoles()
        UpdateCommand cmd = updateCommand

        String newConfigValue = '{"RODDY":{"cvalues":{"mergedBamSuffixList":{"type":"string","value":"newValue"}}}}'
        cmd.value = newConfigValue
        ExternalWorkflowConfigSelector selector = cmd.selector
        ExternalWorkflowConfigFragment oldFragment = cmd.selector.externalWorkflowConfigFragment

        when:
        doWithAuth(ADMIN) {
            service.update(cmd)
        }

        then:
        ExternalWorkflowConfigSelector.all.size() == 1
        ExternalWorkflowConfigFragment.all.size() == 2
        oldFragment.deprecationDate != null
        oldFragment.name == selector.externalWorkflowConfigFragment.name
        selector.externalWorkflowConfigFragment.deprecationDate == null
        selector.externalWorkflowConfigFragment != oldFragment
        selector.externalWorkflowConfigFragment.configValues == newConfigValue
    }

    void "update, should not change the fragment or create a new one when no values are changed"() {
        given:
        createUserAndRoles()
        UpdateCommand cmd = updateCommand

        when:
        doWithAuth(ADMIN) {
            service.update(cmd)
        }

        then:
        ExternalWorkflowConfigSelector.all.size() == 1
        ExternalWorkflowConfigFragment.all.size() == 1
    }

    void "test deprecate"() {
        given:
        createUserAndRoles()
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment()
        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector(externalWorkflowConfigFragment: fragment)

        when:
        doWithAuth(ADMIN) {
            service.deprecate(selector)
        }

        then:
        fragment.deprecationDate != null
        ExternalWorkflowConfigSelector.all.empty
    }
}
