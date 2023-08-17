/*
 * Copyright 2011-2023 The OTP authors
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
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.time.Instant
import java.time.temporal.ChronoUnit

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

@Rollback
@Integration
class TicketServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    private TicketService ticketService

    void setup() {
        ticketService = new TicketService()
        ticketService.processingOptionService = new ProcessingOptionService()
        ticketService.notificationCreator = new NotificationCreator()
        ticketService.notificationCreator.processingOptionService = new ProcessingOptionService()
        ticketService.notificationCreator.userProjectRoleService = new UserProjectRoleService()
    }

    @Unroll
    void 'saveStartTimeIfNeeded, when start time for #step is not set yet, then set value'() {
        given:
        Ticket ticket = createTicket()

        when:
        ticketService.saveStartTimeIfNeeded(ticket, step)

        then:
        ticket["${step}Started"]

        where:
        step << Ticket.ProcessingStep.values()
    }

    @Unroll
    void 'saveStartTimeIfNeeded, when start time for #step is already set, then do not change the value'() {
        given:
        Date date = new Date(2000 - 1900, 2, 2)
        Ticket ticket = createTicket([
                ("${step}Started".toString()): date
        ])

        when:
        ticketService.saveStartTimeIfNeeded(ticket, step)

        then:
        ticket["${step}Started"] == date

        where:
        step << Ticket.ProcessingStep.values()
    }

    @Unroll
    void 'saveEndTimeIfNeeded, when end time for #step is not set yet, then set value'() {
        given:
        Ticket ticket = createTicket()

        when:
        boolean saved = ticketService.saveEndTimeIfNeeded(ticket, step)

        then:
        saved
        ticket["${step}Finished"]

        where:
        step << Ticket.ProcessingStep.values()
    }

    @Unroll
    void 'saveEndTimeIfNeeded, when end time for #step is already set, then do not change the value'() {
        given:
        Date date = new Date(2000 - 1900, 2, 2)
        Ticket ticket = createTicket([
                ("${step}Finished".toString()): date
        ])

        when:
        boolean saved = ticketService.saveEndTimeIfNeeded(ticket, step)

        then:
        !saved
        ticket["${step}Finished"] == date

        where:
        step << Ticket.ProcessingStep.values()
    }

    void 'findAllSeqTracks finds expected SeqTracks'() {
        given:
        Ticket ticketA = createTicket()
        FastqImportInstance fastqImportInstanceA1 = createFastqImportInstance(ticket: ticketA)
        FastqImportInstance fastqImportInstanceA2 = createFastqImportInstance(ticket: ticketA)
        SeqTrack seqTrackA1 = createSeqTrackWithOneFastqFile([:], [fastqImportInstance: fastqImportInstanceA1])
        SeqTrack seqTrackA2 = createSeqTrackWithOneFastqFile([:], [fastqImportInstance: fastqImportInstanceA2])
        SeqTrack seqTrackA3 = createSeqTrackWithOneFastqFile([:], [fastqImportInstance: fastqImportInstanceA2])

        Ticket ticketB = createTicket()
        FastqImportInstance fastqImportInstanceB1 = createFastqImportInstance(ticket: ticketB)
        SeqTrack seqTrackB1 = DomainFactory.createSeqTrackWithTwoFastqFiles([:], [fastqImportInstance: fastqImportInstanceB1], [:])

        Ticket ticketC = createTicket()

        expect:
        assertContainSame(ticketService.findAllSeqTracks(ticketA), [seqTrackA1, seqTrackA2, seqTrackA3])
        assertContainSame(ticketService.findAllSeqTracks(ticketB), [seqTrackB1])
        ticketService.findAllSeqTracks(ticketC).isEmpty()
    }

    void 'markFinalNotificationSent, when call, then the flag finalNotificationSent should be true'() {
        given:
        Ticket ticket = createTicket([
                finalNotificationSent: false
        ])

        when:
        ticketService.markFinalNotificationSent(ticket)

        then:
        ticket.finalNotificationSent
    }

    void 'findAllTickets'() {
        given: "a handfull of tickets and linked and unlinked seqtracks"
        Ticket ticket01, ticket02, ticket03
        SeqTrack seqTrack1A, seqTrack1B, seqTrack02, seqTrack03, seqTrackOrphanNoRawSequenceFile, seqTrackOrphanWithRawSequenceFile
        Set<Ticket> actualBatch, actualSingle, actualOrphanNoRawSequenceFile, actualOrphanWithRawSequenceFile

        // one ticket, with two seqtracks
        ticket01 = createTicket()
        seqTrack1A = createSeqTrack()
        createFastqFile(fastqImportInstance: createFastqImportInstance(ticket: ticket01), seqTrack: seqTrack1A)
        seqTrack1B = createSeqTrack()
        createFastqFile(fastqImportInstance: createFastqImportInstance(ticket: ticket01), seqTrack: seqTrack1B)
        // one ticket, one seqtrack
        ticket02 = createTicket()
        seqTrack02 = createSeqTrack()
        createFastqFile(fastqImportInstance: createFastqImportInstance(ticket: ticket02), seqTrack: seqTrack02)
        // another ticket, again one seqtrack
        ticket03 = createTicket()
        seqTrack03 = createSeqTrack()
        createFastqFile(fastqImportInstance: createFastqImportInstance(ticket: ticket03), seqTrack: seqTrack03)
        // an orphaned seqtrack, no ticket, no datafile
        seqTrackOrphanNoRawSequenceFile = createSeqTrack()
        // an orphaned seqtrack, no ticket, but with a datafile
        seqTrackOrphanWithRawSequenceFile = createSeqTrack()
        createFastqFile(fastqImportInstance: createFastqImportInstance(), seqTrack: seqTrackOrphanWithRawSequenceFile)

        when: "looking for seqtrack batches, find all (unique) tickets"
        actualBatch = ticketService.findAllTickets([seqTrack1A, seqTrack02, seqTrack1B])

        then:
        TestCase.assertContainSame(actualBatch, [ticket01, ticket02])

        when: "looking for a single seqtrack, find its ticket"
        actualSingle = ticketService.findAllTickets([seqTrack03])

        then:
        TestCase.assertContainSame(actualSingle, [ticket03])

        when: "looking for orphans without datafiles, find nothing"
        actualOrphanNoRawSequenceFile = ticketService.findAllTickets([seqTrackOrphanNoRawSequenceFile])

        then:
        TestCase.assertContainSame(actualOrphanNoRawSequenceFile, [])

        when: "looking for orphans with datafiles, find nothing"
        actualOrphanWithRawSequenceFile = ticketService.findAllTickets([seqTrackOrphanWithRawSequenceFile])

        then:
        TestCase.assertContainSame(actualOrphanWithRawSequenceFile, [])
    }

    void "assignTicketToFastqImportInstance, assign new ticketNumber, ticket already exists"() {
        given:
        Ticket oldTicket = createTicket()
        Ticket newTicket = createTicket()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: oldTicket)

        when:
        ticketService.assignTicketToFastqImportInstance(newTicket.ticketNumber, fastqImportInstance.id)

        then:
        fastqImportInstance.ticket == newTicket
    }

    void "assignTicketToFastqImportInstance, assign new ticketNumber, ticket does not exists"() {
        given:
        String newTicketNumber = "2000112201234567"
        Ticket oldTicket = createTicket()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: oldTicket)

        when:
        ticketService.assignTicketToFastqImportInstance(newTicketNumber, fastqImportInstance.id)

        then:
        fastqImportInstance.ticket == CollectionUtils.exactlyOneElement(Ticket.findAllByTicketNumber(newTicketNumber))
    }

    void "assignTicketToFastqImportInstance, new ticketNumber equals old ticketNumber, does not fail"() {
        given:
        Ticket ticket = createTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)

        when:
        ticketService.assignTicketToFastqImportInstance(ticket.ticketNumber, fastqImportInstance.id)

        then:
        fastqImportInstance.ticket == ticket
    }

    void "assignTicketToFastqImportInstance, no FastqImportInstance for runSegementId, throws AssertionError "() {
        when:
        ticketService.assignTicketToFastqImportInstance("", 1)

        then:
        AssertionError error = thrown()
        error.message.contains("No FastqImportInstance found")
    }

    void "assignTicketToFastqImportInstance, new ticketNumber does not pass custom validation, throws UserException"() {
        given:
        Ticket ticket = createTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)

        when:
        ticketService.assignTicketToFastqImportInstance('abc', fastqImportInstance.id)

        then:
        UserException error = thrown()
        error.message.contains("does not pass validation or error while saving.")
    }

    void "assignTicketToFastqImportInstance, old Ticket consists of several other RunSegements, throws UserException"() {
        Ticket ticket = createTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)

        createFastqImportInstance(ticket: ticket)

        when:
        ticketService.assignTicketToFastqImportInstance('2000010112345679', fastqImportInstance.id)

        then:
        UserException error = thrown()
        error.message.contains("Assigning a fastqImportInstance that belongs to a Ticket which consists of several other fastqImportInstances is not allowed.")
    }

    void "assignTicketToFastqImportInstance, new Ticket final notification already sent, throws UserException"() {
        given:
        Ticket oldTicket = createTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: oldTicket)
        Ticket newTicket = createTicket(ticketNumber: '2000010112345679', finalNotificationSent: true)

        when:
        ticketService.assignTicketToFastqImportInstance(newTicket.ticketNumber, fastqImportInstance.id)

        then:
        UserException error = thrown()
        error.message.contains("It is not allowed to assign to an finally notified Ticket.")
    }

    void "assignTicketToFastqImportInstance, adjust ProcessingStatus of new Ticket"() {
        given:
        Date minDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        Date maxDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS))

        Ticket oldTicket = createTicket(
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
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: oldTicket)
        Ticket newTicket = createTicket(
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

        ticketService.notificationCreator.metaClass.getProcessingStatus = { Set<SeqTrack> set ->
            return [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> ALL_DONE },
                    getAlignmentProcessingStatus   : { -> PARTLY_DONE_MIGHT_DO_MORE },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_MIGHT_DO },
            ] as ProcessingStatus
        }

        when:
        ticketService.assignTicketToFastqImportInstance(newTicket.ticketNumber, fastqImportInstance.id)
        newTicket = Ticket.get(newTicket.id)

        then:
        minDate.time == newTicket.installationStarted.time
        maxDate.time == newTicket.installationFinished.time
        minDate.time == newTicket.fastqcStarted.time
        maxDate.time == newTicket.fastqcFinished.time
        minDate.time == newTicket.alignmentStarted.time
        null == newTicket.alignmentFinished
        null == newTicket.snvStarted
        null == newTicket.snvFinished
    }

    void "getMetaDataFilesOfTicket, returns all MetaDataFiles associated with the ticket"() {
        given:
        Ticket ticket = createTicket()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)
        List<MetaDataFile> expected = [
                DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance),
                DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance),
                DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance),
        ]

        FastqImportInstance otherFastqImportInstance = createFastqImportInstance()
        DomainFactory.createMetaDataFile(fastqImportInstance: otherFastqImportInstance)

        when:
        List<MetaDataFile> result = ticketService.getMetaDataFilesOfTicket(ticket).sort { it.id }

        then:
        expected == result
    }

    void "getAllFastqImportInstances, returns all FastqImportInstances related to the given ticket"() {
        given:
        Ticket ticket = createTicket()
        List<FastqImportInstance> expected = instancesToCreate.collect {
            createFastqImportInstance(ticket: ticket)
        }
        createFastqImportInstance()

        when:
        List<FastqImportInstance> result = ticketService.getAllFastqImportInstances(ticket)

        then:
        TestCase.assertContainSame(expected, result)

        where:
        instancesToCreate << [0, 1, 2]
    }

    void "getPrefixedTicketNumber, should give prefixed ticket number"() {
        given:
        final String TICKET_PREFIX = "prefix"
        Ticket ticket = createTicket()

        DomainFactory.createProcessingOptionForTicketPrefix(TICKET_PREFIX)

        expect:
        ticketService.getPrefixedTicketNumber(ticket) == "${TICKET_PREFIX}#${ticket.ticketNumber}"
    }
}
