package de.dkfz.tbi.otp.dataprocessing

class AbstractBamFile {

    enum BamType {
        SORTED,
        RMDUP
    }

    enum StatsFileStatus {
        NOT_EXISTING,
        EXISTING,
        UPLOADED
    }

    BamType type = null
    boolean hasIndexFile = false
    StatsFileStatus flagstats = StatsFileStatus.NOT_EXISTING
    StatsFileStatus coverageStats = StatsFileStatus.NOT_EXISTING
    StatsFileStatus insertSizeStats = StatsFileStatus.NOT_EXISTING

    double insertSizeMean
    double insertSizeRMS
}
