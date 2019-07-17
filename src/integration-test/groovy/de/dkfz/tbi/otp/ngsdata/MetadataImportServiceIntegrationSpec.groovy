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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesInSameDirectory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.Md5sumFormatValidator
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.MailHelperService

@Rollback
@Integration
class MetadataImportServiceIntegrationSpec extends Specification {

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

    void "test notifyAboutUnsetConfig"() {
        given:
        MetadataImportService service = Spy(MetadataImportService) {
            getSeqTracksWithConfiguredAlignment(_) >> null
        }
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        SeqTrack st1 = DomainFactory.createSeqTrack()
        SeqTrack st2 = DomainFactory.createSeqTrack()

        service.sampleTypeService = Mock(SampleTypeService) {
            getSeqTracksWithoutSampleCategory(_) >> [st1]
        }
        service.processingThresholdsService = Mock(ProcessingThresholdsService) {
            getSeqTracksWithoutProcessingThreshold(_) >> [st2]
        }
        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _)  >> { String subject, String body, String recipient ->
                assert subject.contains("threshold")
                assert subject.contains("category")
                assert body.contains(st1.sampleType.displayName)
                assert body.contains(st2.sampleType.displayName)
                assert body.contains(st2.seqType.displayName)
                assert recipient == "operator"
            }
        }
        service.processingOptionService = Mock(ProcessingOptionService) {
            findOptionAsString(_) >> "operator"
        }

        when:
        service.notifyAboutUnsetConfig(ticket)

        then:
        true
    }
}
