package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException

class SavingException extends ProcessingException {
    SavingException(String objectName) {
        super("Object could not be saved: ${objectName}")
    }
}
