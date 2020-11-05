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
package de.dkfz.tbi.otp.tracking

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class OtrsTicketServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                OtrsTicket,
        ]
    }

    private final static String TICKET_NUMBER = "2000010112345678"

    @Unroll
    void 'createOrResetOtrsTicket, when no OtrsTicket with ticket number exists, creates one (comment: #comment)'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        when:
        OtrsTicket otrsTicket = otrsTicketService.createOrResetOtrsTicket(TICKET_NUMBER, comment, true)

        then:
        assertTicket(otrsTicket)
        otrsTicket.seqCenterComment == comment

        where:
        comment << [
                null,
                '',
                'Some Cooment',
                'Some\nMultiline\nComment',
        ]
    }

    void 'createOrResetOtrsTicket, when OtrsTicket with ticket number exists, resets it'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        createOtrsTicket([
                ticketNumber         : TICKET_NUMBER,
                installationFinished : new Date(),
                fastqcFinished       : new Date(),
                alignmentFinished    : new Date(),
                snvFinished          : new Date(),
                indelFinished        : new Date(),
                sophiaFinished       : new Date(),
                aceseqFinished       : new Date(),
                runYapsaFinished     : new Date(),
                finalNotificationSent: true,
                automaticNotification: true,
        ])

        when:
        OtrsTicket otrsTicket = otrsTicketService.createOrResetOtrsTicket(TICKET_NUMBER, null, true)

        then:
        assertTicket(otrsTicket)
    }

    @Unroll
    void 'createOrResetOtrsTicket, when OtrsTicket with ticket number exists, combine the seq center comment'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        createOtrsTicket([
                ticketNumber    : TICKET_NUMBER,
                seqCenterComment: comment1,
        ])

        when:
        OtrsTicket otrsTicket = otrsTicketService.createOrResetOtrsTicket(TICKET_NUMBER, comment2, true)

        then:
        resultComment == otrsTicket.seqCenterComment

        where:
        comment1     | comment2     || resultComment
        null         | null         || null
        'Something'  | null         || 'Something'
        null         | 'Something'  || 'Something'
        'Something'  | 'Something'  || 'Something'
        'Something1' | 'Something2' || 'Something1\n\nSomething2'
    }

    void 'createOrResetOtrsTicket, when ticket number is null, throws ValidationException'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        when:
        otrsTicketService.createOrResetOtrsTicket(null, null, true)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value [null]")
    }

    void 'createOrResetOtrsTicket, when ticket number is blank, throws ValidationException'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        when:
        otrsTicketService.createOrResetOtrsTicket("", null, true)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value []")
    }

    void 'resetAnalysisNotification, when OtrsTicket is reset, then final flag is false and finish date of analysis dates are null'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket = createOtrsTicketWithEndDatesAndNotificationSent([
                ticketNumber         : TICKET_NUMBER,
                automaticNotification: true,
        ])

        when:
        otrsTicketService.resetAnalysisNotification(otrsTicket)

        then:
        otrsTicket.snvFinished == null
        otrsTicket.indelFinished == null
        otrsTicket.sophiaFinished == null
        otrsTicket.aceseqFinished == null
        otrsTicket.runYapsaFinished == null
        otrsTicket.finalNotificationSent == false
    }

    private boolean assertTicket(OtrsTicket otrsTicket) {
        assert otrsTicket.ticketNumber == TICKET_NUMBER
        assert otrsTicket.installationFinished == null
        assert otrsTicket.fastqcFinished == null
        assert otrsTicket.alignmentFinished == null
        assert otrsTicket.snvFinished == null
        assert otrsTicket.indelFinished == null
        assert otrsTicket.aceseqFinished == null
        assert !otrsTicket.finalNotificationSent
        return true
    }
}
