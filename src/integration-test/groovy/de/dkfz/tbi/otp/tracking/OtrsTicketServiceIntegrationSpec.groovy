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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.RunSegment
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.user.UserException

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

@Rollback
@Integration
class OtrsTicketServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    @Unroll
    void 'saveStartTimeIfNeeded, when start time for #step is not set yet, then set value'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket = createOtrsTicket()

        when:
        otrsTicketService.saveStartTimeIfNeeded(otrsTicket, step)

        then:
        otrsTicket["${step}Started"]

        where:
        step << OtrsTicket.ProcessingStep.values()
    }

    @Unroll
    void 'saveStartTimeIfNeeded, when start time for #step is already set, then do not change the value'() {
        given:
        Date date = new Date(2000 - 1900, 2, 2)
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket = createOtrsTicket([
                ("${step}Started".toString()): date
        ])

        when:
        otrsTicketService.saveStartTimeIfNeeded(otrsTicket, step)

        then:
        otrsTicket["${step}Started"] == date

        where:
        step << OtrsTicket.ProcessingStep.values()
    }

    @Unroll
    void 'saveEndTimeIfNeeded, when end time for #step is not set yet, then set value'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket = createOtrsTicket()

        when:
        boolean saved = otrsTicketService.saveEndTimeIfNeeded(otrsTicket, step)

        then:
        saved
        otrsTicket["${step}Finished"]

        where:
        step << OtrsTicket.ProcessingStep.values()
    }

    @Unroll
    void 'saveEndTimeIfNeeded, when end time for #step is already set, then do not change the value'() {
        given:
        Date date = new Date(2000 - 1900, 2, 2)
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket = createOtrsTicket([
                ("${step}Finished".toString()): date
        ])

        when:
        boolean saved = otrsTicketService.saveEndTimeIfNeeded(otrsTicket, step)

        then:
        !saved
        otrsTicket["${step}Finished"] == date

        where:
        step << OtrsTicket.ProcessingStep.values()
    }

    void 'markFinalNotificationSent, when call, then the flag finalNotificationSent should be true'() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket = createOtrsTicket([
                finalNotificationSent: false
        ])

        when:
        otrsTicketService.markFinalNotificationSent(otrsTicket)

        then:
        otrsTicket.finalNotificationSent
    }

    void 'findAllOtrsTickets'() {
        given: "a handfull of tickets and linked and unlinked seqtracks"
        OtrsTicketService otrsTicketService = new OtrsTicketService()
        OtrsTicket otrsTicket01, otrsTicket02, otrsTicket03
        SeqTrack seqTrack1A, seqTrack1B, seqTrack02, seqTrack03, seqTrackOrphanNoDatafile, seqTrackOrphanWithDatafile
        Set<OtrsTicket> actualBatch, actualSingle, actualOrphanNoDatafile, actualOrphanWithDatafile

        // one ticket, with two seqtracks
        otrsTicket01 = createOtrsTicket()
        seqTrack1A = createSeqTrack()
        createDataFile(runSegment: createRunSegment(otrsTicket: otrsTicket01), seqTrack: seqTrack1A)
        seqTrack1B = createSeqTrack()
        createDataFile(runSegment: createRunSegment(otrsTicket: otrsTicket01), seqTrack: seqTrack1B)
        // one ticket, one seqtrack
        otrsTicket02 = createOtrsTicket()
        seqTrack02 = createSeqTrack()
        createDataFile(runSegment: createRunSegment(otrsTicket: otrsTicket02), seqTrack: seqTrack02)
        // another ticket, again one seqtrack
        otrsTicket03 = createOtrsTicket()
        seqTrack03 = createSeqTrack()
        createDataFile(runSegment: createRunSegment(otrsTicket: otrsTicket03), seqTrack: seqTrack03)
        // an orphaned seqtrack, no ticket, no datafile
        seqTrackOrphanNoDatafile = createSeqTrack()
        // an orphaned seqtrack, no ticket, but with a datafile
        seqTrackOrphanWithDatafile = createSeqTrack()
        createDataFile(runSegment: createRunSegment(), seqTrack: seqTrackOrphanWithDatafile)

        when: "looking for seqtrack batches, find all (unique) tickets"
        actualBatch = otrsTicketService.findAllOtrsTickets([seqTrack1A, seqTrack02, seqTrack1B])

        then:
        TestCase.assertContainSame(actualBatch, [otrsTicket01, otrsTicket02])

        when: "looking for a single seqtrack, find its ticket"
        actualSingle = otrsTicketService.findAllOtrsTickets([seqTrack03])

        then:
        TestCase.assertContainSame(actualSingle, [otrsTicket03])

        when: "looking for orphans without datafiles, find nothing"
        actualOrphanNoDatafile = otrsTicketService.findAllOtrsTickets([seqTrackOrphanNoDatafile])

        then:
        TestCase.assertContainSame(actualOrphanNoDatafile, [])

        when: "looking for orphans with datafiles, find nothing"
        actualOrphanWithDatafile = otrsTicketService.findAllOtrsTickets([seqTrackOrphanWithDatafile])

        then:
        TestCase.assertContainSame(actualOrphanWithDatafile, [])
    }

    void "assignOtrsTicketToRunSegment, no RunSegment for runSegementId, throws AssertionError "() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        when:
        otrsTicketService.assignOtrsTicketToRunSegment("", 1)

        then:
        AssertionError error = thrown()
        error.message.contains("No RunSegment found")
    }

    void "assignOtrsTicketToRunSegment, new ticketNumber equals old ticketNumber, returns true"() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        OtrsTicket otrsTicket
        RunSegment runSegment

        otrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        runSegment = createRunSegment(otrsTicket: otrsTicket)

        expect:
        otrsTicketService.assignOtrsTicketToRunSegment(otrsTicket.ticketNumber, runSegment.id)
    }

    void "assignOtrsTicketToRunSegment, new ticketNumber does not pass custom validation, throws UserException"() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        OtrsTicket otrsTicket
        RunSegment runSegment

        otrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        runSegment = createRunSegment(otrsTicket: otrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToRunSegment('abc', runSegment.id)

        then:
        UserException error = thrown()
        error.message.contains("does not pass validation or error while saving.")
    }

    void "assignOtrsTicketToRunSegment, old OtrsTicket consists of several other RunSegements, throws UserException"() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        OtrsTicket otrsTicket
        RunSegment runSegment

        otrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        runSegment = createRunSegment(otrsTicket: otrsTicket)
        createRunSegment(otrsTicket: otrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToRunSegment('2000010112345679', runSegment.id)

        then:
        UserException error = thrown()
        error.message.contains("Assigning a runSegment that belongs to an OTRS-Ticket which consists of several other runSegments is not allowed.")
    }

    void "assignOtrsTicketToRunSegment, new OtrsTicket final notification already sent, throws UserException"() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        OtrsTicket oldOtrsTicket
        RunSegment runSegment
        OtrsTicket newOtrsTicket

        oldOtrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        runSegment = createRunSegment(otrsTicket: oldOtrsTicket)
        newOtrsTicket = createOtrsTicket(ticketNumber: '2000010112345679', finalNotificationSent: true)

        when:
        otrsTicketService.assignOtrsTicketToRunSegment(newOtrsTicket.ticketNumber, runSegment.id)

        then:
        UserException error = thrown()
        error.message.contains("It is not allowed to assign to an finally notified OTRS-Ticket.")
    }

    void "assignOtrsTicketToRunSegment, adjust ProcessingStatus of new OtrsTicket"() {
        given:
        OtrsTicketService otrsTicketService = new OtrsTicketService()

        OtrsTicket newOtrsTicket
        RunSegment runSegment

        Date minDate = new Date() - 1
        Date maxDate = new Date() + 1

        OtrsTicket oldOtrsTicket = createOtrsTicket(
                ticketNumber: '2000010112345678',
                installationStarted: minDate,
                installationFinished: minDate,
                fastqcStarted: maxDate,
                fastqcFinished: maxDate,
                alignmentStarted: null,
                alignmentFinished: maxDate,
                snvStarted: null,
                snvFinished: null
        )
        runSegment = createRunSegment(otrsTicket: oldOtrsTicket)
        newOtrsTicket = createOtrsTicket(
                ticketNumber: '2000010112345679',
                installationStarted: maxDate,
                installationFinished: maxDate,
                fastqcStarted: minDate,
                fastqcFinished: minDate,
                alignmentStarted: minDate,
                alignmentFinished: null,
                snvStarted: null,
                snvFinished: null
        )

        otrsTicketService.metaClass.getProcessingStatus = { Set<SeqTrack> set ->
            return [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> ALL_DONE },
                    getAlignmentProcessingStatus   : { -> PARTLY_DONE_MIGHT_DO_MORE },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_MIGHT_DO },
            ] as ProcessingStatus
        }

        when:
        otrsTicketService.assignOtrsTicketToRunSegment(newOtrsTicket.ticketNumber, runSegment.id)
        newOtrsTicket = OtrsTicket.get(newOtrsTicket.id)

        then:
        minDate.getTime() == newOtrsTicket.installationStarted.getTime()
        maxDate.getTime() == newOtrsTicket.installationFinished.getTime()
        minDate.getTime() == newOtrsTicket.fastqcStarted.getTime()
        maxDate.getTime() == newOtrsTicket.fastqcFinished.getTime()
        minDate.getTime() == newOtrsTicket.alignmentStarted.getTime()
        null == newOtrsTicket.alignmentFinished
        null == newOtrsTicket.snvStarted
        null == newOtrsTicket.snvFinished
    }
}
