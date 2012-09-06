package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

class SavingException extends ProcessingException {
    public SavingException(String objectName) {
        super("Object could not be saved: ${objectName}")
    }
}
