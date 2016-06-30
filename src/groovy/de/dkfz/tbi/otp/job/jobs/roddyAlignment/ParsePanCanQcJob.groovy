package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile

class ParsePanCanQcJob extends AbstractParseAlignmentQcJob {

    public void parseStatistics(RoddyBamFile roddyBamFile) {
        abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
        abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
    }
}
