package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import org.springframework.stereotype.Component

import de.dkfz.tbi.util.spreadsheet.validation.Validator

/**
 * Validators implementing this interfaces and annotated with {@link Component} will automatically be used for
 * validating meta data files.
 */
interface MetadataValidator extends Validator<MetadataValidationContext> {

    /**
     * @return A collection of descriptions of the validations which this validator performs
     */
    Collection<String> getDescriptions()
}
