package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import org.joda.time.format.*
import org.springframework.security.access.prepost.*

import javax.sql.*
import java.sql.*
import java.text.*
import java.util.Date

class ProcessingTimeStatisticsService {

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm"

    DataSource dataSource

    public List<OtrsTicket> findAllOtrsTicketsByDateBetweenAndSearch(LocalDate dateFrom, LocalDate dateTo, String search) {
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
   dataFile.runSegment.otrsTicket = otrsTicket AND (
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
            dateTo: new Timestamp(dateTo.plusDays(1).toDateTimeAtStartOfDay().getMillis())
        ]

        if (search) {
            queryOptions.put('search', '%' + StringUtils.escapeForSqlLike(search).toLowerCase(Locale.ENGLISH) + '%')
        }

        List<OtrsTicket> tickets = OtrsTicket.executeQuery(query.toString(), queryOptions)

        return tickets
    }

    public List formatData(OtrsTicket ticket) {
        assert ticket : "No OTRS ticket defined."

        List<SeqTrack> seqTracks = ticket.findAllSeqTracks() as List

        List<String> ilseIds = selectDistinctAndOrderByFromSeqTrack(seqTracks, "st.ilseSubmission.ilseNumber")
        List<String> projectNames = selectDistinctAndOrderByFromSeqTrack(seqTracks, "st.sample.individual.project.name")
        List<String> sampleNames = selectDistinctAndOrderByFromSeqTrack(seqTracks, ["st.sample.individual.mockFullName", "st.sample.sampleType.name"].join(", ")).collect { "${it.first()} ${it.last()}" }
        List<String> runs = selectDistinctAndOrderByFromSeqTrack(seqTracks, "st.run.name")

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
        (OtrsTicket.ProcessingStep.values() - [OtrsTicket.ProcessingStep.INDEL, OtrsTicket.ProcessingStep.ACESEQ]).each {
            if (previousProcessingStep) {
                data << getFormattedPeriod(ticket."${previousProcessingStep}Finished", ticket."${it}Started")
            } else {
                data << getFormattedPeriod(ticket.ticketCreated, ticket."${it}Started")
            }
            data << ticket."${it.toString().toLowerCase()}Started"?.format(DATE_FORMAT)
            data << getFormattedPeriod(ticket."${it}Started", ticket."${it}Finished")
            data << ticket."${it.toString().toLowerCase()}Finished"?.format(DATE_FORMAT)
            previousProcessingStep = it
        }

        data << getFormattedPeriod(ticket.installationStarted, ticket.snvFinished ?: ticket.alignmentFinished ?: ticket.fastqcFinished ?: ticket.installationFinished)

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

    private static List selectDistinctAndOrderByFromSeqTrack(List seqTracks, String property) {
        return SeqTrack.executeQuery("SELECT DISTINCT ${property} FROM SeqTrack AS st WHERE st IN (:seqTracks) ORDER BY ${property}", [seqTracks: seqTracks]) as List<String>
    }

    public static String getFormattedPeriod(Date d1, Date d2) {
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
    public void editTimestampProperty(OtrsTicket ticket, String property, String dateString) {
        if (dateString == "") {
            ticket.submissionReceivedNotice = null
        } else {
            DateFormat format = new SimpleDateFormat(DATE_FORMAT);
            Date date = format.parse(dateString)

            ticket."${property}" = date
        }
        assert ticket.save(flush: true)
    }
}
