package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import org.joda.time.format.*

class OtrsTicket implements Commentable {

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
        ticketNumber(nullable: false, unique: true, matches: /^[0-9]{16}$/, validator: { val, obj ->
            try {
                DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMdd")
                format.parseDateTime(val.substring(0,8))
                return true
            } catch (IllegalFieldValueException e) {
                return e.message
            }
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

    Date getImportDate() {
        //Doesn't work as a single Query, probably a Unit test problem
        def runSegment = RunSegment.withCriteria {
            eq ('otrsTicket', this)
        }
        return MetaDataFile.createCriteria().get {
            'in'('runSegment', runSegment)
            projections {
                min("dateCreated")
            }
        }
    }
}
