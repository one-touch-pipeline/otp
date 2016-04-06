package de.dkfz.tbi.otp.ngsdata

class MetaDataFile {

    String fileName
    String filePath
    boolean used = false
    Date dateCreated = null

    /**
     * Will only be filled by metadata import 2.0.
     * For metadata import 1.0 this field will stay null.
     */
    String md5sum

    static belongsTo = [
        runSegment : RunSegment
    ]
    static constraints = {
        fileName()
        filePath()
        runSegment()
        dateCreated()
        md5sum(nullable: true, matches: /^[0-9a-f]{32}$/)
    }

    static mapping = {
        runSegment index: "meta_data_file_run_segment_idx"
    }
}
