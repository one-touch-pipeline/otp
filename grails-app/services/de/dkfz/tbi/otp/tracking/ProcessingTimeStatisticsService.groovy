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

import grails.gorm.transactions.Transactional
import org.joda.time.*
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.StringUtils

import javax.sql.DataSource
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

@Transactional
class ProcessingTimeStatisticsService {

    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm"

    DataSource dataSource

    List<OtrsTicket> findAllOtrsTicketsByDateBetweenAndSearch(LocalDate dateFrom, LocalDate dateTo, String search) {
        assert dateFrom : "No date 'from' is defined."
        assert dateTo : "No date 'to' is defined."

        if (search?.length() > 0 && search?.length() < 3) {
            throw new RuntimeException("String to search for is too short - at least three characters are required.")
        }

        String query = """
FROM OtrsTicket otrsTicket
 WHERE
${search ? """
  (EXISTS (FROM DataFile dataFile WHERE
   dataFile.fastqImportInstance.otrsTicket = otrsTicket AND (
    lower(dataFile.seqTrack.sample.individual.project.name) LIKE :search OR
    str(dataFile.seqTrack.ilseSubmission.ilseNumber) LIKE :search OR
    lower(dataFile.seqTrack.run.name) LIKE :search
   )
  ) OR (otrsTicket.ticketNumber LIKE :search)
 ) AND
""" : ""
}
 otrsTicket.dateCreated >= :dateFrom AND
 otrsTicket.dateCreated < :dateTo
 ORDER BY otrsTicket.dateCreated DESC
"""

        Map queryOptions = [
            dateFrom: new Timestamp(dateFrom.toDateTimeAtStartOfDay().getMillis()),
            dateTo: new Timestamp(dateTo.plusDays(1).toDateTimeAtStartOfDay().getMillis()),
        ]

        if (search) {
            queryOptions.put('search', '%' + StringUtils.escapeForSqlLike(search).toLowerCase(Locale.ENGLISH) + '%')
        }

        List<OtrsTicket> tickets = OtrsTicket.executeQuery(query.toString(), queryOptions)

        return tickets
    }

    List formatData(OtrsTicket ticket) {
        assert ticket : "No OTRS ticket defined."

        List<SeqTrack> seqTracks = ticket.findAllSeqTracks() as List
        List<String> ilseIds =      seqTracks.collect { it.ilseSubmission?.ilseNumber as String }.unique().sort()
        List<String> projectNames = seqTracks.collect { it.sample.individual.project.name }.unique().sort()
        List<String> sampleNames =  seqTracks.collect { "${it.sample.individual.mockFullName} ${it.sample.sampleType.name}" }.unique().sort()
        List<String> runs =         seqTracks.collect { it.run.name }.unique().sort()

        List data = [
                ticket.url,
                ilseIds ?: ['none'],
                projectNames,
                runs,
                sampleNames,
                seqTracks.collect { return "${it.run}, lane: ${it.laneId}" },
                ticket.submissionReceivedNotice?.format(DATE_FORMAT) ?: "",
                getFormattedPeriod(ticket.submissionReceivedNotice, ticket.ticketCreated),
                ticket.ticketCreated?.format(DATE_FORMAT) ?: "",
        ]

        String previousProcessingStep
        [
            OtrsTicket.ProcessingStep.INSTALLATION,
            OtrsTicket.ProcessingStep.FASTQC,
            OtrsTicket.ProcessingStep.ALIGNMENT,
            OtrsTicket.ProcessingStep.SNV,
        ].each {
            if (previousProcessingStep) {
                data << getFormattedPeriod(ticket."${previousProcessingStep}Finished", ticket."${it}Started")
            } else {
                data << getFormattedPeriod(ticket.ticketCreated, ticket."${it}Started")
            }
            data << ticket."${it}Started"?.format(DATE_FORMAT)
            data << getFormattedPeriod(ticket."${it}Started", ticket."${it}Finished")
            data << ticket."${it}Finished"?.format(DATE_FORMAT)
            previousProcessingStep = it
        }

        data << getFormattedPeriod(ticket.installationStarted, ticket.snvFinished ?: ticket.alignmentFinished ?: ticket.fastqcFinished ?:
                ticket.installationFinished)

        if (ticket.comment?.comment) {
            data << ticket.comment.comment
        } else  {
            data << ""
        }

        data << ticket.finalNotificationSent
        data << ticket.id
        data << ticket.ticketNumber

        return data
    }

    static String getFormattedPeriod(Date d1, Date d2) {
        if (!d1 || !d2) {
            return ""
        }

        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(2)
                .appendDays()
                .appendSeparator("d ")
                .appendHours()
                .appendSeparator("h ")
                .appendMinutes()
                .appendSuffix("m")
                .toFormatter()

        Period period = new Period(new DateTime(d1), new DateTime(d2), PeriodType.dayTime())
        String periodString = formatter.print(period)

        if (periodString.contains("-")) {
            return "-${periodString.replace("-", "")}"
        }
        return periodString
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void editTimestampProperty(OtrsTicket ticket, String property, String dateString) {
        if (dateString == "") {
            ticket."${property}" = null
        } else {
            DateFormat format = new SimpleDateFormat(DATE_FORMAT)
            Date date = format.parse(dateString)

            ticket."${property}" = date
        }
        assert ticket.save(flush: true)
    }
}
