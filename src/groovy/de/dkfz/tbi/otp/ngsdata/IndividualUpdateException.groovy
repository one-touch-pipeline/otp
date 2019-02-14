package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate that updating an Individual failed.
 */
class IndividualUpdateException extends OtpException {
    Individual individual

    IndividualUpdateException(Individual individual) {
        super("Updating the Individual with id ${individual.id} failed")
        this.individual = individual
    }

}
