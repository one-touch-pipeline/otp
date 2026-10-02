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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.*
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class WorkflowDefaultGroupServiceSpec extends Specification implements ServiceUnitTest<WorkflowDefaultGroupService>, DataTest,
        DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                ReferenceGenome,
                ReferenceGenomeSelector,
                SeqType,
                Species,
                SpeciesCommonName,
                SpeciesWithStrain,
                Workflow,
                WorkflowApiVersion,
                WorkflowDefaultGroup,
                WorkflowVersion,
                WorkflowVersionSelector,
                WorkflowVersionSelectorDefault,
        ]
    }

    void "list, should return all groups sorted by name"() {
        given:
        WorkflowDefaultGroup groupB = createWorkflowDefaultGroup(name: "B")
        WorkflowDefaultGroup groupA = createWorkflowDefaultGroup(name: "A")
        WorkflowDefaultGroup groupC = createWorkflowDefaultGroup(name: "C")

        expect:
        service.list() == [groupA, groupB, groupC]
    }

    void "findAllBySeqType, should return only groups with the given seqType"() {
        given:
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()
        WorkflowDefaultGroup matching = createWorkflowDefaultGroup(seqType: seqType1)
        createWorkflowDefaultGroup(seqType: seqType2)

        expect:
        service.findAllBySeqType(seqType1) == [matching]
    }

    void "search, should find groups by case-insensitive partial name match"() {
        given:
        WorkflowDefaultGroup matching = createWorkflowDefaultGroup(name: "WGS Default")
        createWorkflowDefaultGroup(name: "Exome Default")

        expect:
        service.search("wgs") == [matching]
    }

    void "findEntries, should return only entries belonging to the given group"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault entry1 = createWorkflowVersionSelectorDefault(group: group)
        WorkflowVersionSelectorDefault entry2 = createWorkflowVersionSelectorDefault(group: group)
        createWorkflowVersionSelectorDefault()

        expect:
        service.findEntries(group) as Set == [entry1, entry2] as Set
    }

    void "create, should create a group with all given entries"() {
        given:
        String name = "someName"
        SeqType seqType = createSeqType()
        WorkflowVersion version1 = createWorkflowVersion(supportedSeqTypes: [seqType] as Set)
        WorkflowVersion version2 = createWorkflowVersion(supportedSeqTypes: [seqType] as Set)

        when:
        WorkflowDefaultGroup group = service.create(name, seqType, [
                new WorkflowDefaultEntryDTO(version1, null, [] as Set),
                new WorkflowDefaultEntryDTO(version2, null, [] as Set),
        ])

        then:
        group.name == name
        group.seqType == seqType
        service.findEntries(group)*.workflowVersion as Set == [version1, version2] as Set
    }

    void "create, should throw an exception when name, seqType or entries are missing"() {
        when:
        service.create(null, createSeqType(), [new WorkflowDefaultEntryDTO(createWorkflowVersion(), null, [] as Set)])

        then:
        AssertionError error1 = thrown(AssertionError)
        error1.message.contains("Parameter 'name' must not be null.")

        when:
        service.create("name", null, [new WorkflowDefaultEntryDTO(createWorkflowVersion(), null, [] as Set)])

        then:
        AssertionError error2 = thrown(AssertionError)
        error2.message.contains("Parameter 'seqType' must not be null.")

        when:
        service.create("name", createSeqType(), [])

        then:
        AssertionError error3 = thrown(AssertionError)
        error3.message.contains("At least one entry is required to create a default group.")
    }

    void "create, should throw an exception when seqType is not supported by a given entry's workflow version"() {
        given:
        SeqType seqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion(supportedSeqTypes: [createSeqType()] as Set)

        when:
        service.create("name", seqType, [new WorkflowDefaultEntryDTO(version, null, [] as Set)])

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("is not supported by all given workflow versions")
    }

    void "update, should update name and seqType without touching entries"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault entry = createWorkflowVersionSelectorDefault(group: group)
        SeqType newSeqType = createSeqType()

        when:
        WorkflowDefaultGroup result = service.update(group, "newName", newSeqType)

        then:
        result == group
        result.name == "newName"
        result.seqType == newSeqType
        service.findEntries(group) == [entry]
    }

    void "update, should throw an exception when group is null"() {
        when:
        service.update(null, "newName", createSeqType())

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("Parameter group must not be null.")
    }

    void "addEntry, should create a new entry linked to the group"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersion version = createWorkflowVersion()
        ReferenceGenome referenceGenome = createReferenceGenome()
        SpeciesWithStrain species = createSpeciesWithStrain()

        when:
        WorkflowVersionSelectorDefault entry = service.addEntry(group, new WorkflowDefaultEntryDTO(version, referenceGenome, [species] as Set))

        then:
        entry.group == group
        entry.workflowVersion == version
        entry.referenceGenome == referenceGenome
        entry.species == [species] as Set
    }

    void "addEntry, should throw an exception when group or workflowVersion are missing"() {
        when:
        service.addEntry(null, new WorkflowDefaultEntryDTO(createWorkflowVersion(), null, [] as Set))

        then:
        AssertionError error1 = thrown(AssertionError)
        error1.message.contains("Parameter 'group' must not be null.")

        when:
        service.addEntry(createWorkflowDefaultGroup(), new WorkflowDefaultEntryDTO(null, null, [] as Set))

        then:
        AssertionError error2 = thrown(AssertionError)
        error2.message.contains("Parameter 'workflowVersion' must not be null.")
    }

    void "saveGroup, when group is null, should create a new group with an entry per workflow version"() {
        given:
        String name = "someName"
        SeqType seqType = createSeqType()
        WorkflowVersion version1 = createWorkflowVersion(supportedSeqTypes: [seqType] as Set)
        WorkflowVersion version2 = createWorkflowVersion(supportedSeqTypes: [seqType] as Set)

        when:
        WorkflowDefaultGroup group = service.saveGroup(null, name, seqType, [version1, version2] as Set)

        then:
        group.name == name
        group.seqType == seqType
        service.findEntries(group)*.workflowVersion as Set == [version1, version2] as Set
    }

    void "saveGroup, when group is given, should update its name and seqType"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        SeqType newSeqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion(supportedSeqTypes: [newSeqType] as Set)

        when:
        WorkflowDefaultGroup result = service.saveGroup(group, "newName", newSeqType, [version] as Set)

        then:
        result == group
        result.name == "newName"
        result.seqType == newSeqType
    }

    void "saveGroup, when group is given, should add missing entries and remove entries no longer present"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault keptEntry = createWorkflowVersionSelectorDefault(
                group: group, workflowVersion: createWorkflowVersion(supportedSeqTypes: [group.seqType] as Set)
        )
        WorkflowVersionSelectorDefault removedEntry = createWorkflowVersionSelectorDefault(group: group)
        WorkflowVersion newVersion = createWorkflowVersion(supportedSeqTypes: [group.seqType] as Set)

        when:
        service.saveGroup(group, group.name, group.seqType, [keptEntry.workflowVersion, newVersion] as Set)

        then:
        service.findEntries(group)*.workflowVersion as Set == [keptEntry.workflowVersion, newVersion] as Set
        !WorkflowVersionSelectorDefault.exists(removedEntry.id)
    }

    void "saveGroup, when group is given, should leave an untouched entry's dateCreated unchanged"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault keptEntry = createWorkflowVersionSelectorDefault(
                group: group, workflowVersion: createWorkflowVersion(supportedSeqTypes: [group.seqType] as Set)
        )
        Date originalDateCreated = keptEntry.dateCreated

        when:
        service.saveGroup(group, group.name, group.seqType, [keptEntry.workflowVersion] as Set)

        then:
        WorkflowVersionSelectorDefault result = service.findEntries(group).find { it.workflowVersion == keptEntry.workflowVersion }
        result.id == keptEntry.id
        result.dateCreated == originalDateCreated
    }

    void "saveGroup, should throw an exception when no workflow versions are given"() {
        when:
        service.saveGroup(createWorkflowDefaultGroup(), "name", createSeqType(), [] as Set)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("At least one workflow version is required to save a default group.")
    }

    void "saveGroup, should throw an exception when multiple versions of the same workflow are given"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowVersion version1 = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: workflow))
        WorkflowVersion version2 = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: workflow))

        when:
        service.saveGroup(null, "name", createSeqType(), [version1, version2] as Set)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("Only one version of the same workflow is allowed in a default group.")
    }

    void "saveGroup, should throw an exception when seqType is not supported by a given workflow version"() {
        given:
        SeqType seqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion(supportedSeqTypes: [createSeqType()] as Set)

        when:
        service.saveGroup(null, "name", seqType, [version] as Set)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("is not supported by all given workflow versions")
    }

    void "deleteEntry, should remove the entry"() {
        given:
        WorkflowVersionSelectorDefault entry = createWorkflowVersionSelectorDefault()

        when:
        service.deleteEntry(entry)

        then:
        WorkflowVersionSelectorDefault.count == 0
    }

    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "delete, should remove the group and all its entries"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        createWorkflowVersionSelectorDefault(group: group)
        createWorkflowVersionSelectorDefault(group: group)
        WorkflowDefaultGroup otherGroup = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault otherEntry = createWorkflowVersionSelectorDefault(group: otherGroup)

        when:
        service.delete(group)

        then:
        WorkflowDefaultGroup.all == [otherGroup]
        WorkflowVersionSelectorDefault.all == [otherEntry]
    }

    void "applyToProject, should apply every entry via workflowVersionSelectorService, and referenceGenomeSelectorService only when referenceGenome is set"() {
        given:
        Project project = createProject()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault analysisEntry = createWorkflowVersionSelectorDefault(group: group, referenceGenome: null, species: [] as Set)
        ReferenceGenome referenceGenome = createReferenceGenome()
        SpeciesWithStrain species = createSpeciesWithStrain()
        WorkflowVersionSelectorDefault alignmentEntry = createWorkflowVersionSelectorDefault(
                group: group, referenceGenome: referenceGenome, species: [species] as Set
        )

        WorkflowVersionSelector wvSelector1 = createWorkflowVersionSelector()
        WorkflowVersionSelector wvSelector2 = createWorkflowVersionSelector()
        ReferenceGenomeSelector rgSelector = createReferenceGenomeSelector()

        service.workflowVersionSelectorService = Mock(WorkflowVersionSelectorService)
        service.referenceGenomeSelectorService = Mock(ReferenceGenomeSelectorService)

        when:
        List<AppliedDefaultEntry> result = service.applyToProject(project, group)

        then:
        1 * service.workflowVersionSelectorService.createOrUpdate(project, group.seqType, analysisEntry.workflowVersion) >> wvSelector1
        1 * service.workflowVersionSelectorService.createOrUpdate(project, group.seqType, alignmentEntry.workflowVersion) >> wvSelector2
        0 * service.workflowVersionSelectorService._

        and:
        1 * service.referenceGenomeSelectorService.createOrUpdate(
                project, group.seqType, [species], alignmentEntry.workflowVersion.workflow, referenceGenome
        ) >> rgSelector
        0 * service.referenceGenomeSelectorService._

        and:
        result.size() == 2
        AppliedDefaultEntry appliedAnalysis = exactlyOneElement(result.findAll { it.workflowVersionSelector == wvSelector1 })
        appliedAnalysis.referenceGenomeSelector == null
        AppliedDefaultEntry appliedAlignment = exactlyOneElement(result.findAll { it.workflowVersionSelector == wvSelector2 })
        appliedAlignment.referenceGenomeSelector == rgSelector
    }
}
