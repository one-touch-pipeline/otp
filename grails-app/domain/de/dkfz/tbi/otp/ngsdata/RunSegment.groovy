package de.dkfz.tbi.otp.ngsdata

class RunSegment {

    enum Status {NEW, BLOCKED, PROCESSING, COMPLETE}
    Status metaDataStatus = Status.NEW
    boolean allFilesUsed = false           // all files find relations

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
    FilesStatus filesStatus

    enum DataFormat {FILES_IN_DIRECTORY, TAR, TAR_IN_DIRECTORY}
    DataFormat initialFormat
    DataFormat currentFormat

    String dataPath                  // path to data (ftp area)
    String mdPath                    // path to meta-data

    static belongsTo = [run : Run]
    static constraints = {
        dataPath()
        mdPath()
    }
}
