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
package de.dkfz.tbi.otp.infrastructure

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.tracking.ProcessingTimeStatisticsService
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class ProcessingTimeStatisticsServiceIntegrationSpec extends Specification {

    @Autowired
    ProcessingTimeStatisticsService processingTimeStatisticsService

    void "findAllOtrsTicketsByDateBetweenAndSearch, when no dateFrom and no dateTo and no search, throw Exception"() {
        when:
        processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(null, null, null)

        then:
        AssertionError e = thrown()
        e.message.contains('No date')
    }

    void "findAllOtrsTicketsByDateBetweenAndSearch, when no search, ignore search"() {
        given:
        LocalDate dateFrom = LocalDate.now()
        LocalDate dateTo = LocalDate.now().plusDays(1)

        OtrsTicket ticket = DomainFactory.createOtrsTicket(dateCreated: TimeUtils.toDate(dateTo))

        expect:
        [ticket] == processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, null)
    }

    void "findAllOtrsTicketsByDateBetweenAndSearch, when string to search for is too small, throw Exception"() {
        given:
        LocalDate dateFrom = LocalDate.now()
        LocalDate dateTo = LocalDate.now().plusDays(1)

        createOtrsTicketWithSeqTrack([dateCreated: TimeUtils.toDate(dateFrom)])

        when:
        processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, "56")

        then:
        RuntimeException e = thrown()
        e.message.contains('String to search for is too short')
    }

    void "findAllOtrsTicketsByDateBetweenAndSearch, when string to search in ILSe ID, return ticket with searched ILSe ID"() {
        LocalDate dateFrom = LocalDate.now()
        LocalDate dateTo = LocalDate.now().plusDays(1)

        // values that are searched for are set explicitly, otherwise they could contain the search term depending on the order tests are executed
        def (ticketA, seqTrackA) = createOtrsTicketWithSeqTrack([dateCreated: TimeUtils.toDate(dateFrom), ticketNumber: "2016122411111111",], [ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 1234)])
        seqTrackA.sample.individual.project.name = "proj_1"
        seqTrackA.sample.individual.project.save(flush: true)
        seqTrackA.run.name = "run_1"
        seqTrackA.run.save(flush: true)
        SeqTrack seqTrackB = createOtrsTicketWithSeqTrack([dateCreated: TimeUtils.toDate(dateFrom), ticketNumber: "2016122422222222",], [ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 5678)])[1] as SeqTrack
        seqTrackB.sample.individual.project.name = "proj_2"
        seqTrackB.sample.individual.project.save(flush: true)
        seqTrackB.run.name = "run_2"
        seqTrackB.run.save(flush: true)

        expect:
        [ticketA] == processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, '234')
    }

    @Ignore('Fails with H2, succeeds with PostgreSQL -> OTP-1874')
    void "findAllOtrsTicketsByDateBetweenAndSearch, when string to search in project name, return ticket with searched project name"() {
        LocalDate dateFrom = LocalDate.now()
        LocalDate dateTo = LocalDate.now().plusDays(1)

        def (ticketA, projectA) = createOtrsTicketWithProject([dateCreated: TimeUtils.toDate(dateFrom)])
        createOtrsTicketWithProject([dateCreated: TimeUtils.toDate(dateFrom)])

        expect:
        [ticketA] == processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, projectA.name)
    }

    void "formatData, when all fine, return formatted ticket information"() {
        given:
        Comment comment = new Comment(comment: "comment", author: "me", modificationDate: new Date())
        comment.save(flush: true)

        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                submissionReceivedNotice: Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                ticketCreated: new Date(),
                dateCreated: Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
                installationStarted: Date.from(Instant.now().plus(2, ChronoUnit.DAYS)),
                installationFinished: Date.from(Instant.now().plus(3, ChronoUnit.DAYS)),
                fastqcStarted: Date.from(Instant.now().plus(4, ChronoUnit.DAYS)),
                fastqcFinished: Date.from(Instant.now().plus(5, ChronoUnit.DAYS)),
                alignmentStarted: Date.from(Instant.now().plus(6, ChronoUnit.DAYS)),
                alignmentFinished: Date.from(Instant.now().plus(7, ChronoUnit.DAYS)),
                snvStarted: Date.from(Instant.now().plus(8, ChronoUnit.DAYS)),
                snvFinished: Date.from(Instant.now().plus(9, ChronoUnit.DAYS)),
                finalNotificationSent: true,
                comment: comment,
        )

        String url = "url.url"
        processingTimeStatisticsService.otrsTicketService = Spy(OtrsTicketService) {
            1 * buildTicketDirectLink(_) >> url
        }

        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        Run run = DomainFactory.createRun()
        Project projectA = DomainFactory.createProject()
        Project projectB = DomainFactory.createProject()
        Individual individualA = DomainFactory.createIndividual(project: projectA)
        Individual individualB = DomainFactory.createIndividual(project: projectB)
        Sample sampleA = DomainFactory.createSample(individual: individualA)
        Sample sampleB = DomainFactory.createSample(individual: individualB)
        SeqTrack seqTrackA = DomainFactory.createSeqTrackWithOneFastqFile([run: run, sample: sampleA, ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 1234)], [fastqImportInstance: fastqImportInstance])
        SeqTrack seqTrackB = DomainFactory.createSeqTrackWithOneFastqFile([run: run, sample: sampleB, ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 5678)], [fastqImportInstance: fastqImportInstance])

        when:
        List result = processingTimeStatisticsService.formatData(ticket)

        then:
        result.size() == 30
        result[0] == url
        TestCase.assertContainSame(result[1], [seqTrackA.ilseId as String, seqTrackB.ilseId as String])
        TestCase.assertContainSame(result[2], [projectA.name, projectB.name].sort())
        TestCase.assertContainSame(result[3], [seqTrackA.run.name])
        TestCase.assertContainSame(result[4], [sampleA.displayName, sampleB.displayName])
        TestCase.assertContainSame(result[5], ["${seqTrackA.run}, lane: ${seqTrackA.laneId}", "${seqTrackB.run}, lane: ${seqTrackB.laneId}"])
        result[6] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.submissionReceivedNotice)
        result[7] == TimeUtils.getFormattedDurationWithDays(ticket.submissionReceivedNotice, ticket.ticketCreated)
        result[8] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.ticketCreated)
        result[9] == TimeUtils.getFormattedDurationWithDays(ticket.ticketCreated, ticket.installationStarted)
        result[10] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.installationStarted)
        result[11] == TimeUtils.getFormattedDurationWithDays(ticket.installationStarted, ticket.installationFinished)
        result[12] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.installationFinished)
        result[13] == TimeUtils.getFormattedDurationWithDays(ticket.installationFinished, ticket.fastqcStarted)
        result[14] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.fastqcStarted)
        result[15] == TimeUtils.getFormattedDurationWithDays(ticket.fastqcStarted, ticket.fastqcFinished)
        result[16] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.fastqcFinished)
        result[17] == TimeUtils.getFormattedDurationWithDays(ticket.fastqcFinished, ticket.alignmentStarted)
        result[18] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.alignmentStarted)
        result[19] == TimeUtils.getFormattedDurationWithDays(ticket.alignmentStarted, ticket.alignmentFinished)
        result[20] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.alignmentFinished)
        result[21] == TimeUtils.getFormattedDurationWithDays(ticket.alignmentFinished, ticket.snvStarted)
        result[22] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.snvStarted)
        result[23] == TimeUtils.getFormattedDurationWithDays(ticket.snvStarted, ticket.snvFinished)
        result[24] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.snvFinished)
        result[25] == TimeUtils.getFormattedDurationWithDays(ticket.installationStarted, ticket.snvFinished)
        result[26] == comment.comment
        result[27] == ticket.finalNotificationSent
        result[28] == ticket.id
        result[29] == ticket.ticketNumber
    }

    private static List createOtrsTicketWithSeqTrack(Map otrsTicketProperties = [:], Map seqTrackProperties = [:]) {
        OtrsTicket ticket = DomainFactory.createOtrsTicket(otrsTicketProperties)
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneFastqFile(seqTrackProperties, [fastqImportInstance: fastqImportInstance])

        return [ticket, seqTrack]
    }

    private static List createOtrsTicketWithProject(Map otrsTicketProperties = [:], Map projectProperties = [:]) {
        Project project = DomainFactory.createProject(projectProperties)
        Individual individual = DomainFactory.createIndividual(project: project)
        Sample sample = DomainFactory.createSample(individual: individual)

        OtrsTicket ticket = createOtrsTicketWithSeqTrack(otrsTicketProperties, [sample: sample])[0] as OtrsTicket

        return [ticket, project]
    }
}
