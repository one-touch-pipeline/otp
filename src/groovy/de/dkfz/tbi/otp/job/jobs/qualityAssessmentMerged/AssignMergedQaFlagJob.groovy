package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class AssignMergedQaFlagJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        qualityAssessmentMergedPassService.passFinished(pass)
        succeed()
    }
}
