/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class TicketServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Ticket,
                ProcessingOption,
        ]
    }

    private final static String TICKET_NUMBER = "2000010112345678"

    @Unroll
    void 'createOrResetTicket, when no ticket with ticket number exists, creates one (comment: #comment)'() {
        given:
        TicketService ticketService = new TicketService()

        when:
        Ticket ticket = ticketService.createOrResetTicket(TICKET_NUMBER, comment, true)

        then:
        assertTicket(ticket)
        ticket.seqCenterComment == comment

        where:
        comment << [
                null,
                '',
                'Some Cooment',
                'Some\nMultiline\nComment',
        ]
    }

    void 'createOrResetTicket, when ticket with ticket number exists, resets it'() {
        given:
        TicketService ticketService = new TicketService()
        createTicket([
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
        Ticket ticket = ticketService.createOrResetTicket(TICKET_NUMBER, null, true)

        then:
        assertTicket(ticket)
    }

    @Unroll
    void 'createOrResetTicket, when ticket with ticket number exists, combine the seq center comment'() {
        given:
        TicketService ticketService = new TicketService()
        createTicket([
                ticketNumber    : TICKET_NUMBER,
                seqCenterComment: comment1,
        ])

        when:
        Ticket ticket = ticketService.createOrResetTicket(TICKET_NUMBER, comment2, true)

        then:
        resultComment == ticket.seqCenterComment

        where:
        comment1     | comment2     || resultComment
        null         | null         || null
        'Something'  | null         || 'Something'
        null         | 'Something'  || 'Something'
        'Something'  | 'Something'  || 'Something'
        'Something1' | 'Something2' || 'Something1\n\nSomething2'
    }

    void 'createOrResetTicket, when ticket number is null, throws ValidationException'() {
        given:
        TicketService ticketService = new TicketService()

        when:
        ticketService.createOrResetTicket(null, null, true)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value [null]")
    }

    void 'createOrResetTicket, when ticket number is blank, throws ValidationException'() {
        given:
        TicketService ticketService = new TicketService()

        when:
        ticketService.createOrResetTicket("", null, true)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value []")
    }

    void 'resetAnalysisNotification, when ticket is reset, then final flag is false and finish date of analysis dates are null'() {
        given:
        TicketService ticketService = new TicketService()
        Ticket ticket = createTicketWithEndDatesAndNotificationSent([
                ticketNumber         : TICKET_NUMBER,
                automaticNotification: true,
        ])

        when:
        ticketService.resetAnalysisNotification(ticket)

        then:
        ticket.snvFinished == null
        ticket.indelFinished == null
        ticket.sophiaFinished == null
        ticket.aceseqFinished == null
        ticket.runYapsaFinished == null
        ticket.finalNotificationSent == false
    }

    @SuppressWarnings("GStringExpressionWithinString")
    void "buildTicketDirectLink, when build direct link then should return direct link with ticket number"() {
        given:
        TicketService ticketService = new TicketService()
        final String TICKET_SYSTEM_URL = "https://ticketsystem:8080/index.pl?Action=AgentTicketZoom;TicketNumber="
        final String TICKET_NUMBER = "\${ticketNumber}"
        Ticket ticket = createTicket()
        findOrCreateProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_URL, TICKET_SYSTEM_URL + TICKET_NUMBER)

        when:
        String directLink = ticketService.buildTicketDirectLink(ticket)

        then:
        directLink == "${TICKET_SYSTEM_URL}${ticket.ticketNumber}"
    }

    private boolean assertTicket(Ticket ticket) {
        assert ticket.ticketNumber == TICKET_NUMBER
        assert ticket.installationFinished == null
        assert ticket.fastqcFinished == null
        assert ticket.alignmentFinished == null
        assert ticket.snvFinished == null
        assert ticket.indelFinished == null
        assert ticket.aceseqFinished == null
        assert !ticket.finalNotificationSent
        return true
    }
}
