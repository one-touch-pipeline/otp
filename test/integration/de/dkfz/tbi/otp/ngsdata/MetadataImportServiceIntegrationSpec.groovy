package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.ngsdata.MetadataImportService.*

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.Md5sumFormatValidator
import grails.test.spock.IntegrationSpec

class MetadataImportServiceIntegrationSpec extends IntegrationSpec {

    @Autowired
    MetadataImportService metadataImportService

    void 'getSupportedDirectoryStructures returns map of directory structures'() {
        when:
        Map<String, String> directoryStructures = metadataImportService.supportedDirectoryStructures

        then:
        directoryStructures.get(AUTO_DETECT_DIRECTORY_STRUCTURE_NAME) == 'detect automatically'
        directoryStructures.get(DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME) == new DataFilesInSameDirectory().description
    }

    void 'getMetadataValidators returns MetadataValidators'() {
        expect:
        metadataImportService.metadataValidators.find { it instanceof Md5sumFormatValidator }
    }

    void 'getDirectoryStructure, when called with bean name, returns bean'() {
        expect:
        metadataImportService.getDirectoryStructure(DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME) instanceof DataFilesInSameDirectory
    }
}
