/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.validation.Errors
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow

@Rollback
@Integration
class MergingCriteriaServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, WorkflowSystemDomainFactory {
    MergingCriteriaService mergingCriteriaService = new MergingCriteriaService()

    void setupData() {
        createUserAndRoles()
        createWorkflow(name: WgbsWorkflow.WORKFLOW)
    }

    void "test findMergingCriteria, no MergingCriteria exists"() {
        given:
        setupData()

        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        MergingCriteria mergingCriteria = doWithAuth(OPERATOR) {
            mergingCriteriaService.findMergingCriteria(project, seqType)
        }

        then:
        mergingCriteria.id == null
        mergingCriteria.project == null
        mergingCriteria.seqType == null
    }

    void "test findMergingCriteria, MergingCriteria exists"() {
        given:
        setupData()

        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = createMergingCriteriaLazy(project: project, seqType: seqType)

        expect:
        mergingCriteria == doWithAuth(OPERATOR) {
            mergingCriteriaService.findMergingCriteria(project, seqType)
        }
    }

    void "test createOrUpdateMergingCriteria, no MergingCriteria exists"() {
        given:
        setupData()

        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        Errors errors = doWithAuth(OPERATOR) {
            mergingCriteriaService.createOrUpdateMergingCriteria(project, seqType, false, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        }

        then:
        !errors
        MergingCriteria mergingCriteria = CollectionUtils.atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
        mergingCriteria
        mergingCriteria.project == project
        mergingCriteria.seqType == seqType
        !mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test createOrUpdateMergingCriteria, MergingCriteria exists"() {
        given:
        setupData()

        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        Errors errors = doWithAuth(OPERATOR) {
            mergingCriteriaService.createOrUpdateMergingCriteria(project, seqType, false, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        }

        then:
        !errors
        mergingCriteria
        mergingCriteria.project == project
        mergingCriteria.seqType == seqType
        !mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test createOrUpdateMergingCriteria, no MergingCriteria exists, seqType Exome"() {
        given:
        setupData()

        Project project = createProject()
        SeqType seqType = DomainFactory.createExomeSeqType()

        when:
        Errors errors = doWithAuth(OPERATOR) {
            mergingCriteriaService.createOrUpdateMergingCriteria(project, seqType, false, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        }

        then:
        errors
        !MergingCriteria.findAllByProjectAndSeqType(project, seqType)
    }

    void "test createOrUpdateMergingCriteria, for sequencing platform group"() {
        given:
        setupData()

        MergingCriteria mc = createMergingCriteria()

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.updateMergingCriteria(mc, spg)
        }

        then:
        mc.useSeqPlatformGroup == spg

        where:
        spg << MergingCriteria.SpecificSeqPlatformGroups.values()
    }

    void "test createOrUpdateMergingCriteria, for library preparation kit"() {
        given:
        setupData()

        MergingCriteria mc = createMergingCriteria()

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.updateMergingCriteria(mc, lpk)
        }

        then:
        mc.useLibPrepKit == lpk

        where:
        lpk << [true, false]
    }

    void "test createDefaultMergingCriteria, creates a MergingCriteria"() {
        given:
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        mergingCriteriaService.createDefaultMergingCriteria(project, seqType)

        then:
        MergingCriteria.findAllByProjectAndSeqType(project, seqType).size() == 1
    }

    void "test createDefaultMergingCriteria, MergingCriteria already exists"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        createMergingCriteria(project: project, seqType: seqType, useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)

        when:
        mergingCriteriaService.createDefaultMergingCriteria(project, seqType)

        then:
        MergingCriteria.all.size() == 1
        MergingCriteria.all.first().useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
    }

    void "test createDefaultMergingCriteria, sequencing type cannot be aligned"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        Project project = createProject()
        SeqType seqType = createSeqType()

        when:
        mergingCriteriaService.createDefaultMergingCriteria(project, seqType)

