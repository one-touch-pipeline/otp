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
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.acl.AclSid
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.CollectionUtils

class MergingCriteriaControllerSpec extends Specification implements ControllerUnitTest<MergingCriteriaController>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AclSid,
                MergingCriteria,
                Project,
                ProjectRole,
                Realm,
                Role,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqType,
                User,
                UserProjectRole,
                UserRole,
        ]
    }

    void setupData() {
        controller.mergingCriteriaService = new MergingCriteriaService()
        controller.mergingCriteriaService.commentService = Mock(CommentService)
    }

    void "test projectAndSeqTypeSpecific, no MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        controller.projectSelectionService = Mock(ProjectSelectionService) {
            getSelectedProject() >> project
        }
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."seqType.id" = seqType.id
        def model
        model = controller.projectAndSeqTypeSpecific()

        then:
        controller.response.status == 200
        model.seqType == seqType
        model.mergingCriteria instanceof MergingCriteria
        model.mergingCriteria.project == null
        model.mergingCriteria.seqType == null
    }

    void "test projectAndSeqTypeSpecific, MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        controller.projectSelectionService = Mock(ProjectSelectionService) {
            getSelectedProject() >> project
        }
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        controller.params."seqType.id" = seqType.id
        def model
        model = controller.projectAndSeqTypeSpecific()

        then:
        controller.response.status == 200
        model.seqType == seqType
        model.mergingCriteria == mergingCriteria
    }

    void "test projectAndSeqTypeSpecific"() {
        given:
        setupData()

        controller.projectSelectionService = Mock(ProjectSelectionService) {
            getSelectedProject() >> DomainFactory.createProject()
        }
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."seqType.id" = seqType.id
        controller.projectAndSeqTypeSpecific()

        then:
        controller.response.status == 200
    }

    void "test defaultSeqPlatformGroupConfiguration"() {
        given:
        setupData()

        when:
        controller.defaultSeqPlatformGroupConfiguration()

        then:
        controller.response.status == 200
    }

    void "test update, no MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        controller.projectSelectionService = Mock(ProjectSelectionService) {
            getRequestedProject() >> project
        }
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        controller.params."seqType.id" = seqType.id
        controller.params.useLibPrepKit = "on"
        controller.params.useSeqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        controller.update()

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${seqType.id}"
        MergingCriteria mergingCriteria = MergingCriteria.findByProjectAndSeqType(project, seqType)
        mergingCriteria
        mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test update, MergingCriteria exists"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        controller.projectSelectionService = Mock(ProjectSelectionService) {
            getRequestedProject() >> project
        }
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        when:
        controller.params."seqType.id" = seqType.id
        controller.params.useSeqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        controller.update()

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${seqType.id}"
        mergingCriteria
        !mergingCriteria.useLibPrepKit
        mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
    }

    void "test removePlatformFromSeqPlatformGroup, group contains many seqPlatforms"() {
        given:
        setupData()

        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform seqPlatform1 = DomainFactory.createSeqPlatform()
        SeqPlatform seqPlatform2 = DomainFactory.createSeqPlatform()
        group.addToSeqPlatforms(seqPlatform1)
        group.addToSeqPlatforms(seqPlatform2)
        group.save(flush: true)

        when:
        controller.params."group.id" = group.id
        controller.params."platform.id" = seqPlatform1.id
        controller.removePlatformFromSeqPlatformGroup()

        then:
        controller.response.status == 302
        controller.response.redirectedUrl ==
                "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${group.mergingCriteria.seqType.id}"
        !group.seqPlatforms.contains(seqPlatform1)
        group.seqPlatforms.contains(seqPlatform2)
    }

    void "test addPlatformToExistingSeqPlatformGroup"() {
        given:
        setupData()

        SeqPlatformGroup group = DomainFactory.createSeqPlatformGroupWithMergingCriteria()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        controller.params."group.id" = group.id
        controller.params."platform.id" = seqPlatform.id
        controller.addPlatformToExistingSeqPlatformGroup(group, seqPlatform)

        then:
        controller.response.status == 302
        controller.response.redirectedUrl ==
                "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${group.mergingCriteria.seqType.id}"
        group.seqPlatforms.contains(seqPlatform)
    }

    void "test addPlatformToNewGroup"() {
        given:
        setupData()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy(
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )

        when:
        controller.params."mergingCriteria.id" = mergingCriteria.id
        controller.params."platform.id" = seqPlatform.id
        controller.createNewSpecificGroupAndAddPlatform(seqPlatform, mergingCriteria)

        then:
        controller.response.status == 302
        controller.response.redirectedUrl ==
                "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${mergingCriteria.seqType.id}"
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
        SeqPlatformGroup.list().first().mergingCriteria == mergingCriteria
    }

    void "test createNewDefaultGroupAndAddPlatform"() {
        given:
        setupData()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform()

        when:
        controller.createNewDefaultGroupAndAddPlatform(seqPlatform)

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/defaultSeqPlatformGroupConfiguration"
        SeqPlatformGroup.list().size() == 1
        SeqPlatformGroup.list().first().seqPlatforms.contains(seqPlatform)
        !SeqPlatformGroup.list().first().mergingCriteria
    }

    void "test emptySeqPlatformGroup"() {
        given:
        setupData()

        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroupWithMergingCriteria()

        when:
        controller.params."group.id" = seqPlatformGroup.id
        controller.emptySeqPlatformGroup(seqPlatformGroup)

        then:
        controller.response.status == 302
        controller.response.redirectedUrl == "/mergingCriteria/projectAndSeqTypeSpecific?" +
                "seqType.id=${seqPlatformGroup.mergingCriteria.seqType.id}"
        SeqPlatformGroup.list().size() == 1
        !SeqPlatformGroup.list().first().seqPlatforms
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
        controller.params."seqPlatformGroup.id" = defaultGroup.id
        controller.params."mergingCriteria.id" = mergingCriteria.id
        controller.copyDefaultToSpecific(defaultGroup, mergingCriteria)

        then:
        controller.response.status == 302
        controller.response.redirectedUrl ==
                "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${mergingCriteria.seqType.id}"
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
        controller.copyAllDefaultToSpecific(mergingCriteria)

        then:
        controller.response.status == 302
        controller.response.redirectedUrl ==
                "/mergingCriteria/projectAndSeqTypeSpecific?seqType.id=${mergingCriteria.seqType.id}"
        SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria).size() == 2
        CollectionUtils.containSame(SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria)*.seqPlatforms, [[seqPlatform] as Set, [seqPlatform1] as Set])
    }
}
