package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import spock.lang.*

class MergingCriteriaServiceSpec extends Specification implements UserAndRoles {
    MergingCriteriaService mergingCriteriaService = new MergingCriteriaService()

    def setup() {
        createUserAndRoles()
    }

    def "test findMergingCriteria, no MergingCriteria exists"() {
        given:
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

    def "test findMergingCriteria, MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        expect:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteria == mergingCriteriaService.findMergingCriteria(project, seqType)
        }
    }

    def "test createOrUpdateMergingCriteria, no MergingCriteria exists"() {
        given:
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

    def "test createOrUpdateMergingCriteria, MergingCriteria exists"() {
        given:
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

    def "test createOrUpdateMergingCriteria, no MergingCriteria exists, seqType Exome"() {
        given:
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


    def "test removePlatformFromSeqPlatformGroup, group contains many seqPlatforms"() {
        given:
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


    def "test removePlatformFromSeqPlatformGroup, group contains only one seqPlatform"() {
        given:
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


    def "test addPlatformToExistingSeqPlatformGroup"() {
        given:
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


    def "test createNewGroupAndAddPlatform, mergingCriteria is null"() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform, null)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
    }


    def "test createNewGroupAndAddPlatform, mergingCriteria exists"() {
        given:
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


    def "test deleteSeqPlatformGroup when no seqPlatform is left"() {
        given:
        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.deleteSeqPlatformGroup(group)
        }

        then:
        SeqPlatformGroup.list().contains(group)
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
        !group.comments.empty
    }


    def "test deleteSeqPlatformGroup when seqPlatformGroup still contains seqPlatforms"() {
        given:
        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        SeqPlatform seqPlatform2 = DomainFactory.createSeqPlatform()
        seqPlatformGroup.addToSeqPlatforms(seqPlatform1)
        seqPlatformGroup.addToSeqPlatforms(seqPlatform2)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.deleteSeqPlatformGroup(seqPlatformGroup)
        }

        then:
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
        SeqPlatform.list().size() == 2
        !seqPlatformGroup.comments.empty
    }

    def "test copyDefaultToSpecific"() {
        given:
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

    def "test copyAllDefaultToSpecific"() {
        given:
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

    def "test findSeqPlatformGroupsForProjectAndSeqType"() {
        given:
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

    def "test findDefaultSeqPlatformGroups"() {
        given:
        DomainFactory.createSeqPlatformGroup()
        DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform platform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        expect:
        platform.seqPlatformGroups == SpringSecurityUtils.doWithAuth(OPERATOR) {
            mergingCriteriaService.findDefaultSeqPlatformGroups()
        } as Set
    }
}
