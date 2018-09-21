package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.Entity

/**
 * An import of one or more {@linkplain MetaDataFile}s and {@linkplain DataFile}s.
 *
 * <p>
 * Named "RunSegment" for historical reasons only.
 */
class RunSegment implements Entity {


    /**
     * This flag specifies if the lanes, which are in this {@link RunSegment} shall be aligned automatically.
     * Per default they shall be aligned.
     */
    Boolean align = true

    OtrsTicket otrsTicket

    enum ImportMode {
        MANUAL,
        AUTOMATIC
    }
    ImportMode importMode

    static constraints = {
        //the field can be null, since for the old data the information is not needed; only for new incoming runSegments
        align(nullable: true)
        otrsTicket(nullable: true)
    }

    static mapping = {
        otrsTicket index: "run_segment_otrs_ticket_idx"
    }
}
