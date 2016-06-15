package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity
import org.joda.time.*
import org.joda.time.format.*

class OtrsTicket implements Commentable, Entity {

    enum ProcessingStep {
        INSTALLATION,
        FASTQC,
        ALIGNMENT,
        SNV

        public String toString() {
            return name().toLowerCase(Locale.ENGLISH)
        }
    }

    String ticketNumber

    Date dateCreated

    Date installationStarted
    Date installationFinished

    Date fastqcStarted
    Date fastqcFinished

    Date alignmentStarted
    Date alignmentFinished

    Date snvStarted
    Date snvFinished

    boolean finalNotificationSent = false

    Comment comment


    static constraints = {
        ticketNumber(nullable: false, unique: true, validator: { val, obj ->
            return ticketNumberConstraint(val) ?: true
        })

        installationStarted(nullable: true)
        installationFinished(nullable: true)

        fastqcStarted(nullable: true)
        fastqcFinished(nullable: true)

        alignmentStarted(nullable: true)
        alignmentFinished(nullable: true)

        snvStarted(nullable: true)
        snvFinished(nullable: true)

        comment(nullable: true)
    }

    Date getFirstImportTimestamp() {
        //Doesn't work as a single Query, probably a Unit test problem
        def runSegment = RunSegment.withCriteria {
            eq ('otrsTicket', this)
        }
        return (Date) MetaDataFile.createCriteria().get {
            'in'('runSegment', runSegment)
            projections {
                min("dateCreated")
            }
        }
    }

    Date getLastImportTimestamp() {
        //Doesn't work as a single Query, probably a Unit test problem
        def runSegment = RunSegment.withCriteria {
            eq ('otrsTicket', this)
        }
        return (Date) MetaDataFile.createCriteria().get {
            'in'('runSegment', runSegment)
            projections {
                max("dateCreated")
            }
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
}
