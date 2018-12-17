package de.dkfz.tbi.otp.tracking

import grails.converters.JSON
import org.joda.time.LocalDate

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProcessingTimeStatisticsController {

    ProcessingTimeStatisticsService processingTimeStatisticsService
    CommentService commentService

    def index() { }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = OtrsTicket.count()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        LocalDate dateFrom = LocalDate.parse(params.from)
        LocalDate dateTo = LocalDate.parse(params.to)

        List<OtrsTicket> tickets = processingTimeStatisticsService.findAllOtrsTicketsByDateBetweenAndSearch(dateFrom, dateTo, cmd.sSearch)

        dataToRender.aaData = tickets.collect { OtrsTicket ticket ->
            return processingTimeStatisticsService.formatData(ticket)
        }

        render dataToRender as JSON
    }

    def editValue() {
        Map data = [:]

        OtrsTicket ticket = OtrsTicket.get(params.id as long)
        assert ticket

        switch (params.type) {
            case 'submissionReceivedNotice':
            case 'ticketCreated':
                processingTimeStatisticsService.editTimestampProperty(ticket, params.type, params.value)
                break
            case 'comment':
                commentService.saveComment(ticket, params.value)
                break
        }

        data.value = params.value
        render data as JSON
    }
}
