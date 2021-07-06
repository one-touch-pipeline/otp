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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class MergingCriteriaServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {
    MergingCriteriaService mergingCriteriaService = new MergingCriteriaService()

    void setupData() {
        createUserAndRoles()
    }

    void "test findMergingCriteria, no MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        MergingCriteria mergingCriteria = SpringSecurityUtils.doWithAuth(OPERATOR) {
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

        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        expect:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteria == mergingCriteriaService.findMergingCriteria(project, seqType)
        }
    }

    void "test createOrUpdateMergingCriteria, no MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        def errors = SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.createOrUpdateMergingCriteria(project, seqType, false, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        }

        then:
        !errors
        MergingCriteria mergingCriteria = MergingCriteria.findByProjectAndSeqType(project, seqType)
        mergingCriteria
        mergingCriteria.project == project
        mergingCriteria.seqType == seqType
        !mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test createOrUpdateMergingCriteria, MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        def errors = SpringSecurityUtils.doWithAuth(OPERATOR) {
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

        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createExomeSeqType()

        when:
        def errors = SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.createOrUpdateMergingCriteria(project, seqType, false, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        }

        then:
        errors
        !MergingCriteria.findByProjectAndSeqType(project, seqType)
    }

    void "test createDefaultMergingCriteria, creates a MergingCriteria"() {
        given:
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

    void "test removePlatformFromSeqPlatformGroup, group contains many seqPlatforms"() {
        given:
        setupData()

        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        SeqPlatform seqPlatform2 = DomainFactory.createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform1)
        group.addToSeqPlatforms(seqPlatform2)
        group.save(flush: true)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
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

        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform)
        group.save(flush: true)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
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

        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.addPlatformToExistingSeqPlatformGroup(group, seqPlatform)
        }

        then:
        group.seqPlatforms.contains(seqPlatform)
        !group.comments.empty
    }

    void "test createNewGroupAndAddPlatform, mergingCriteria is null"() {
        given:
        setupData()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform, null)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
    }

    void "test createNewGroupAndAddPlatform, mergingCriteria exists"() {
        given:
        setupData()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
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

        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
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

        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        SeqPlatform seqPlatform2 = DomainFactory.createSeqPlatform()
        seqPlatformGroup.addToSeqPlatforms(seqPlatform1)
        seqPlatformGroup.addToSeqPlatforms(seqPlatform2)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.emptySeqPlatformGroup(seqPlatformGroup)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
        SeqPlatform.list().size() == 2
        !seqPlatformGroup.comments.empty
    }

    void "test copyDefaultToSpecific"() {
        given:
        setupData()

        SeqPlatformGroup defaultGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        defaultGroup.addToSeqPlatforms(seqPlatform)
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.copyDefaultToSpecific(defaultGroup, mergingCriteria)
        }

        then:
        SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria).size() == 1
        SeqPlatformGroup.findByMergingCriteria(mergingCriteria).seqPlatforms == [seqPlatform] as Set
    }

    void "test copyAllDefaultToSpecific"() {
        given:
        setupData()

        SeqPlatformGroup defaultGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        defaultGroup.addToSeqPlatforms(seqPlatform)
        SeqPlatformGroup defaultGroup1 = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        defaultGroup1.addToSeqPlatforms(seqPlatform1)
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.copyAllDefaultToSpecific(mergingCriteria)
        }

        then:
        SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria).size() == 2
        CollectionUtils.containSame(SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria)*.seqPlatforms, [[seqPlatform] as Set, [seqPlatform1] as Set])
    }

    void "test findSeqPlatformGroupsForProjectAndSeqType"() {
        given:
        setupData()

        DomainFactory.createSeqPlatformGroup()
        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        seqPlatformGroup.addToSeqPlatforms(DomainFactory.createSeqPlatform())
        assert seqPlatformGroup.save(flush: true)
        DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        expect:
        [seqPlatformGroup] == SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(seqPlatformGroup.mergingCriteria.project, seqPlatformGroup.mergingCriteria.seqType)
        }
    }

    void "test findDefaultSeqPlatformGroups"() {
        given:
        setupData()

        DomainFactory.createSeqPlatformGroup()
        DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform platform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        expect:
        platform.seqPlatformGroups == SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.findDefaultSeqPlatformGroups()
        } as Set
    }
}
