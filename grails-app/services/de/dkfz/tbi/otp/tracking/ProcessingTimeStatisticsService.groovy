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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.NotSupportedException
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate

@CompileDynamic
@Transactional
class ProcessingTimeStatisticsService {

    TicketService ticketService

    List<Ticket> findAllTicketsByDateBetweenAndSearch(LocalDate dateFrom, LocalDate dateTo, String search) {
        assert dateFrom: "No date 'from' is defined."
        assert dateTo: "No date 'to' is defined."

        if (search?.length() > 0 && search?.length() < 3) {
            throw new NotSupportedException("String to search for is too short - at least three characters are required.")
        }

        String query = """
FROM Ticket ticket
 WHERE
${search ? """
  (EXISTS (FROM RawSequenceFile sequenceFile WHERE
   sequenceFile.fastqImportInstance.ticket = ticket AND (
    lower(sequenceFile.seqTrack.sample.individual.project.name) LIKE :search OR
    str(sequenceFile.seqTrack.ilseSubmission.ilseNumber) LIKE :search OR
    lower(sequenceFile.seqTrack.run.name) LIKE :search
   )
  ) OR (ticket.ticketNumber LIKE :search)
 ) AND
""" : ""
        }
 ticket.dateCreated >= :dateFrom AND
 ticket.dateCreated < :dateTo
 ORDER BY ticket.dateCreated DESC
"""

        Map<String, ?> queryOptions = [
                dateFrom: TimeUtils.toDate(dateFrom),
                dateTo  : TimeUtils.toDate(dateTo.plusDays(1)),
        ]

        if (search) {
            queryOptions.put('search', '%' + StringUtils.escapeForSqlLike(search).toLowerCase(Locale.ENGLISH) + '%')
        }

        List<Ticket> tickets = Ticket.executeQuery(query.toString(), queryOptions)

        return tickets
    }

    List formatData(Ticket ticket) {
        assert ticket: "No ticket defined."

        List<SeqTrack> seqTracks = ticketService.findAllSeqTracks(ticket) as List
        List<String> ilseIds = seqTracks.collect { it.ilseSubmission?.ilseNumber as String }.unique().sort()
        List<String> projectNames = seqTracks.collect { it.sample.individual.project.name }.unique().sort()
        List<String> sampleNames = seqTracks.collect { "${it.sample.individual.pid} ${it.sample.sampleType.name}" }.unique().sort()
        List<String> runs = seqTracks.collect { it.run.name }.unique().sort()

        List data = [
                ticketService.buildTicketDirectLink(ticket),
                ilseIds ?: ['none'],
                projectNames,
                runs,
                sampleNames,
                seqTracks.collect { return "${it.run}, lane: ${it.laneId}" },
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.submissionReceivedNotice),
                TimeUtils.getFormattedDurationWithDays(ticket.submissionReceivedNotice as Date, ticket.ticketCreated as Date),
                TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket.ticketCreated),
        ]

        String previousProcessingStep
        [
                Ticket.ProcessingStep.INSTALLATION,
                Ticket.ProcessingStep.FASTQC,
                Ticket.ProcessingStep.ALIGNMENT,
                Ticket.ProcessingStep.SNV,
        ].each {
            if (previousProcessingStep) {
                data << TimeUtils.getFormattedDurationWithDays(ticket."${previousProcessingStep}Finished" as Date, ticket."${it}Started" as Date)
            } else {
                data << TimeUtils.getFormattedDurationWithDays(ticket.ticketCreated as Date, ticket."${it}Started" as Date)
            }
            data << TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket."${it}Started" as Date)
            data << TimeUtils.getFormattedDurationWithDays(ticket."${it}Started" as Date, ticket."${it}Finished" as Date)
            data << TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(ticket."${it}Finished" as Date)
            previousProcessingStep = it
        }

        data << TimeUtils.getFormattedDurationWithDays(ticket.installationStarted as Date,
                (ticket.snvFinished ?: ticket.alignmentFinished ?: ticket.fastqcFinished ?: ticket.installationFinished) as Date)

        if (ticket.comment?.comment) {
            data << ticket.comment.comment
        } else {
            data << ""
        }

        data << ticket.finalNotificationSent
        data << ticket.id
        data << ticket.ticketNumber

        return data
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void editTimestampProperty(Ticket ticket, String property, String dateString) {
        if (dateString == "") {
            ticket."${property}" = null
        } else {
            DateFormat format = new SimpleDateFormat(TimeFormats.DATE_TIME_WITHOUT_SECONDS.format, Locale.ENGLISH)
            Date date = format.parse(dateString)

            ticket."${property}" = date
        }
        assert ticket.save(flush: true)
    }
}
