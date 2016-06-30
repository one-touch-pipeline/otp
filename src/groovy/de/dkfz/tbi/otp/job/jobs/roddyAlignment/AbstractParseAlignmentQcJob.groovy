package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessmentService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.AutoRestartable
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired


abstract class AbstractParseAlignmentQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartable {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    public void execute() {

        final RoddyBamFile roddyBamFile = getProcessParameterObject()

        // the following line is needed to avoid the following error,
        // which appears at least in the workflow test environment:
        // ERROR hibernate.AssertionFailure  - an assertion failure occured (
        // this may indicate a bug in Hibernate, but is more likely due to unsafe use of the session)
        // org.hibernate.AssertionFailure: collection [de.dkfz.tbi.otp.dataprocessing.RoddyBamFile.seqTracks]
        // was not processed by flush()
        roddyBamFile.containedSeqTracks

        RoddyBamFile.withTransaction {
            parseStatistics(roddyBamFile)
            // Set the coverage value in roddyBamFile
            abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)
            roddyBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            assert roddyBamFile.save(flush: true)
            succeed()
        }
    }

    abstract void  parseStatistics(RoddyBamFile roddyBamFile)
}
