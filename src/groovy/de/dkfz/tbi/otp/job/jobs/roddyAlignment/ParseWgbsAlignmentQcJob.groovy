package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class ParseWgbsAlignmentQcJob extends AbstractParseAlignmentQcJob {

    @Override
    RoddyMergedBamQa parseStatistics(RoddyBamFile roddyBamFile) {
        abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
        if (roddyBamFile.getContainedSeqTracks()*.getLibraryDirectoryName().unique().size() > 1) {
            abstractQualityAssessmentService.parseRoddyLibraryQaStatistics(roddyBamFile)
        }
        return abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
    }
}
