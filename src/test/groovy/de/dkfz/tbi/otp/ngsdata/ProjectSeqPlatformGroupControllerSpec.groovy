/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.ConfigureMergingCriteriaBaseCommand
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.ProjectSeqPlatformGroupController
import de.dkfz.tbi.otp.project.Project

@SuppressWarnings(['GroovyImplicitNullArgumentCall', 'GroovyAssignabilityCheck',])
class ProjectSeqPlatformGroupControllerSpec extends Specification implements ControllerUnitTest<ProjectSeqPlatformGroupController>, DataTest,
        DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqPlatformGroup,
                SeqPlatform,
                MergingCriteria,
                Project,
                SeqType,
        ]
    }

    List<SeqType> availableSeqTypes

    void setupData() {
        availableSeqTypes = DomainFactory.createAllAlignableSeqTypes()
        GroovySpy(SeqTypeService, global: true) {
            _ * SeqTypeService.allAlignableSeqTypes >> availableSeqTypes
        }
        controller.mergingCriteriaService = Mock(MergingCriteriaService)
        controller.projectSelectionService = Mock(ProjectSelectionService)
    }

    void "index, when MergingCriteria is project specific it should respond with status 200"() {
        given:
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()
        final MergingCriteria mergingCriteria = createMergingCriteria(
                [
                        project            : project,
                        seqType            : seqType,
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                ]
        )

        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup defaultSeqPlatformGroup = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])

        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup projectSeqPlatformGroup = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform],
                mergingCriteria: mergingCriteria)

        final SeqPlatform firstSeqPlatformWithOutGroup = createSeqPlatform()
        final SeqPlatform secondSeqPlatformWithOutGroup = createSeqPlatform()

        final List<SeqPlatformGroup> expectedFoundSeqPlatformGroups = [defaultSeqPlatformGroup]
        final List<SeqPlatformGroup> expectedProjectSeqPlatformGroups = [projectSeqPlatformGroup]
        final List<SeqPlatform> expectedSeqPlatformsWithoutGroup = [
                firstSeqPlatformWithOutGroup,
                secondSeqPlatformWithOutGroup,
                firstGroupFirstSeqPlatform,
                firstGroupSecondSeqPlatform,
        ]
        final List<SeqPlatform> expectedUsedSpecificSeqPlatforms = expectedProjectSeqPlatformGroups*.seqPlatforms?.flatten()
        final boolean expectedCopyingAllAllowed = expectedFoundSeqPlatformGroups*.seqPlatforms?.flatten()?.intersect(expectedUsedSpecificSeqPlatforms) as Boolean

        when:
        params['seqType.id'] = seqType.id
        Object model = controller.index()

        then:
        1 * controller.projectSelectionService.selectedProject >> project
        0 * controller.projectSelectionService.selectedProject
        1 * controller.mergingCriteriaService.findMergingCriteria(project, seqType) >> mergingCriteria
        1 * controller.mergingCriteriaService.findDefaultSeqPlatformGroups() >> expectedFoundSeqPlatformGroups
        1 * controller.mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(project, seqType, true) >> expectedProjectSeqPlatformGroups
        0 * controller.mergingCriteriaService

        and:
        response.status == 200
        model.mergingCriteria == mergingCriteria
        model.seqType == seqType
        model.seqPlatformGroups == expectedFoundSeqPlatformGroups
        model.seqPlatformGroupsPerProjectAndSeqType == expectedProjectSeqPlatformGroups
        model.allSeqPlatformsWithoutGroup == expectedSeqPlatformsWithoutGroup.sort { it.toString() }
        model.allUsedSpecificSeqPlatforms == expectedUsedSpecificSeqPlatforms
        model.dontAllowCopyingAll == expectedCopyingAllAllowed
        model.availableSeqTypes == availableSeqTypes
        model.selectedProjectToCopyForm == null
        model.selectedSeqTypeToCopyFrom == null
    }

    void "index, when searched for template in other projects it should respond with the search results and with status 200"() {
        given:
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()

        final MergingCriteria mergingCriteria = createMergingCriteria(
                [
                        project            : project,
                        seqType            : seqType,
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                ]
        )
        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup projectSeqPlatformGroup = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform],
                mergingCriteria: mergingCriteria)

        final Project expectedProjectToCopyForm = createProject()
        final SeqType expectedSeqTypeToCopyFrom = availableSeqTypes.last()
        final MergingCriteria templateMergingCriteria = createMergingCriteria(
                [
                        project            : expectedProjectToCopyForm,
                        seqType            : expectedSeqTypeToCopyFrom,
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                ]
        )
        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup foundSeqPlatformGroup = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform],
                mergingCriteria: templateMergingCriteria)

        final SeqPlatform firstSeqPlatformWithOutGroup = createSeqPlatform()
        final SeqPlatform secondSeqPlatformWithOutGroup = createSeqPlatform()

        final List<SeqPlatformGroup> expectedFoundSeqPlatformGroups = [foundSeqPlatformGroup]
        final List<SeqPlatformGroup> expectedProjectSeqPlatformGroups = [projectSeqPlatformGroup]
        final List<SeqPlatform> expectedSeqPlatformsWithoutGroup = [
                firstSeqPlatformWithOutGroup,
                secondSeqPlatformWithOutGroup,
                secondGroupFirstSeqPlatform,
                secondGroupSecondSeqPlatform,
        ]
        final List<SeqPlatform> expectedUsedSpecificSeqPlatforms = expectedProjectSeqPlatformGroups*.seqPlatforms?.flatten()
        final boolean expectedCopyingAllAllowed = expectedFoundSeqPlatformGroups*.seqPlatforms?.flatten()?.intersect(expectedUsedSpecificSeqPlatforms) as Boolean

        when:
        params['seqType.id'] = seqType.id
        params['selectedProjectToCopyForm'] = expectedProjectToCopyForm.id
        params['selectedSeqTypeToCopyFrom'] = expectedSeqTypeToCopyFrom.id
        Object model = controller.index()

        then:
        1 * controller.projectSelectionService.selectedProject >> project
        0 * controller.projectSelectionService.selectedProject
        1 * controller.mergingCriteriaService.findMergingCriteria(project, seqType) >> mergingCriteria
        1 * controller.mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(expectedProjectToCopyForm,
                expectedSeqTypeToCopyFrom, true) >> expectedFoundSeqPlatformGroups
        1 * controller.mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(project, seqType, true) >> expectedProjectSeqPlatformGroups
        0 * controller.mergingCriteriaService

        and:
        response.status == 200
        model.mergingCriteria == mergingCriteria
        model.seqType == seqType
        model.seqPlatformGroups == expectedFoundSeqPlatformGroups
        model.seqPlatformGroupsPerProjectAndSeqType == expectedProjectSeqPlatformGroups
        model.allSeqPlatformsWithoutGroup == expectedSeqPlatformsWithoutGroup.sort { it.toString() }
        model.allUsedSpecificSeqPlatforms == expectedUsedSpecificSeqPlatforms
        model.dontAllowCopyingAll == expectedCopyingAllAllowed
        model.availableSeqTypes == availableSeqTypes
        model.selectedProjectToCopyForm == expectedProjectToCopyForm
        model.selectedSeqTypeToCopyFrom == expectedSeqTypeToCopyFrom
    }

    void "index, when default seqPlatforms are used it should respond with status 200"() {
        given:
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()

        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup defaultSeqPlatformGroup = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])
        final MergingCriteria mergingCriteria = createMergingCriteria(
                [
                        project            : project,
                        seqType            : seqType,
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
                ]
        )

        final List<SeqPlatformGroup> expectedProjectSeqPlatformGroups = [defaultSeqPlatformGroup]

        when:
        params['seqType.id'] = seqType.id
        Object model = controller.index()

        then:
        1 * controller.projectSelectionService.selectedProject >> project
        0 * controller.projectSelectionService.selectedProject
        1 * controller.mergingCriteriaService.findMergingCriteria(project, seqType) >> mergingCriteria
        1 * controller.mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(project, seqType, true) >> expectedProjectSeqPlatformGroups
        0 * controller.mergingCriteriaService

        and:
        response.status == 200
        model.mergingCriteria == mergingCriteria
        model.seqType == seqType
        model.seqPlatformGroupsPerProjectAndSeqType == expectedProjectSeqPlatformGroups
    }

    void "index, when ignore for merging is set it should respond with status 200"() {
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()

        final MergingCriteria mergingCriteria = createMergingCriteria(
                [
                        project            : project,
                        seqType            : seqType,
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING,
                ]
        )

        when:
        params['seqType.id'] = seqType.id
        Object model = controller.index()

        then:
        1 * controller.projectSelectionService.selectedProject >> project
        0 * controller.projectSelectionService.selectedProject
        1 * controller.mergingCriteriaService.findMergingCriteria(project, seqType) >> mergingCriteria
        1 * controller.mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(project, seqType, true) >> _
        0 * controller.mergingCriteriaService

        and:
        response.status == 200
        model.mergingCriteria == mergingCriteria
        model.seqType == seqType
        model.seqPlatformGroupsPerProjectAndSeqType == null
    }

    void "update, when mergingCriteriaService.createOrUpdateMergingCriteria does not return errors it should redirect to index"() {
        given:
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()
        final boolean useLibPrepKit = true
        final MergingCriteria.SpecificSeqPlatformGroups useSeqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqType.id'] = seqType.id
        params['useLibPrepKit'] = useLibPrepKit
        params['useSeqPlatformGroup'] = useSeqPlatformGroup
        controller.update()

        then:
        1 * controller.projectSelectionService.requestedProject >> project
        1 * controller.mergingCriteriaService.createOrUpdateMergingCriteria(
                project, seqType, useLibPrepKit, useSeqPlatformGroup
        ) >> _

        and:
        response.status == 302
        flash.message.message == "Data stored successfully"
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "update, when mergingCriteriaService.createOrUpdateMergingCriteria return errors it should redirect to index and show flash message"() {
        given:
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()
        final boolean useLibPrepKit = true
        final MergingCriteria.SpecificSeqPlatformGroups useSeqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT
        final MergingCriteria mergingCriteria = createMergingCriteria([project: project, seqType: seqType])

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqType.id'] = seqType.id
        params['useLibPrepKit'] = useLibPrepKit
        params['useSeqPlatformGroup'] = useSeqPlatformGroup
        controller.update()

        then:
        1 * controller.projectSelectionService.requestedProject >> project
        1 * controller.mergingCriteriaService.createOrUpdateMergingCriteria(project, seqType, useLibPrepKit, useSeqPlatformGroup) >> {
            mergingCriteria.errors.reject("some error")
            return mergingCriteria.errors
        }

        and:
        response.status == 302
        flash.message.message == "An error occurred"
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "removePlatformFromSeqPlatformGroup, should call mergingCriteriaService.removePlatformFromSeqPlatformGroup and redirect to index"() {
        given:
        setupData()
        final SeqPlatform firstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group = createSeqPlatformGroup(seqPlatforms: [firstSeqPlatform, secondSeqPlatform])

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqPlatformGroup.id'] = group.id
        params['seqPlatform.id'] = secondSeqPlatform.id
        controller.removePlatformFromSeqPlatformGroup()

        then:
        1 * controller.mergingCriteriaService.removePlatformFromSeqPlatformGroup(group, secondSeqPlatform)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "addPlatformToExistingSeqPlatformGroup, should call mergingCriteriaService.addPlatformToExistingSeqPlatformGroup and redirect to index"() {
        given:
        setupData()
        final SeqPlatform firstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group = createSeqPlatformGroup(seqPlatforms: [firstSeqPlatform])

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqPlatformGroup.id'] = group.id
        params['seqPlatform.id'] = secondSeqPlatform.id
        controller.addPlatformToExistingSeqPlatformGroup()

        then:
        1 * controller.mergingCriteriaService.addPlatformToExistingSeqPlatformGroup(group, secondSeqPlatform)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "emptySeqPlatformGroup, should call mergingCriteriaService.emptySeqPlatformGroup and redirect to index"() {
        given:
        setupData()
        final SeqPlatform firstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group = createSeqPlatformGroup(seqPlatforms: [firstSeqPlatform, secondSeqPlatform])

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqPlatformGroup.id'] = group.id
        controller.emptySeqPlatformGroup()

        then:
        1 * controller.mergingCriteriaService.emptySeqPlatformGroup(group)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "emptyAllSeqPlatformGroups, should call mergingCriteriaService.emptyAllSeqPlatformGroups and redirect to index"() {
        given:
        setupData()
        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group1 = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])

        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group2 = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform])
        final List<SeqPlatformGroup> groupList = [group1, group2]

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        groupList.eachWithIndex { it, i ->
            params["seqPlatformGroupList[${i}]"] = it.id
        }
        controller.emptyAllSeqPlatformGroups()

        then:
        1 * controller.mergingCriteriaService.emptyAllSeqPlatformGroups(groupList)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "createNewGroupAndAddPlatform, should call mergingCriteriaService.createNewGroupAndAddPlatform and redirect to index"() {
        given:
        setupData()
        final Project project = createProject()
        final SeqType seqType = availableSeqTypes.first()
        final MergingCriteria mergingCriteria = createMergingCriteria([project: project, seqType: seqType])
        final SeqPlatform seqPlatform = createSeqPlatform()

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqPlatform.id'] = seqPlatform.id
        params['mergingCriteria.id'] = mergingCriteria.id
        controller.createNewGroupAndAddPlatform()

        then:
        1 * controller.mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform, mergingCriteria)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "copySeqPlatformGroup, should call mergingCriteriaService.copySeqPlatformGroup and redirect to index"() {
        given:
        setupData()
        final SeqPlatform firstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group = createSeqPlatformGroup(seqPlatforms: [firstSeqPlatform, secondSeqPlatform])
        final MergingCriteria mergingCriteria = createMergingCriteria()

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqPlatformGroup.id'] = group.id
        params['mergingCriteria.id'] = mergingCriteria.id
        controller.copySeqPlatformGroup()

        then:
        1 * controller.mergingCriteriaService.copySeqPlatformGroup(group, mergingCriteria)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "copyAllSeqPlatformGroups, should call mergingCriteriaService.copySeqPlatformGroups and redirect to index"() {
        given:
        setupData()
        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group1 = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])

        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup group2 = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform])
        final List<SeqPlatformGroup> groupList = [group1, group2]
        final MergingCriteria mergingCriteria = createMergingCriteria()

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        groupList.eachWithIndex { it, i ->
            params["seqPlatformGroupList[${i}]"] = it.id
        }
        params['mergingCriteria.id'] = mergingCriteria.id
        controller.copyAllSeqPlatformGroups()

        then:
        1 * controller.mergingCriteriaService.copySeqPlatformGroups(groupList, mergingCriteria)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    void "searchForSeqPlatformGroups, should redirect to index"() {
        given:
        setupData()
        final SeqType seqType = availableSeqTypes.first()
        final Project selectedProjectToCopyForm = createProject()
        final SeqType selectedSeqTypeToCopyFrom = availableSeqTypes.last()

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqType.id'] = seqType.id
        params['selectedProjectToCopyForm.id'] = selectedProjectToCopyForm.id
        params['selectedSeqTypeToCopyFrom.id'] = selectedSeqTypeToCopyFrom.id
        controller.searchForSeqPlatformGroups()

        then:
        response.status == 302
        !flash.massage
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }

    @Unroll
    void "redirectToIndex, it should redirect to index"() {
        given:
        setupData()
        final SeqType seqType = availableSeqTypes.first()
        when:
        ConfigureMergingCriteriaBaseCommand cmd = new ConfigureMergingCriteriaBaseCommand(
                seqType: seqType,
                selectedProjectToCopyForm: selectedProjectToCopyForm,
                selectedSeqTypeToCopyFrom: selectedSeqTypeToCopyFrom
        )
        controller.redirectToIndex(cmd)

        then:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index" +
                "?seqType.id=${seqType?.id}&" + "selectedProjectToCopyForm.id=${selectedProjectToCopyForm ? selectedProjectToCopyForm.id : ""}" +
                "&selectedSeqTypeToCopyFrom.id=${selectedSeqTypeToCopyFrom ? selectedSeqTypeToCopyFrom.id : ""}")

        where:
        selectedProjectToCopyForm | selectedSeqTypeToCopyFrom
        createProject()           | createSeqType()
        null                      | null
    }
}
