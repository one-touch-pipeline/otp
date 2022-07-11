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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class ConfigSelectorServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, DomainFactoryProcessingPriority {

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

    void setupData() {
        Project project1 = createProject(name: "p1")
        Project project2 = createProject(name: "p2")
        createProject(name: "p3")

        Workflow workflow1 = createWorkflow(name: "w1")
        Workflow workflow2 = createWorkflow(name: "w2")
        createWorkflow(name: "w3")

        WorkflowVersion workflowVersion1 = createWorkflowVersion(workflowVersion: "v1")
        WorkflowVersion workflowVersion2 = createWorkflowVersion(workflowVersion: "v2")

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
                [workflowVersion1] as Set<WorkflowVersion>,
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
        ConfigSelectorService service = new ConfigSelectorService()
        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector()

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
        ConfigSelectorService service = new ConfigSelectorService()
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
        x | projects                     | workflowVersions                                | workflows                     | seqTypes                     | referenceGenomes                     | libraryPreparationKits                     | result
        1 | { CollectionUtils.atMostOneElement(Project.findAllByName("p1")) } | { CollectionUtils.atMostOneElement(WorkflowVersion.findAllByWorkflowVersion("v1")) } | { CollectionUtils.atMostOneElement(Workflow.findAllByName("w1")) } | { CollectionUtils.atMostOneElement(SeqType.findAllByName("s1")) } | { CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName("r1")) } | { CollectionUtils.atMostOneElement(LibraryPreparationKit.findAllByName("l1")) } | ["ewcs1", "ewcs3", "ewcs5"]
        2 | { null }                     | { null }                                        | { CollectionUtils.atMostOneElement(Workflow.findAllByName("w2")) } | { null }                     | { null }                             | { null }                                   | ["ewcs2", "ewcs3", "ewcs4", "ewcs5"]
        3 | { CollectionUtils.atMostOneElement(Project.findAllByName("p1")) } | { null }                                        | { null }                      | { null }                     | { null }                             | { null }                                   | ["ewcs1", "ewcs3", "ewcs5"]
        4 | { CollectionUtils.atMostOneElement(Project.findAllByName("p1")) } | { null }                                        | { null }                      | { CollectionUtils.atMostOneElement(SeqType.findAllByName("s2")) } | { null }                             | { null }                                   | ["ewcs3", "ewcs5"]
        5 | { null }                     | { null }                                        | { CollectionUtils.atMostOneElement(Workflow.findAllByName("w3")) } | { null }                     | { null }                             | { null }                                   | ["ewcs5"]
        6 | { CollectionUtils.atMostOneElement(Project.findAllByName("p3")) } | { null }                                        | { null }                      | { null }                     | { null }                             | { null }                                   | ["ewcs5"]
        7 | { null }                     | { null }                                        | { null }                      | { null }                     | { null }                             | { null }                                   | []
    }

    @Unroll
    @SuppressWarnings("LineLength")
    void "test findAllRelatedSelectors #x"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
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
        3 | { Project.findAllByName("p1") } | { null }                                           | { null }                         | { SeqType.findAllByName("s2") } | { null }                                | { null }                                      | ["ewcs1", "ewcs2", "ewcs3"]
        4 | { Project.findAll() }           | { null }                                           | { null }                         | { null }                        | { null }                                | { null }                                      | ["ewcs1", "ewcs2", "ewcs3", "ewcs4"]
        5 | { null }                        | { null }                                           | { null }                         | { null }                        | { ReferenceGenome.findAll() }           | { null }                                      | ["ewcs1", "ewcs2", "ewcs3"]
        6 | { null }                        | { null }                                           | { Workflow.findAllByName("w3") } | { null }                        | { null }                                | { null }                                      | []
        7 | { Project.findAllByName("p3") } | { null }                                           | { null }                         | { null }                        | { null }                                | { null }                                      | []
        8 | { null }                        | { null }                                           | { null }                         | { null }                        | { null }                                | { null }                                      | []
    }

    @Unroll
    @SuppressWarnings("LineLength")
    void "test findExactSelector #x"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        setupData()

        MultiSelectSelectorExtendedCriteria multiSelectSelectorExtendedCriteria = new MultiSelectSelectorExtendedCriteria([
                projects              : projects(),
                workflowVersions      : workflowVersions(),
                workflows             : workflows(),
                seqTypes              : seqTypes(),
                referenceGenomes      : referenceGenomes(),
                libraryPreparationKits: libraryPreparationKits(),
        ])
        expect:
        result == service.findExactSelector(multiSelectSelectorExtendedCriteria)?.name

        where:
        x | projects                                      | workflowVersions                                                 | workflows                                      | seqTypes                                      | referenceGenomes                                      | libraryPreparationKits                                      | result
        1 | { Project.findAllByName("p1") }               | { WorkflowVersion.findAllByWorkflowVersion("v1") }               | { Workflow.findAllByName("w1") }               | { SeqType.findAllByName("s1") }               | { ReferenceGenome.findAllByName("r1") }               | { LibraryPreparationKit.findAllByName("l1") }               | "ewcs1"
        2 | { Project.findAllByName("p2") }               | { null }                                                         | { Workflow.findAllByName("w2") }               | { SeqType.findAllByName("s2") }               | { ReferenceGenome.findAllByName("r2") }               | { null }                                                    | "ewcs2"
        3 | { Project.findAllByNameInList(["p1", "p2"]) } | { WorkflowVersion.findAllByWorkflowVersionInList(["v1", "v2"]) } | { Workflow.findAllByNameInList(["w1", "w2"]) } | { SeqType.findAllByNameInList(["s1", "s2"]) } | { ReferenceGenome.findAllByNameInList(["r1", "r2"]) } | { LibraryPreparationKit.findAllByNameInList(["l1", "l2"]) } | "ewcs3"
        4 | { Project.findAllByName("p2") }               | { WorkflowVersion.findAllByWorkflowVersion("v1") }               | { Workflow.findAllByName("w2") }               | { SeqType.findAllByName("s1") }               | { null }                                              | { LibraryPreparationKit.findAllByName("l1") }               | "ewcs4"
        5 | { null }                                      | { null }                                                         | { null }                                       | { null }                                      | { null }                                              | { null }                                                    | "ewcs5"
        6 | { Project.findAllByName("p3") }               | { null }                                                         | { null }                                       | { null }                                      | { null }                                              | { null }                                                    | null
        7 | { Project.findAll() }                         | { WorkflowVersion.findAllByWorkflowVersionInList(["v1", "v2"]) } | { Workflow.findAllByNameInList(["w1", "w2"]) } | { SeqType.findAllByNameInList(["s1", "s2"]) } | { ReferenceGenome.findAllByNameInList(["r1", "r2"]) } | { LibraryPreparationKit.findAllByNameInList(["l1", "l2"]) } | null
        8 | { null }                                      | { WorkflowVersion.findAllByWorkflowVersionInList(["v1", "v2"]) } | { null }                                       | { null }                                      | { null }                                              | { null }                                                    | null
    }

    void "findAllRelatedSelectorsSortedByPriority, assure correct sorting"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        setupData()

        ExternalWorkflowConfigSelector ewcs1 = CollectionUtils.atMostOneElement(ExternalWorkflowConfigSelector.findAllByName("ewcs1"))
        ewcs1.save(flush: true)

        ExternalWorkflowConfigSelector ewcs3 = CollectionUtils.atMostOneElement(ExternalWorkflowConfigSelector.findAllByName("ewcs3"))
        ewcs3.save(flush: true)

        ExternalWorkflowConfigSelector ewcs4 = CollectionUtils.atMostOneElement(ExternalWorkflowConfigSelector.findAllByName("ewcs4"))
        ewcs4.save(flush: true)

        ExternalWorkflowConfigSelector ewcs5 = CollectionUtils.atMostOneElement(ExternalWorkflowConfigSelector.findAllByName("ewcs5"))
        ewcs5.save(flush: true)

        expect:
        service.findAllSelectorsSortedByPriority(
                new SingleSelectSelectorExtendedCriteria(seqType: SeqType.findAllByName("s1").first())
        )*.name == [ewcs1.name, ewcs3.name, ewcs4.name, ewcs5.name]
    }

    ExternalWorkflowConfigSelector createEWCSHelperBaseCriteria(String name) {
        return createExternalWorkflowConfigSelector([
                name        : name,
        ])
    }

    @Unroll
    void "test findRelatedSelectorsByName #x"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        createEWCSHelperBaseCriteria("ewcs1")
        createEWCSHelperBaseCriteria("ewcs2")
        createEWCSHelperBaseCriteria("ewcs3")
        createEWCSHelperBaseCriteria("ewcs4")

        expect:
        CollectionUtils.containSame(result, service.findRelatedSelectorsByName(name())*.name)

        where:
        x | name        | result
        1 | { "ewcs1" } | ["ewcs1"]
        2 | { "wcs" }   | ["ewcs1", "ewcs2", "ewcs3", "ewcs4"]
        3 | { "" }      | []
        4 | { null }    | []
    }

    void "test create"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        CreateCommand cmd = new CreateCommand(
                workflows: [],
                workflowVersions: [],
                projects: [],
                seqTypes: [],
                referenceGenomes: [],
                libraryPreparationKits: [],

                selectorName: "selectorName",
                type: SelectorType.GENERIC,
                fragmentName: "fragmentName",
                value: '{"OTP_CLUSTER": {"MEMORY": "1"}}',
        )

        when:
        service.create(cmd)

        then:
        ExternalWorkflowConfigFragment.all.size() == 1
        ExternalWorkflowConfigFragment fragment = ExternalWorkflowConfigFragment.all.first()
        fragment.name == "fragmentName"

        ExternalWorkflowConfigSelector.all.size() == 1
        ExternalWorkflowConfigSelector selector = ExternalWorkflowConfigSelector.all.first()
        selector.name == "selectorName"
        selector.externalWorkflowConfigFragment == fragment
        fragment.selector.get() == selector
    }

    private UpdateCommand getUpdateCommand() {
        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector()
        return new UpdateCommand(
                selector: selector,
                fragment: selector.externalWorkflowConfigFragment,

                workflows: selector.workflows.toList(),
                workflowVersions: selector.workflowVersions.toList(),
                projects: selector.projects.toList(),
                seqTypes: selector.seqTypes.toList(),
                referenceGenomes: selector.referenceGenomes.toList(),
                libraryPreparationKits: selector.libraryPreparationKits.toList(),

                selectorName: selector.name,
                type: selector.selectorType,
                fragmentName: selector.externalWorkflowConfigFragment.name,
                value: selector.externalWorkflowConfigFragment.configValues,
        )
    }

    void "test update selector"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        UpdateCommand cmd = updateCommand
        ExternalWorkflowConfigSelector selector = cmd.selector
        ExternalWorkflowConfigFragment fragment = cmd.selector.externalWorkflowConfigFragment
        cmd.selectorName = "new name"

        when:
        service.update(cmd)

        then:
        ExternalWorkflowConfigSelector.all.size() == 1
        selector.name == "new name"
        ExternalWorkflowConfigFragment.all.size() == 1
        selector.externalWorkflowConfigFragment == fragment
    }

    void "test update fragment"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        UpdateCommand cmd = updateCommand
        ExternalWorkflowConfigSelector selector = cmd.selector
        ExternalWorkflowConfigFragment fragment = cmd.selector.externalWorkflowConfigFragment
        cmd.fragmentName = "new name"

        when:
        service.update(cmd)

        then:
        ExternalWorkflowConfigSelector.all.size() == 1
        ExternalWorkflowConfigFragment.all.size() == 2
        fragment.deprecationDate != null
        selector.externalWorkflowConfigFragment != fragment
        selector.externalWorkflowConfigFragment.name == "new name"
    }

    void "test deprecate"() {
        given:
        ConfigSelectorService service = new ConfigSelectorService()
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment()
        createExternalWorkflowConfigSelector(externalWorkflowConfigFragment: fragment)

        when:
        service.deprecate(fragment)

        then:
        fragment.deprecationDate != null
        ExternalWorkflowConfigSelector.all.empty
    }
}
