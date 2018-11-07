package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

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
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThreshold(rnaRoddyBamFile, rnaQa)
            assert rnaRoddyBamFile.save(flush: true)
            succeed()
        }
    }

}
