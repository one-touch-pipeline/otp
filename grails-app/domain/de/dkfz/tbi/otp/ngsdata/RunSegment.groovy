package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

/**
 * An import of one or more {@linkplain MetaDataFile}s and {@linkplain DataFile}s.
 *
 * <p>
 * Named "RunSegment" for historical reasons only.
 */
class RunSegment implements Entity {

    boolean allFilesUsed = false           // all files find relations


    /**
     * This flag specifies if the lanes, which are in this {@link RunSegment} shall be aligned automatically.
     * Per default they shall be aligned.
     */
    Boolean align = true

    enum FilesStatus {
        NEEDS_UNPACK,
        PROCESSING_UNPACK,
        NEEDS_INSTALLATION,
        PROCESSING_INSTALLATION,
        NEEDS_CHECKING,
        PROCESSING_CHECKING,
        FILES_CORRECT,
        FILES_MISSING
    }

    /**
     * Collection of all {@link FilesStatus}es which indicate that a {@link RunSegment} is being
     * processed by the data installation workflow.
     */
    static final Collection<FilesStatus> PROCESSING_FILE_STATUSES = [
        FilesStatus.PROCESSING_INSTALLATION,
        FilesStatus.NEEDS_CHECKING,
        FilesStatus.PROCESSING_CHECKING,
        FilesStatus.FILES_MISSING,
    ].asImmutable()

    FilesStatus filesStatus

    enum DataFormat {FILES_IN_DIRECTORY, TAR, TAR_IN_DIRECTORY}
    DataFormat initialFormat
    DataFormat currentFormat

    /** @deprecated Use {@link MetaDataFile#filePath} */ @Deprecated
    String mdPath                    // path to meta-data

    OtrsTicket otrsTicket

    static constraints = {
        mdPath blank: false, validator: { OtpPath.isValidAbsolutePath(it) }
        //the field can be null, since for the old data the information is not needed; only for new incoming runSegments
        align(nullable: true)
        otrsTicket(nullable: true)
    }

    static mapping = {
        otrsTicket index: "run_segment_otrs_ticket_idx"
    }
}
