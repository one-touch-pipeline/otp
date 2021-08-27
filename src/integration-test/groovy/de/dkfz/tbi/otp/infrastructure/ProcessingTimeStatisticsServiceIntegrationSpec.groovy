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
package de.dkfz.tbi.otp.infrastructure

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.ProcessingTimeStatisticsService
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.time.LocalDate

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
                submissionReceivedNotice: new Date() - 1,
                ticketCreated: new Date(),
                dateCreated: new Date() + 1,
                installationStarted: new Date() + 2,
                installationFinished: new Date() + 3,
                fastqcStarted: new Date() + 4,
                fastqcFinished: new Date() + 5,
                alignmentStarted: new Date() + 6,
                alignmentFinished: new Date() + 7,
                snvStarted: new Date() + 8,
                snvFinished: new Date() + 9,
                finalNotificationSent: true,
                comment: comment,
        )

        String url = "url.url"
        ticket.metaClass.getUrl = { ->
            return url
        }

        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        Run run = DomainFactory.createRun()
        Project projectA = DomainFactory.createProject()
        Project projectB = DomainFactory.createProject()
        Individual individualA = DomainFactory.createIndividual(project: projectA)
        Individual individualB = DomainFactory.createIndividual(project: projectB)
        Sample sampleA = DomainFactory.createSample(individual: individualA)
        Sample sampleB = DomainFactory.createSample(individual: individualB)
        SeqTrack seqTrackA = DomainFactory.createSeqTrackWithOneDataFile([run: run, sample: sampleA, ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 1234)], [fastqImportInstance: fastqImportInstance])
        SeqTrack seqTrackB = DomainFactory.createSeqTrackWithOneDataFile([run: run, sample: sampleB, ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 5678)], [fastqImportInstance: fastqImportInstance])

        expect:
        List expect = [
                url,
                [seqTrackA.ilseId as String, seqTrackB.ilseId as String],
                [projectA.name, projectB.name],
                [seqTrackA.run.name],
                [sampleA.displayName, sampleB.displayName],
                ["${seqTrackA.run}, lane: ${seqTrackA.laneId}", "${seqTrackB.run}, lane: ${seqTrackB.laneId}"],
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.submissionReceivedNotice),
                TimeUtils.getFormattedDurationWithDays(ticket.submissionReceivedNotice, ticket.ticketCreated),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.ticketCreated),
                TimeUtils.getFormattedDurationWithDays(ticket.ticketCreated, ticket.installationStarted),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.installationStarted),
                TimeUtils.getFormattedDurationWithDays(ticket.installationStarted, ticket.installationFinished),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.installationFinished),
                TimeUtils.getFormattedDurationWithDays(ticket.installationFinished, ticket.fastqcStarted),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.fastqcStarted),
                TimeUtils.getFormattedDurationWithDays(ticket.fastqcStarted, ticket.fastqcFinished),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.fastqcFinished),
                TimeUtils.getFormattedDurationWithDays(ticket.fastqcFinished, ticket.alignmentStarted),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.alignmentStarted),
                TimeUtils.getFormattedDurationWithDays(ticket.alignmentStarted, ticket.alignmentFinished),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.alignmentFinished),
                TimeUtils.getFormattedDurationWithDays(ticket.alignmentFinished, ticket.snvStarted),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.snvStarted),
                TimeUtils.getFormattedDurationWithDays(ticket.snvStarted, ticket.snvFinished),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(ticket.snvFinished),
                TimeUtils.getFormattedDurationWithDays(ticket.installationStarted, ticket.snvFinished),
                comment.comment,
                ticket.finalNotificationSent,
                ticket.id,
                ticket.ticketNumber,
        ]

        expect == processingTimeStatisticsService.formatData(ticket)
    }

    private static List createOtrsTicketWithSeqTrack(Map otrsTicketProperties = [:], Map seqTrackProperties = [:]) {
        OtrsTicket ticket = DomainFactory.createOtrsTicket(otrsTicketProperties)
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(seqTrackProperties, [fastqImportInstance: fastqImportInstance])

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
