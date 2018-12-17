package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyMergedBamQa
import de.dkfz.tbi.otp.job.ast.UseJobLog

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