        then:
        MergingCriteria.all.empty
    }

    void "test createDefaultMergingCriteria for seq. type, creates a MergingCriteria"() {
        given:
        setupData()
        createWorkflowVersion([supportedSeqTypes: DomainFactory.createAllAlignableSeqTypes()])
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        mergingCriteriaService.createDefaultMergingCriteria(seqType)

        then:
        MergingCriteria mergingCriteria = CollectionUtils.exactlyOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
        mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT
    }

    void "test createDefaultMergingCriteria for seq. type, MergingCriteria already exists, is not changed"() {
        given:
        setupData()
        createWorkflow(defaultSeqTypesForWorkflowVersions: DomainFactory.createAllAlignableSeqTypes())
        Project project = createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        createMergingCriteria(project: project, seqType: seqType, useLibPrepKit: false, useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)

        when:
        mergingCriteriaService.createDefaultMergingCriteria(seqType)

        then:
        MergingCriteria mergingCriteria = CollectionUtils.exactlyOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
        !mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
    }

    void "test createDefaultMergingCriteria for seq. type, sequencing type cannot be aligned"() {
        given:
        createWorkflow(defaultSeqTypesForWorkflowVersions: DomainFactory.createAllAlignableSeqTypes())
        createProject()
        SeqType seqType = createSeqType()

        when:
        mergingCriteriaService.createDefaultMergingCriteria(seqType)

        then:
        MergingCriteria.all.empty
    }

    void "test createDefaultMergingCriteria for project, creates a MergingCriteria"() {
        given:
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        createWorkflowVersion([supportedSeqTypes: [seqType]])
        Project project = createProject()

        when:
        mergingCriteriaService.createDefaultMergingCriteria(project)

        then:
        MergingCriteria mergingCriteria = CollectionUtils.exactlyOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
        mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT
    }

    void "test createDefaultMergingCriteria for project, MergingCriteria already exists, is not changed"() {
        given:
        setupData()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        createWorkflow(defaultSeqTypesForWorkflowVersions: [seqType])
        Project project = createProject()

        createMergingCriteria(project: project, seqType: seqType, useLibPrepKit: false, useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)

        when:
        mergingCriteriaService.createDefaultMergingCriteria(project)

        then:
        MergingCriteria mergingCriteria = CollectionUtils.exactlyOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
        !mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
    }

    void "test createDefaultMergingCriteria for project, sequencing type cannot be aligned"() {
        given:
        DomainFactory.createWholeGenomeSeqType()
        Project project = createProject()

        when:
        mergingCriteriaService.createDefaultMergingCriteria(project)

        then:
        MergingCriteria.all.empty
    }

    void "test removePlatformFromSeqPlatformGroup, group contains many seqPlatforms"() {
        given:
        setupData()

        SeqPlatformGroup group = createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform1)
        group.addToSeqPlatforms(seqPlatform2)
        group.save(flush: true)

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.removePlatformFromSeqPlatformGroup(group, seqPlatform1)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first() == group
        group.seqPlatforms.contains(seqPlatform2)
        !group.comments.empty
    }

    void "test removePlatformFromSeqPlatformGroup, group contains only one seqPlatform"() {
        given:
        setupData()

        SeqPlatformGroup group = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform)
        group.save(flush: true)

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.removePlatformFromSeqPlatformGroup(group, seqPlatform)
        }

        then:
        SeqPlatform.list().contains(seqPlatform)
        SeqPlatformGroup.list().contains(group)
        SeqPlatformGroup.list().first().seqPlatforms.empty
        !group.comments.empty
    }

    void "test addPlatformToExistingSeqPlatformGroup"() {
        given:
        setupData()

        SeqPlatformGroup group = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatform()

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.addPlatformToExistingSeqPlatformGroup(group, seqPlatform)
        }

        then:
        group.seqPlatforms.contains(seqPlatform)
        !group.comments.empty
    }

    void "test createNewGroupAndAddPlatform, mergingCriteria is null"() {
        given:
        setupData()

        SeqPlatform seqPlatform = createSeqPlatform()

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform, null)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
    }

    void "test createNewGroupAndAddPlatform, mergingCriteria exists"() {
        given:
        setupData()

        SeqPlatform seqPlatform = createSeqPlatform()
        MergingCriteria mergingCriteria = createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform, mergingCriteria)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
        SeqPlatformGroup.list().first().mergingCriteria == mergingCriteria
    }

    void "test emptySeqPlatformGroup when no seqPlatform is left"() {
        given:
        setupData()

        SeqPlatformGroup group = createSeqPlatformGroup()

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.emptySeqPlatformGroup(group)
        }

        then:
        SeqPlatformGroup.list().contains(group)
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
        !group.comments.empty
    }

    void "test emptySeqPlatformGroup when seqPlatformGroup still contains seqPlatforms"() {
        given:
        setupData()

        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()
        seqPlatformGroup.addToSeqPlatforms(seqPlatform1)
        seqPlatformGroup.addToSeqPlatforms(seqPlatform2)

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.emptySeqPlatformGroup(seqPlatformGroup)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
        SeqPlatform.list().size() == 2
        !seqPlatformGroup.comments.empty
    }

    void "test emptyAllSeqPlatformGroups"() {
        given:
        setupData()
        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup firstGroup = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])

        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup secondGroup = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform])

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.emptyAllSeqPlatformGroups([firstGroup, secondGroup])
        }

        then:
        SeqPlatformGroup.list().size() == 2
        SeqPlatformGroup.list().every { !it.seqPlatforms }
        SeqPlatform.list().size() == 4
        SeqPlatformGroup.list().every { !it.comments.empty }
    }

    void "test copySeqPlatformGroup"() {
        given:
        setupData()

        SeqPlatformGroup defaultGroup = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatform()
        defaultGroup.addToSeqPlatforms(seqPlatform)
        MergingCriteria mergingCriteria = createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.copySeqPlatformGroup(defaultGroup, mergingCriteria)
        }

        then:
        SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria).size() == 1
        CollectionUtils.atMostOneElement(SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria)).seqPlatforms == [seqPlatform] as Set
    }

    void "test copySeqPlatformGroups"() {
        given:
        setupData()
        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup firstGroup = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])

        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup secondGroup = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform])

        final MergingCriteria mergingCriteria = createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        doWithAuth(OPERATOR) {
            mergingCriteriaService.copySeqPlatformGroups([firstGroup, secondGroup], mergingCriteria)
        }

        then:
        final List<SeqPlatformGroup> groups = SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria)
        groups.size() == 2
        TestCase.assertContainSame(groups*.seqPlatforms.flatten(), [
                firstGroupFirstSeqPlatform,
                firstGroupSecondSeqPlatform,
                secondGroupFirstSeqPlatform,
                secondGroupSecondSeqPlatform,
        ])
    }

    void "test findSeqPlatformGroupsForProjectAndSeqType"() {
        given:
        setupData()

        createSeqPlatformGroup()
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroupWithMergingCriteria()
        seqPlatformGroup.addToSeqPlatforms(createSeqPlatform())
        assert seqPlatformGroup.save(flush: true)
        createSeqPlatformWithSeqPlatformGroup()

        expect:
        [seqPlatformGroup] == doWithAuth(OPERATOR) {
            mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(seqPlatformGroup.mergingCriteria.project, seqPlatformGroup.mergingCriteria.seqType)
        }
    }

    void "test findDefaultSeqPlatformGroups"() {
        given:
        setupData()

        createSeqPlatformGroup()
        createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform platform = createSeqPlatformWithSeqPlatformGroup()

        expect:
        TestCase.assertContainSame(platform.seqPlatformGroups, doWithAuth(OPERATOR) {
            mergingCriteriaService.findDefaultSeqPlatformGroups()
        })
    }
}
