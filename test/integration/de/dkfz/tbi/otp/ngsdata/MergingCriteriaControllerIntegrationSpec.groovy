package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import org.springframework.security.access.*
import spock.lang.*

class MergingCriteriaControllerIntegrationSpec extends Specification implements UserAndRoles {

    MergingCriteriaController controller = new MergingCriteriaController()

    def setup() {
        createUserAndRoles()
    }

    void "test projectAndSeqTypeSpecific, no MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        def model
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            model = controller.projectAndSeqTypeSpecific()
        }

        then:
        controller.response.status == 200
        model.project == project
        model.seqType == seqType
        model.mergingCriteria instanceof MergingCriteria
        model.mergingCriteria.project == null
        model.mergingCriteria.seqType == null
    }

    void "test projectAndSeqTypeSpecific, MergingCriteria exists"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        def model
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            model = controller.projectAndSeqTypeSpecific()
        }

        then:
        controller.response.status == 200
        model.project == project
        model.seqType == seqType
        model.mergingCriteria == mergingCriteria
    }

    void "test projectAndSeqTypeSpecific, fails for user not in project"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        SpringSecurityUtils.doWithAuth(USER) {
            controller.projectAndSeqTypeSpecific()
        }

        then:
        thrown(AccessDeniedException)
    }

    @Unroll
    void "test projectAndSeqTypeSpecific, works users with authorization"() {
        given:
        Project project = DomainFactory.createProject()
        addUserToProject(USER, project)
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        SpringSecurityUtils.doWithAuth(username) {
            controller.projectAndSeqTypeSpecific()
        }

        then:
        controller.response.status == 200

        where:
        username   | _
        ADMIN      | _
        OPERATOR   | _
        USER       | _
    }

    void "test projectAndSeqTypeSpecific, fails without authentication"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."project.id" = project.id
        controller.params."seqType.id" = seqType.id
        doWithAnonymousAuth {
            controller.projectAndSeqTypeSpecific()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test defaultSeqPlatformGroupConfiguration, fails without authentication"() {
        when:
        doWithAnonymousAuth {
            controller.defaultSeqPlatformGroupConfiguration()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test defaultSeqPlatformGroupConfiguration, fails with wrong authentication"() {
        when:
        SpringSecurityUtils.doWithAuth(USER) {
            controller.defaultSeqPlatformGroupConfiguration()
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test defaultSeqPlatformGroupConfiguration, works for operator"() {
        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.defaultSeqPlatformGroupConfiguration()
        }

        then:
        controller.response.status == 200
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
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${project.id}&seqType.id=${seqType.id}"
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
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${project.id}&seqType.id=${seqType.id}"
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

    def "test removePlatformFromSeqPlatformGroup, group contains many seqPlatforms, not authorized"() {
        given:
        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform1)

        when:
        doWithAnonymousAuth {
            controller.removePlatformFromSeqPlatformGroup(group, seqPlatform1)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test removePlatformFromSeqPlatformGroup, group contains many seqPlatforms, authorized"() {
        given:
        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        SeqPlatform seqPlatform2 = DomainFactory.createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform1)
        group.addToSeqPlatforms(seqPlatform2)
        group.save(flush: true)

        when:
        controller.params."group.id" = group.id
        controller.params."platform.id" = seqPlatform1.id
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.removePlatformFromSeqPlatformGroup()
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${group.mergingCriteria.project.id}&seqType.id=${group.mergingCriteria.seqType.id}"
        !group.seqPlatforms.contains(seqPlatform1)
        group.seqPlatforms.contains(seqPlatform2)
    }

    def "test addPlatformToExistingSeqPlatformGroup, not authorized"() {
        given:
        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        doWithAnonymousAuth {
            controller.addPlatformToExistingSeqPlatformGroup(group, seqPlatform)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test addPlatformToExistingSeqPlatformGroup, authorized"() {
        given:
        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        controller.params."group.id" = group.id
        controller.params."platform.id" = seqPlatform.id
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.addPlatformToExistingSeqPlatformGroup(group, seqPlatform)
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${group.mergingCriteria.project.id}&seqType.id=${group.mergingCriteria.seqType.id}"
        group.seqPlatforms.contains(seqPlatform)
    }

    def "test addPlatformToNewGroup, not authorized"() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy()

        when:
        doWithAnonymousAuth {
            controller.createNewSpecificGroupAndAddPlatform(seqPlatform, mergingCriteria)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test addPlatformToNewGroup, authorized"() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        controller.params."mergingCriteria.id" = mergingCriteria.id
        controller.params."platform.id" = seqPlatform.id
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createNewSpecificGroupAndAddPlatform(seqPlatform, mergingCriteria)
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${mergingCriteria.project.id}&seqType.id=${mergingCriteria.seqType.id}"
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
        SeqPlatformGroup.list().first().mergingCriteria == mergingCriteria
    }

    def "test createNewDefaultGroupAndAddPlatform, not authorized"() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        doWithAnonymousAuth {
            controller.createNewDefaultGroupAndAddPlatform(seqPlatform)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test createNewDefaultGroupAndAddPlatform, authorized"() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createNewDefaultGroupAndAddPlatform(seqPlatform)
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/defaultSeqPlatformGroupConfiguration"
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
        !SeqPlatformGroup.list().first().mergingCriteria
    }

    def "test deleteSeqPlatformGroup, not authorized"() {
        given:
        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroup()

        when:
        doWithAnonymousAuth {
            controller.deleteSeqPlatformGroup(seqPlatformGroup)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test deleteSeqPlatformGroup, authorized"() {
        given:
        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroupWithMergingCriteria()

        when:
        controller.params."group.id" = seqPlatformGroup.id
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.deleteSeqPlatformGroup(seqPlatformGroup)
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${seqPlatformGroup.mergingCriteria.project.id}&seqType.id=${seqPlatformGroup.mergingCriteria.seqType.id}"
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
    }

    def "test copyDefaultToSpecific, not authorized"() {
        given:
        SeqPlatformGroup defaultGroup = DomainFactory.createSeqPlatformGroup()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            controller.copyDefaultToSpecific(defaultGroup, mergingCriteria)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test copyDefaultToSpecific, authorized"() {
        given:
        SeqPlatformGroup defaultGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        defaultGroup.addToSeqPlatforms(seqPlatform)
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        controller.params."seqPlatformGroup.id" = defaultGroup.id
        controller.params."mergingCriteria.id" = mergingCriteria.id
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.copyDefaultToSpecific(defaultGroup, mergingCriteria)
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${mergingCriteria.project.id}&seqType.id=${mergingCriteria.seqType.id}"
        SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria).size() == 1
        SeqPlatformGroup.findByMergingCriteria(mergingCriteria).seqPlatforms == [seqPlatform] as Set
    }

    def "test copyAllDefaultToSpecific, not authorized"() {
        given:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            controller.copyAllDefaultToSpecific(mergingCriteria)
        }

        then:
        thrown(AccessDeniedException)
    }

    def "test copyAllDefaultToSpecific, authorized"() {
        given:
        SeqPlatformGroup defaultGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        defaultGroup.addToSeqPlatforms(seqPlatform)
        SeqPlatformGroup defaultGroup1 = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        defaultGroup1.addToSeqPlatforms(seqPlatform1)
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.copyAllDefaultToSpecific(mergingCriteria)
        }

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?project.id=${mergingCriteria.project.id}&seqType.id=${mergingCriteria.seqType.id}"
        SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria).size() == 2
        CollectionUtils.containSame(SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria)*.seqPlatforms, [[seqPlatform] as Set, [seqPlatform1] as Set])
    }
}
