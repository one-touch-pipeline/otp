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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestMessageSourceService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService

@Rollback
@Integration
class QcTrafficLightNotificationServiceIntegrationSpec extends Specification implements IsRoddy {

    QcTrafficLightNotificationService qcTrafficLightNotificationService

    TicketService ticketService

    void setupData() {
        qcTrafficLightNotificationService.messageSourceService = new TestMessageSourceService()
    }

    @Unroll
    void "createResultsAreWarnedSubject, properly builds subject"() {
        given:
        setupData()
        DomainFactory.createProcessingOptionForTicketPrefix()

        List<Ticket> tickets = (1..2).collect {
            return useTickets ? createTicket() : null
        }.sort { it?.ticketCreated }

        SeqType seqType = createSeqType()
        Sample sample = createSample()
        List<SeqTrack> seqTracks = tickets.collect { Ticket ticket ->
            createFastqFile(
                    fastqImportInstance: createFastqImportInstance(ticket: ticket),
                    seqTrack           : createSeqTrack(
                            sample        : sample,
                            seqType       : seqType,
                            ilseSubmission: createIlseSubmission(),
                    ),
            ).seqTrack
        }
        RoddyBamFile bamFile = createBamFile(seqTracks: seqTracks)

        String prefix = useTickets ? "${ticketService.getPrefixedTicketNumber(tickets.last())} " : ""
        String base = "[S#${seqTracks*.ilseSubmission.ilseNumber.sort().join(',')}] QC issues for bam file of ${bamFile.sample} ${bamFile.seqType}"

        when:
        String result = qcTrafficLightNotificationService.createResultsAreWarnedSubject(bamFile, false)

        then:
        prefix + base == result

        where:
        useTickets | _
        true           | _
        false          | _
    }
}
