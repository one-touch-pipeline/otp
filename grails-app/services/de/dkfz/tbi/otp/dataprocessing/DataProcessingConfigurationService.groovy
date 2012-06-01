package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This is the configuration service for data processing.
 */
class DataProcessingConfigurationService {

    def grailsApplication

    String getAlignmentReferenceIndexFileLocation() {
        String refIndex = grailsApplication.config.otp.dataprocessing.alignment.referenceIndex
        return refIndex
    }
}
