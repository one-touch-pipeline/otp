package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*

class ParseRnaAlignmentQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob{

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Override
    void execute() throws Exception {
        final RnaRoddyBamFile rnaRoddyBamFile = getProcessParameterObject()

        RnaRoddyBamFile.withTransaction {
            abstractQualityAssessmentService.parseRnaRoddyBamFileQaStatistics(rnaRoddyBamFile)
            rnaRoddyBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            assert rnaRoddyBamFile.save(flush: true)
            succeed()
        }
    }

}
