package de.dkfz.tbi.otp.ngsdata

import grails.test.spock.IntegrationSpec
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesInSameDirectory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.Md5sumFormatValidator

class MetadataImportServiceIntegrationSpec extends IntegrationSpec {

    @Autowired
    MetadataImportService metadataImportService

    void 'getSupportedDirectoryStructures returns map of directory structures'() {
        when:
        Map<String, String> directoryStructures = metadataImportService.supportedDirectoryStructures

        then:
        directoryStructures.get(MetadataImportService.AUTO_DETECT_DIRECTORY_STRUCTURE_NAME) == 'detect automatically'
        directoryStructures.get(MetadataImportService.DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME) == new DataFilesInSameDirectory().description
    }

    void 'getMetadataValidators returns MetadataValidators'() {
        expect:
        metadataImportService.metadataValidators.find { it instanceof Md5sumFormatValidator }
    }

    void 'getDirectoryStructure, when called with bean name, returns bean'() {
        expect:
        metadataImportService.getDirectoryStructure(MetadataImportService.DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME) instanceof DataFilesInSameDirectory
    }
}
