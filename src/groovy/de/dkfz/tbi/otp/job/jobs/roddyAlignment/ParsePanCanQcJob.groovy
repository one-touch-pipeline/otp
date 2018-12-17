package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyQualityAssessment
import de.dkfz.tbi.otp.job.ast.UseJobLog

@Component
@Scope("prototype")
@UseJobLog
class ParsePanCanQcJob extends AbstractParseAlignmentQcJob {

    @Override
    RoddyQualityAssessment parseStatistics(RoddyBamFile roddyBamFile) {
        abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
        return abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
    }
}
