package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import groovy.transform.*
import org.joda.time.*
import org.joda.time.format.*

class OtrsTicket implements Commentable, Entity {

    @TupleConstructor
    enum ProcessingStep {
        INSTALLATION('installation', 'installed', null),
        FASTQC('FastQC', null, INSTALLATION),
        ALIGNMENT('alignment', 'aligned', INSTALLATION),
        SNV('SNV calling', 'SNV-called', ALIGNMENT),
        INDEL('Indel calling', 'Indel-called', ALIGNMENT),
        SOPHIA('SV calling', 'SV-called', ALIGNMENT),
        ACESEQ('CNV calling', 'CNV-called', SOPHIA),
        RUN_YAPSA('YAPSA signature analysis', 'YAPSA ran', SNV),

        final String displayName
        /**
         * Will be used in the subject of notification e-mails: "sequencing data ${notificationSubject}"
         *
         * {@code null} means that no notification shall be sent for this step
         */
        final String notificationSubject

        final ProcessingStep dependsOn

        /**
         * Converts UPPER_SNAKE_CASE "ENUM_NAMES" to camelCase "fieldNames".
         *
         * This is needed by the {@link TrackingService},
         * which does some stringly-typed magic to dynamically determine which field to set.
         */
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH).replaceAll( "(_)([a-z0-9])", { it -> it[2].toUpperCase() } )
        }
    }

    String ticketNumber

    /**
     * Timestamp of the earliest "Submission Received Notice" mail for an ILSe submission belonging to this ticket.
     */
    Date submissionReceivedNotice
    /**
     * Timestamp when the ticket gets created in OTRS.
     */
    Date ticketCreated

    Date dateCreated

    Date installationStarted
    Date installationFinished

    Date fastqcStarted
    Date fastqcFinished

    Date alignmentStarted
    Date alignmentFinished

    Date snvStarted
    Date snvFinished

    Date indelStarted
    Date indelFinished

    Date sophiaStarted
    Date sophiaFinished

    Date aceseqStarted
    Date aceseqFinished

    Date runYapsaStarted
    Date runYapsaFinished

    boolean finalNotificationSent = false
    boolean automaticNotification = true

    Comment comment

    String seqCenterComment


    static constraints = {
        ticketNumber(nullable: false, unique: true, validator: { val, obj ->
            return ticketNumberConstraint(val) ?: true
        })

        submissionReceivedNotice(nullable: true)
        ticketCreated(nullable: true)

        installationStarted(nullable: true)
        installationFinished(nullable: true)

        fastqcStarted(nullable: true)
        fastqcFinished(nullable: true)

        alignmentStarted(nullable: true)
        alignmentFinished(nullable: true)

        snvStarted(nullable: true)
        snvFinished(nullable: true)

        indelStarted(nullable: true)
        indelFinished(nullable: true)

        sophiaStarted(nullable: true)
        sophiaFinished(nullable: true)

        aceseqStarted(nullable: true)
        aceseqFinished(nullable: true)

        runYapsaStarted(nullable: true)
        runYapsaFinished(nullable: true)

        comment(nullable: true)
        seqCenterComment(nullable: true)
    }

    static mapping = {
        seqCenterComment type: "text"
    }



    Date getFirstImportTimestamp() {
        return (Date) MetaDataFile.createCriteria().get {
            'in'('runSegment', runSegments)
            projections {
                min("dateCreated")
            }
        }
    }

    Date getLastImportTimestamp() {
        return (Date) MetaDataFile.createCriteria().get {
            'in'('runSegment', runSegments)
            projections {
                max("dateCreated")
            }
        }
    }

    List<RunSegment> getRunSegments() {
        //Doesn't work as a single Query, probably a Unit test problem
        return RunSegment.withCriteria {
            eq ('otrsTicket', this)
        }
    }

    Set<SeqTrack> findAllSeqTracks() {
        return new LinkedHashSet<SeqTrack>(SeqTrack.findAll(
                'FROM SeqTrack st WHERE EXISTS (FROM DataFile df WHERE df.seqTrack = st AND df.runSegment.otrsTicket = :otrsTicket)',
                [otrsTicket: this]
        ))
    }

    static String ticketNumberConstraint(String val) {
        if (val =~ /^[0-9]{16}$/) {
            try {
                DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMdd")
                format.parseDateTime(val.substring(0,8))
                return null
            } catch (IllegalFieldValueException e) {
                return e.message
            }
        } else {
            return "does not match the required pattern"
        }
    }

    String getUrl() {
        return "${ProcessingOptionService.getValueOfProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_URL)}/index.pl?Action=AgentTicketZoom;TicketNumber=${ticketNumber}"
    }

    String toString() {
        return "OtrsTicket ${id}: #${ticketNumber}"
    }

    @Override
    Project getProject()  {
        return null
    }
}
