package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.joda.time.*
import org.springframework.beans.factory.annotation.*

class ProcessingTimeStatisticsServiceIntegrationSpec extends IntegrationSpec {

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
        LocalDate dateFrom = new LocalDate()
        LocalDate dateTo = new LocalDate().plusDays(1)

        OtrsTicket ticket = DomainFactory.createOtrsTicket(dateCreated: dateFrom.toDate())

        expect:
        [ticket] == processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, null)
    }

    void "findAllOtrsTicketsByDateBetweenAndSearch, when string to search for is too small, throw Exception"() {
        given:
        LocalDate dateFrom = new LocalDate()
        LocalDate dateTo = new LocalDate().plusDays(1)

        createOtrsTicketWithSeqTrack([dateCreated: dateFrom.toDate()], [ilseId: "1234"])

        when:
        processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, "56")

        then:
        RuntimeException e = thrown()
        e.message.contains('String to search for is too short')
    }

    void "findAllOtrsTicketsByDateBetweenAndSearch, when string to search in ILSe ID, return ticket with searched ILSe ID"() {
        LocalDate dateFrom = new LocalDate()
        LocalDate dateTo = new LocalDate().plusDays(1)

        def (ticketA, seqTrackA) = createOtrsTicketWithSeqTrack([dateCreated: dateFrom.toDate()], [ilseId: "1234"])
        createOtrsTicketWithSeqTrack([dateCreated: dateFrom.toDate()], [ilseId: "5678"])

        expect:
        [ticketA] == processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, seqTrackA.ilseId)
    }

    void "findAllOtrsTicketsByDateBetweenAndSearch, when string to search in project name, return ticket with searched project name"() {
        LocalDate dateFrom = new LocalDate()
        LocalDate dateTo = new LocalDate().plusDays(1)

        def (ticketA, projectA) = createOtrsTicketWithProject([dateCreated: dateFrom.toDate()])
        createOtrsTicketWithProject([dateCreated: dateFrom.toDate()])

        expect:
        [ticketA] == processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, projectA.name)
    }

    void "formatData, when all fine, return formatted ticket information"() {
        given:
        Comment comment = new Comment(comment: "comment", author: "me", modificationDate: new Date())
        comment.save(flush: true)

        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                submissionReceivedNotice: new Date().minus(1),
                ticketCreated: new Date(),
                dateCreated: new Date().plus(1),
                installationStarted: new Date().plus(2),
                installationFinished: new Date().plus(3),
                fastqcStarted: new Date().plus(4),
                fastqcFinished: new Date().plus(5),
                alignmentStarted: new Date().plus(6),
                alignmentFinished: new Date().plus(7),
                snvStarted: new Date().plus(8),
                snvFinished: new Date().plus(9),
                finalNotificationSent: true,
                comment: comment,
        )

        String url = "url.url"
        ticket.metaClass.getUrl = { ->
            return url
        }

        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        Run run = DomainFactory.createRun()
        Project projectA = DomainFactory.createProject()
        Project projectB = DomainFactory.createProject()
        Individual individualA = DomainFactory.createIndividual(project: projectA)
        Individual individualB = DomainFactory.createIndividual(project: projectB)
        Sample sampleA = DomainFactory.createSample(individual: individualA)
        Sample sampleB = DomainFactory.createSample(individual: individualB)
        SeqTrack seqTrackA = DomainFactory.createSeqTrackWithOneDataFile([run: run, sample: sampleA, ilseId: "1234"], [runSegment: runSegment])
        SeqTrack seqTrackB = DomainFactory.createSeqTrackWithOneDataFile([run: run, sample: sampleB, ilseId: "5678"], [runSegment: runSegment])

        expect:
        List expect = [
                url,
                [seqTrackA.ilseId, seqTrackB.ilseId],
                [projectA.name, projectB.name],
                [seqTrackA.run.name],
                [sampleA.displayName, sampleB.displayName],
                ["${seqTrackA.run}, lane: ${seqTrackA.laneId}", "${seqTrackB.run}, lane: ${seqTrackB.laneId}"],
                ticket.submissionReceivedNotice.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.submissionReceivedNotice, ticket.ticketCreated),
                ticket.ticketCreated.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.ticketCreated, ticket.installationStarted),
                ticket.installationStarted.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.installationStarted, ticket.installationFinished),
                ticket.installationFinished.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.installationFinished, ticket.fastqcStarted),
                ticket.fastqcStarted.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.fastqcStarted, ticket.fastqcFinished),
                ticket.fastqcFinished.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.fastqcFinished, ticket.alignmentStarted),
                ticket.alignmentStarted.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.alignmentStarted, ticket.alignmentFinished),
                ticket.alignmentFinished.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.alignmentFinished, ticket.snvStarted),
                ticket.snvStarted.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.snvStarted, ticket.snvFinished),
                ticket.snvFinished.format(ProcessingTimeStatisticsService.DATE_FORMAT),
                ProcessingTimeStatisticsService.getFormattedPeriod(ticket.installationStarted, ticket.snvFinished),
                comment.comment,
                ticket.finalNotificationSent,
                ticket.id,
                ticket.ticketNumber,
        ]

        expect == processingTimeStatisticsService.formatData(ticket)
    }

    private static List createOtrsTicketWithSeqTrack(Map otrsTicketProperties = [:], Map seqTrackProperties = [:]) {
        OtrsTicket ticket = DomainFactory.createOtrsTicket(otrsTicketProperties)
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(seqTrackProperties, [runSegment: runSegment])

        return [ticket, seqTrack]
    }

    private static List createOtrsTicketWithProject(Map otrsTicketProperties = [:], Map projectProperties = [:]) {
        Project project = DomainFactory.createProject(projectProperties)
        Individual individual = DomainFactory.createIndividual(project: project)
        Sample sample = DomainFactory.createSample(individual: individual)

        def (ticket, seqTrack) = createOtrsTicketWithSeqTrack(otrsTicketProperties, [sample: sample])

        return [ticket, project]
    }
}
