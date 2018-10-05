package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate that updating QualityAssessment pass has failed
 */
class QualityAssessmentException extends OtpException {

    QualityAssessmentException(String msg) {
        super(msg)
    }
}
