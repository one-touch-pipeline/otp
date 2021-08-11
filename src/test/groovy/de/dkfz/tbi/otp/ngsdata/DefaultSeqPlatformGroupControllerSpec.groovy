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

import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.DefaultSeqPlatformGroupController

@SuppressWarnings(['GroovyImplicitNullArgumentCall', 'GroovyAssignabilityCheck'])
class DefaultSeqPlatformGroupControllerSpec extends Specification implements ControllerUnitTest<DefaultSeqPlatformGroupController>, DataTest,
        DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqPlatformGroup,
                SeqPlatform,
        ]
    }

    void setupData() {
        controller.mergingCriteriaService = Mock(MergingCriteriaService)
    }

    void "index, when no SeqPlatform and SeqPlatformGroup configured should respond with status 200"() {
        given:
        setupData()

        when:
        controller.index()

        then:
        1 * controller.mergingCriteriaService.findDefaultSeqPlatformGroupsOperator() >> _
        0 * controller.mergingCriteriaService

        and:
        response.status == 200
    }

    void "index, should respond with correct amount of SeqPlatformGroups and SeqPlatforms without group and with status 200"() {
        given:
        setupData()
        final SeqPlatform firstSeqPlatformWithOutGroup = createSeqPlatform()
        final SeqPlatform secondSeqPlatformWithOutGroup = createSeqPlatform()

        final SeqPlatform firstGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform firstGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup firstGroup = createSeqPlatformGroup(seqPlatforms: [firstGroupFirstSeqPlatform, firstGroupSecondSeqPlatform])

        final SeqPlatform secondGroupFirstSeqPlatform = createSeqPlatform()
        final SeqPlatform secondGroupSecondSeqPlatform = createSeqPlatform()
        final SeqPlatformGroup secondGroup = createSeqPlatformGroup(seqPlatforms: [secondGroupFirstSeqPlatform, secondGroupSecondSeqPlatform])

        final List<SeqPlatformGroup> expectedSeqPlatformGroups = [firstGroup, secondGroup]
        final List<SeqPlatform> expectedSeqPlatforms = [firstSeqPlatformWithOutGroup, secondSeqPlatformWithOutGroup]

        when:
        Object model = controller.index()

        then:
        1 * controller.mergingCriteriaService.findDefaultSeqPlatformGroupsOperator() >> expectedSeqPlatformGroups
        0 * controller.mergingCriteriaService

        and:
        response.status == 200
        model.seqPlatformGroups == expectedSeqPlatformGroups
        model.allSeqPlatformsWithoutGroup == expectedSeqPlatforms
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

    void "createNewGroupAndAddPlatform, should call mergingCriteriaService.createNewGroupAndAddPlatform and redirect to index"() {
        given:
        setupData()
        final SeqPlatform seqPlatform = createSeqPlatform()

        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        params['seqPlatform.id'] = seqPlatform.id
        controller.createNewGroupAndAddPlatform()

        then:
        1 * controller.mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform)
        0 * controller.mergingCriteriaService

        and:
        response.status == 302
        response.redirectedUrl.startsWith("/${controller.controllerName}/index")
    }
}
