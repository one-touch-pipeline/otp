package de.dkfz.tbi.otp.ngsdata

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.Md5sumFormatValidator

class BamMetadataImportServiceIntegrationSpec extends Specification {

    @Autowired
    BamMetadataImportService bamMetadataImportService

    void 'getBamMetadataValidators returns BamMetadataValidators'() {
        expect:
        bamMetadataImportService.bamMetadataValidators.find { it instanceof Md5sumFormatValidator }
    }
}