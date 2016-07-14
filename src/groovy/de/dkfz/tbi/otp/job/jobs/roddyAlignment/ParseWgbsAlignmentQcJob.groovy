package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile

class ParseWgbsAlignmentQcJob extends AbstractParseAlignmentQcJob {

    public void parseStatistics(RoddyBamFile roddyBamFile) {
        abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
        abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
        if (roddyBamFile.getContainedSeqTracks()*.normalizedLibraryName.unique().size() > 1) {
            abstractQualityAssessmentService.parseRoddyLibraryQaStatistics(roddyBamFile)
        }
    }
}
