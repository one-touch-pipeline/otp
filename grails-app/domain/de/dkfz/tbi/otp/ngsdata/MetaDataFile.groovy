package de.dkfz.tbi.otp.ngsdata

class MetaDataFile {

    String fileName
    String filePath
    boolean used = false
    Date dateCreated = null

    static belongsTo = [
        runSegment : RunSegment
    ]
    static constraints = {
        fileName()
        filePath()
        runSegment()
        dateCreated()
    }

    static mapping = {
        runSegment index: "meta_data_file_run_segment_idx"
    }
}
