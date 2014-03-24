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

    String dataPath                  // path to data (ftp area)
    String mdPath                    // path to meta-data

    static belongsTo = [run : Run]
    static constraints = {
        dataPath()
        mdPath()
    }
}
