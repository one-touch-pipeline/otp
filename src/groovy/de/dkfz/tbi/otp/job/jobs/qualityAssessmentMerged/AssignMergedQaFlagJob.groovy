package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class AssignMergedQaFlagJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Override
    void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        qualityAssessmentMergedPassService.passFinished(pass)
        succeed()
    }
}
