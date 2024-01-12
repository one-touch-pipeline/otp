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

import de.dkfz.tbi.otp.ngsdata.*

class TicketSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MetaDataFile,
                Ticket,
                Run,
                FastqImportInstance,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
        ]
    }

    void 'test creation, correct String'() {
        given:
        Ticket ticket

        when:
        ticket = DomainFactory.createTicket([ticketNumber: '2000010112345678'])

        then:
        ticket.ticketNumber == '2000010112345678'
    }

    void 'test creation, incorrect String'() {
        when:
        DomainFactory.createTicket([ticketNumber: '20000101a2345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("does not match the required pattern")
    }

    void 'test creation, duplicate String'() {
        when:
        DomainFactory.createTicket([ticketNumber: '2000010112345678'])
        DomainFactory.createTicket([ticketNumber: '2000010112345678'])

        then:
        ValidationException ex = thrown()
        ex.message.contains("ticket.ticketNumber.unique.error")
    }
}
