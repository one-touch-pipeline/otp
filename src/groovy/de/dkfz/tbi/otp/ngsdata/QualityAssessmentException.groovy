package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate that updating QualityAssessment pass has failed
 */
class QualityAssessmentException extends OtpException {

    public QualityAssessmentException(String msg) {
        super(msg)
    }
}
