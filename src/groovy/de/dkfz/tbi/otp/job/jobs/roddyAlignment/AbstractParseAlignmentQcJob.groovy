package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue

abstract class AbstractParseAlignmentQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() {

        final RoddyBamFile roddyBamFile = getProcessParameterObject()

        // the following line is needed to avoid the following error,
        // which appears at least in the workflow test environment:
        // ERROR hibernate.AssertionFailure  - an assertion failure occured (
        // this may indicate a bug in Hibernate, but is more likely due to unsafe use of the session)
        // org.hibernate.AssertionFailure: collection [de.dkfz.tbi.otp.dataprocessing.RoddyBamFile.seqTracks]
        // was not processed by flush()
        roddyBamFile.containedSeqTracks

        RoddyBamFile.withTransaction {
            RoddyQualityAssessment qa = parseStatistics(roddyBamFile)
            // Set the coverage value in roddyBamFile
            abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)
            roddyBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(roddyBamFile, (QcTrafficLightValue) qa)
            assert roddyBamFile.save(flush: true)
            succeed()
        }
    }

    abstract RoddyQualityAssessment parseStatistics(RoddyBamFile roddyBamFile)
}
