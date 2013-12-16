package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

/**
 */
class AssignQaFlagJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        qualityAssessmentPassService.assertNumberOfReadsIsTheSameAsCalculatedWithFastqc(pass)
        qualityAssessmentPassService.passFinished(pass)
        succeed()
    }
}
