package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessmentService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired


class ParsePanCanQcJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    public void execute() {

        final RoddyBamFile roddyBamFile = getProcessParameterObject()
        RoddyBamFile.withTransaction {
            abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
            abstractQualityAssessmentService.parseRoddyBamFileQaStatistics(roddyBamFile)

            // Set the coverage value in roddyBamFile
            abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)

            succeed()
        }
    }
}
