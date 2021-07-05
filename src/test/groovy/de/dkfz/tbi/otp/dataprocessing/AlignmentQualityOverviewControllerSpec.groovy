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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.HttpStatus
import spock.lang.Specification

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.tracking.OtrsTicketService

class AlignmentQualityOverviewControllerSpec extends Specification implements ControllerUnitTest<AlignmentQualityOverviewController> {

    AbstractFileSystemBamFile bamFile = new RoddyBamFile()

    final String commentUsed      = 'some comments'
    final String qcAcceptedStatus = AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED

    void setupData() {
        bamFile.id                   = 123
        bamFile.version              = 0
        bamFile.qcTrafficLightStatus = AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
    }

    void "Test changeQcStatus, when save valid input posted, then return HTTP OK"() {
        given:
        setupData()

        controller.qcTrafficLightService = Mock(QcTrafficLightService) {
            1 * setQcTrafficLightStatusWithComment(_, _, _) >> { AbstractMergedBamFile bamFile,
                                                                 AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus,
                                                                 String comment ->
                assert bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
                assert qcTrafficLightStatus         == AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
                assert comment == commentUsed
            }
        }

        when:
        controller.request.method = 'POST'
        controller.params.comment = commentUsed
        controller.params.dbVersion = 0
        controller.params.abstractBamFile = bamFile
        controller.params.newValue = qcAcceptedStatus

        controller.changeQcStatus()

        then: 'HTTP OK code is returned'
        response.status == HttpStatus.OK.value() //200

        and: 'BAM file status is updated'
    }

    void "Test changeQcStatus, when two users modify the same bam file status, then modification not allowed, an error shown"() {
        given:
        setupData()

        //someone has changed the content: db version incremented by 1
        bamFile.version = 1

        controller.qcTrafficLightService = Mock(QcTrafficLightService)
        controller.qcTrafficLightService.commentService = Mock(CommentService) {
            0 * saveComment(_, _)
        }
        controller.qcTrafficLightService.otrsTicketService = Mock(OtrsTicketService) {
            0 * findAllOtrsTickets(_)
        }

        when:
        controller.request.method = 'POST'
        controller.params.comment = commentUsed
        controller.params.dbVersion = 0
        controller.params.abstractBamFile = bamFile
        controller.params.newValue = qcAcceptedStatus

        controller.changeQcStatus()

        then:
        response.status == HttpStatus.CONFLICT.value() //409
    }
}
