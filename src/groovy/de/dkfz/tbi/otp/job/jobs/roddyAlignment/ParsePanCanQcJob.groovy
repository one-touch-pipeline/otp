package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessmentService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import org.springframework.beans.factory.annotation.Autowired


class ParsePanCanQcJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    public void execute() {

        final RoddyBamFile roddyBamFile = getProcessParameterObject()

        // the following line is needed to avoid the following error,
        // which appears at least in the workflow test environment:
        // ERROR hibernate.AssertionFailure  - an assertion failure occured (
        // this may indicate a bug in Hibernate, but is more likely due to unsafe use of the session)
        // org.hibernate.AssertionFailure: collection [de.dkfz.tbi.otp.dataprocessing.RoddyBamFile.seqTracks]
        // was not processed by flush()
        roddyBamFile.containedSeqTracks

        executeRoddyCommandService.correctPermissions(roddyBamFile)

        RoddyBamFile.withTransaction {
            abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
            abstractQualityAssessmentService.parseRoddyBamFileQaStatistics(roddyBamFile)

            // Set the coverage value in roddyBamFile
            abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)
            roddyBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            assert roddyBamFile.save(flush: true)
            succeed()
        }
    }
}
