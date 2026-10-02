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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.SqlUtil

@Transactional
class WorkflowDefaultGroupService {

    WorkflowVersionSelectorService workflowVersionSelectorService
    ReferenceGenomeSelectorService referenceGenomeSelectorService

    @CompileDynamic
    List<WorkflowDefaultGroup> list() {
        return WorkflowDefaultGroup.list(sort: "name")
    }

    @CompileDynamic
    List<WorkflowDefaultGroup> findAllBySeqType(SeqType seqType) {
        return WorkflowDefaultGroup.findAllBySeqType(seqType, [sort: "name"])
    }

    @CompileDynamic
    List<WorkflowDefaultGroup> search(String query) {
        return WorkflowDefaultGroup.createCriteria().list(sort: "name") {
            ilike("name", "%${SqlUtil.replaceWildcardCharactersInLikeExpression(query)}%")
        } as List<WorkflowDefaultGroup>
    }

    @CompileDynamic
    List<WorkflowVersionSelectorDefault> findEntries(WorkflowDefaultGroup group) {
        return WorkflowVersionSelectorDefault.findAllByGroup(group, [sort: "id"])
    }

    @CompileDynamic
    List<WorkflowVersionSelectorDefault> findAllEntries() {
        return WorkflowVersionSelectorDefault.list(sort: "id")
    }

    @CompileDynamic
    WorkflowDefaultGroup create(String name, SeqType seqType, List<WorkflowDefaultEntryDTO> entries) {
        assert name: "Parameter 'name' must not be null."
        assert seqType: "Parameter 'seqType' must not be null."
        assert entries: "At least one entry is required to create a default group."
        assert entries*.workflowVersion.every { it.supportedSeqTypes.contains(seqType) } :
                "The seqType ${seqType} is not supported by all given workflow versions."

        WorkflowDefaultGroup group = new WorkflowDefaultGroup(name: name, seqType: seqType).save(flush: true)
        entries.each { addEntry(group, it) }
        return group
    }

    WorkflowDefaultGroup update(WorkflowDefaultGroup group, String name, SeqType seqType) {
        assert group: "Parameter group must not be null."

        group.name = name
        group.seqType = seqType
        return group.save(flush: true)
    }

    @CompileDynamic
    WorkflowDefaultGroup saveGroup(WorkflowDefaultGroup group, String name, SeqType seqType, Set<WorkflowVersion> workflowVersions) {
        assert workflowVersions: "At least one workflow version is required to save a default group."
        List<Workflow> workflows = workflowVersions*.workflow
        assert workflows.size() == workflows.unique(false).size() :
                "Only one version of the same workflow is allowed in a default group."
        assert workflowVersions.every { it.supportedSeqTypes.contains(seqType) } :
                "The seqType ${seqType} is not supported by all given workflow versions."

        if (!group) {
            return create(name, seqType, workflowVersions.collect { new WorkflowDefaultEntryDTO(it, null, [] as Set) })
        }

        WorkflowDefaultGroup savedGroup = update(group, name, seqType)
        Set<WorkflowVersionSelectorDefault> existingEntries = findEntries(savedGroup)

        existingEntries.findAll { !(it.workflowVersion in workflowVersions) }.each { deleteEntry(it) }
        workflowVersions.findAll { version -> !existingEntries.any { it.workflowVersion == version } }
                .each { addEntry(savedGroup, new WorkflowDefaultEntryDTO(it, null, [] as Set)) }

        return savedGroup
    }

    @CompileDynamic
    WorkflowVersionSelectorDefault addEntry(WorkflowDefaultGroup group, WorkflowDefaultEntryDTO entry) {
        assert group: "Parameter 'group' must not be null."
        assert entry.workflowVersion: "Parameter 'workflowVersion' must not be null."

        return new WorkflowVersionSelectorDefault(
                group: group,
                workflowVersion: entry.workflowVersion,
                referenceGenome: entry.referenceGenome,
                species: entry.species,
        ).save(flush: true)
    }

    @CompileDynamic
    void deleteEntry(WorkflowVersionSelectorDefault entry) {
        entry.delete(flush: true)
    }

    @CompileDynamic
    void delete(WorkflowDefaultGroup group) {
        findEntries(group).each { it.delete(flush: true) }
        group.delete(flush: true)
    }

    List<AppliedDefaultEntry> applyToProject(Project project, WorkflowDefaultGroup group) {
        return findEntries(group).collect { entry ->
            WorkflowVersionSelector wvSelector = workflowVersionSelectorService.createOrUpdate(project, group.seqType, entry.workflowVersion)
            ReferenceGenomeSelector rgSelector = entry.referenceGenome ? referenceGenomeSelectorService.createOrUpdate(
                    project, group.seqType, entry.species as List, entry.workflowVersion.workflow, entry.referenceGenome
            ) : null
            new AppliedDefaultEntry(wvSelector, rgSelector)
        }
    }
}

@TupleConstructor
class WorkflowDefaultEntryDTO {
    WorkflowVersion workflowVersion
    ReferenceGenome referenceGenome
    Set<SpeciesWithStrain> species
}

@TupleConstructor
class AppliedDefaultEntry {
    WorkflowVersionSelector workflowVersionSelector
    ReferenceGenomeSelector referenceGenomeSelector
}
