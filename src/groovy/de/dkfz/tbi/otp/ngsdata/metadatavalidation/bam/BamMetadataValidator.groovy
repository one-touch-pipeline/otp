package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam

import org.springframework.stereotype.Component

import de.dkfz.tbi.util.spreadsheet.validation.Validator

/**
 * Validators implementing this interfaces and annotated with {@link Component} will automatically be used for
 * validating externally processed merged bam meta data files.
 */
interface BamMetadataValidator extends Validator<BamMetadataValidationContext> {

    /**
     * @return A collection of descriptions of the validations which this validator performs
     */
    Collection<String> getDescriptions()
}