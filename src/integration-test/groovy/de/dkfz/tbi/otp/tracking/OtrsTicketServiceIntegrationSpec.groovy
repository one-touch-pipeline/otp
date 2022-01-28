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
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.time.Instant
import java.time.temporal.ChronoUnit

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

@Rollback
@Integration
class OtrsTicketServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    private OtrsTicketService otrsTicketService

    void setup() {
        otrsTicketService = new OtrsTicketService()
        otrsTicketService.notificationCreator = new NotificationCreator()
        otrsTicketService.notificationCreator.processingOptionService = new ProcessingOptionService()
        otrsTicketService.notificationCreator.userProjectRoleService = new UserProjectRoleService()
    }

    @Unroll
    void 'saveStartTimeIfNeeded, when start time for #step is not set yet, then set value'() {
        given:
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
        OtrsTicket otrsTicket01, otrsTicket02, otrsTicket03
        SeqTrack seqTrack1A, seqTrack1B, seqTrack02, seqTrack03, seqTrackOrphanNoDatafile, seqTrackOrphanWithDatafile
        Set<OtrsTicket> actualBatch, actualSingle, actualOrphanNoDatafile, actualOrphanWithDatafile

        // one ticket, with two seqtracks
        otrsTicket01 = createOtrsTicket()
        seqTrack1A = createSeqTrack()
        createDataFile(fastqImportInstance: createFastqImportInstance(otrsTicket: otrsTicket01), seqTrack: seqTrack1A)
        seqTrack1B = createSeqTrack()
        createDataFile(fastqImportInstance: createFastqImportInstance(otrsTicket: otrsTicket01), seqTrack: seqTrack1B)
        // one ticket, one seqtrack
        otrsTicket02 = createOtrsTicket()
        seqTrack02 = createSeqTrack()
        createDataFile(fastqImportInstance: createFastqImportInstance(otrsTicket: otrsTicket02), seqTrack: seqTrack02)
        // another ticket, again one seqtrack
        otrsTicket03 = createOtrsTicket()
        seqTrack03 = createSeqTrack()
        createDataFile(fastqImportInstance: createFastqImportInstance(otrsTicket: otrsTicket03), seqTrack: seqTrack03)
        // an orphaned seqtrack, no ticket, no datafile
        seqTrackOrphanNoDatafile = createSeqTrack()
        // an orphaned seqtrack, no ticket, but with a datafile
        seqTrackOrphanWithDatafile = createSeqTrack()
        createDataFile(fastqImportInstance: createFastqImportInstance(), seqTrack: seqTrackOrphanWithDatafile)

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

    void "assignOtrsTicketToFastqImportInstance, assign new ticketNumber, ticket already exists"() {
        given:
        OtrsTicket oldOtrsTicket = createOtrsTicket()
        OtrsTicket newOtrsTicket = createOtrsTicket()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: oldOtrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance(newOtrsTicket.ticketNumber, fastqImportInstance.id)

        then:
        fastqImportInstance.otrsTicket == newOtrsTicket
    }

    void "assignOtrsTicketToFastqImportInstance, assign new ticketNumber, ticket does not exists"() {
        given:
        String newTicketNumber = "2000112201234567"
        OtrsTicket oldOtrsTicket = createOtrsTicket()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: oldOtrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance(newTicketNumber, fastqImportInstance.id)

        then:
        fastqImportInstance.otrsTicket == CollectionUtils.exactlyOneElement(OtrsTicket.findAllByTicketNumber(newTicketNumber))
    }

    void "assignOtrsTicketToFastqImportInstance, new ticketNumber equals old ticketNumber, does not fail"() {
        given:
        OtrsTicket otrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: otrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance(otrsTicket.ticketNumber, fastqImportInstance.id)

        then:
        fastqImportInstance.otrsTicket == otrsTicket
    }

    void "assignOtrsTicketToFastqImportInstance, no FastqImportInstance for runSegementId, throws AssertionError "() {
        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance("", 1)

        then:
        AssertionError error = thrown()
        error.message.contains("No FastqImportInstance found")
    }

    void "assignOtrsTicketToFastqImportInstance, new ticketNumber does not pass custom validation, throws UserException"() {
        given:
        OtrsTicket otrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: otrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance('abc', fastqImportInstance.id)

        then:
        UserException error = thrown()
        error.message.contains("does not pass validation or error while saving.")
    }

    void "assignOtrsTicketToFastqImportInstance, old OtrsTicket consists of several other RunSegements, throws UserException"() {
        OtrsTicket otrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: otrsTicket)

        createFastqImportInstance(otrsTicket: otrsTicket)

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance('2000010112345679', fastqImportInstance.id)

        then:
        UserException error = thrown()
        error.message.contains("Assigning a fastqImportInstance that belongs to an OTRS-Ticket which consists of several other fastqImportInstances is not allowed.")
    }

    void "assignOtrsTicketToFastqImportInstance, new OtrsTicket final notification already sent, throws UserException"() {
        given:
        OtrsTicket oldOtrsTicket = createOtrsTicket(ticketNumber: '2000010112345678')
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: oldOtrsTicket)
        OtrsTicket newOtrsTicket = createOtrsTicket(ticketNumber: '2000010112345679', finalNotificationSent: true)

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance(newOtrsTicket.ticketNumber, fastqImportInstance.id)

        then:
        UserException error = thrown()
        error.message.contains("It is not allowed to assign to an finally notified OTRS-Ticket.")
    }

    void "assignOtrsTicketToFastqImportInstance, adjust ProcessingStatus of new OtrsTicket"() {
        given:
        Date minDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        Date maxDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS))

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
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: oldOtrsTicket)
        OtrsTicket newOtrsTicket = createOtrsTicket(
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

        otrsTicketService.notificationCreator.metaClass.getProcessingStatus = { Set<SeqTrack> set ->
            return [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> ALL_DONE },
                    getAlignmentProcessingStatus   : { -> PARTLY_DONE_MIGHT_DO_MORE },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_MIGHT_DO },
            ] as ProcessingStatus
        }

        when:
        otrsTicketService.assignOtrsTicketToFastqImportInstance(newOtrsTicket.ticketNumber, fastqImportInstance.id)
        newOtrsTicket = OtrsTicket.get(newOtrsTicket.id)

        then:
        minDate.time == newOtrsTicket.installationStarted.time
        maxDate.time == newOtrsTicket.installationFinished.time
        minDate.time == newOtrsTicket.fastqcStarted.time
        maxDate.time == newOtrsTicket.fastqcFinished.time
        minDate.time == newOtrsTicket.alignmentStarted.time
        null == newOtrsTicket.alignmentFinished
        null == newOtrsTicket.snvStarted
        null == newOtrsTicket.snvFinished
    }

    void "getMetaDataFilesOfOtrsTicket, returns all MetaDataFiles associated with the ticket"() {
        given:
        OtrsTicket otrsTicket = createOtrsTicket()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(otrsTicket: otrsTicket)
        List<MetaDataFile> expected = [
                DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance),
                DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance),
                DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance),
        ]

        FastqImportInstance otherFastqImportInstance = createFastqImportInstance()
        DomainFactory.createMetaDataFile(fastqImportInstance: otherFastqImportInstance)

        when:
        List<MetaDataFile> result = otrsTicketService.getMetaDataFilesOfOtrsTicket(otrsTicket).sort { it.id }

        then:
        expected == result
    }

    void "getAllFastqImportInstances, returns all FastqImportInstances related to the given ticket"() {
        given:
        OtrsTicket otrsTicket = createOtrsTicket()
        List<FastqImportInstance> expected = instancesToCreate.collect {
            createFastqImportInstance(otrsTicket: otrsTicket)
        }
        createFastqImportInstance()

        when:
        List<FastqImportInstance> result = otrsTicketService.getAllFastqImportInstances(otrsTicket)

        then:
        TestCase.assertContainSame(expected, result)

        where:
        instancesToCreate << [0, 1, 2]
    }
}
