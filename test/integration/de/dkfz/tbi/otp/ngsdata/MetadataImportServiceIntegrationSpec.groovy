/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
