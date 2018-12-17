package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService

@Component
@Scope("prototype")
@UseJobLog
class ParseRnaAlignmentQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() throws Exception {
        final RnaRoddyBamFile rnaRoddyBamFile = getProcessParameterObject()

        RnaRoddyBamFile.withTransaction {
            RnaQualityAssessment rnaQa = abstractQualityAssessmentService.parseRnaRoddyBamFileQaStatistics(rnaRoddyBamFile)
            rnaRoddyBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(rnaRoddyBamFile, rnaQa)
            assert rnaRoddyBamFile.save(flush: true)
            succeed()
        }
    }

}
