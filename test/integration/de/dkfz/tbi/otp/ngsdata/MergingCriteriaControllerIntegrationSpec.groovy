package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.testing.*
import grails.plugin.springsecurity.*
import org.springframework.security.access.*
import spock.lang.*

class MergingCriteriaControllerIntegrationSpec extends Specification implements UserAndRoles {

    MergingCriteriaController controller = new MergingCriteriaController()

    def setup() {
        createUserAndRoles()
    }

    void "test index, no MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        def model
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            model = controller.index()
        }

        then:
        controller.response.status == 200
        model.project == project
        model.seqType == seqType
        model.mergingCriteria instanceof MergingCriteria
        model.mergingCriteria.project == null
        model.mergingCriteria.seqType == null
    }

    void "test index, MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        def model
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            model = controller.index()
        }

        then:
        controller.response.status == 200
        model.project == project
        model.seqType == seqType
        model.mergingCriteria == mergingCriteria
    }

    void "test index, fails with wrong authentication"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        SpringSecurityUtils.doWithAuth(USER) {
            controller.index()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test index, fails without authentication"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        doWithAnonymousAuth {
            controller.index()
        }

        then:
        thrown(AccessDeniedException)
    }


    void "test update, no MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        controller.params.libPrepKit = "on"
        controller.params.seqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.update()
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/projectConfig/index"
        MergingCriteria mergingCriteria = MergingCriteria.findByProjectAndSeqType(project, seqType)
        mergingCriteria
        mergingCriteria.libPrepKit
        mergingCriteria.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test update, MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        controller.params.seqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.update()
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/projectConfig/index"
        mergingCriteria
        !mergingCriteria.libPrepKit
        mergingCriteria.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test update, fails with wrong authentication"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        controller.params.seqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        SpringSecurityUtils.doWithAuth(USER) {
            controller.update()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test update, fails without authentication"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        controller.params.seqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        doWithAnonymousAuth {
            controller.update()
        }

        then:
        thrown(AccessDeniedException)
    }


}
