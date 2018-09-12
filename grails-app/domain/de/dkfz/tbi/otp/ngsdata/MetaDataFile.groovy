package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.Entity

class MetaDataFile implements Entity {


    String fileName
    String filePath
    Date dateCreated = null

    /**
     * Will only be filled by metadata import 2.0.
     * For metadata import 1.0 this field will stay null.
     */
    String md5sum

    static belongsTo = [
        runSegment : RunSegment,
    ]
    static constraints = {
        fileName(validator: { OtpPath.isValidPathComponent(it) })
        filePath(validator: { OtpPath.isValidAbsolutePath(it) })
        runSegment()
        dateCreated()
        md5sum(nullable: true, matches: /^[0-9a-f]{32}$/)
    }

    static mapping = {
        runSegment index: "meta_data_file_run_segment_idx"
    }
}
