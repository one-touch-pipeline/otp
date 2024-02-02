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

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.util.TimeFormats

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ProcessingTimeStatisticsController {

    ProcessingTimeStatisticsService processingTimeStatisticsService
    CommentService commentService

    static allowedMethods = [
            index          : "GET",
            dataTableSource: "POST",
            editValue      : "POST",
    ]

    def index() {
        return [
                dateFrom: TimeFormats.DATE.getFormattedDate(Date.from(Instant.now().minus(6, ChronoUnit.DAYS))),
                dateTo  : TimeFormats.DATE.getFormattedLocalDate(LocalDate.now()),
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = Ticket.count()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        LocalDate dateFrom = LocalDate.parse(params.from)
        LocalDate dateTo = LocalDate.parse(params.to)

        List<Ticket> tickets = processingTimeStatisticsService.findAllTicketsByDateBetweenAndSearch(dateFrom, dateTo, cmd.sSearch)

        dataToRender.aaData = tickets.collect { Ticket ticket ->
            return processingTimeStatisticsService.formatData(ticket)
        }

        render(dataToRender as JSON)
    }

    def editValue() {
        Map data = [:]

        Ticket ticket = Ticket.get(params.id as long)
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
        render(data as JSON)
    }
}
