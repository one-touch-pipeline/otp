package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.*
import org.springframework.beans.factory.annotation.*
import spock.lang.*

class BamMetadataImportServiceIntegrationSpec extends Specification {

    @Autowired
    BamMetadataImportService bamMetadataImportService

    void 'getBamMetadataValidators returns BamMetadataValidators'() {

        expect:
        bamMetadataImportService.bamMetadataValidators.find { it instanceof Md5sumFormatValidator }
    }
}