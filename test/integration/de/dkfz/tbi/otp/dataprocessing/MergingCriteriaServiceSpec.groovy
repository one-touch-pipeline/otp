package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.*
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
        !mergingCriteria.libPrepKit
        mergingCriteria.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
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
        !mergingCriteria.libPrepKit
        mergingCriteria.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
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
}
