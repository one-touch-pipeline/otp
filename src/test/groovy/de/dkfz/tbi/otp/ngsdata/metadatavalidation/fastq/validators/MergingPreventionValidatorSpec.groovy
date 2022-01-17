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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MergingPreventionValidatorSpec extends Specification implements DataTest, DomainFactoryCore, AlignmentPipelineFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
        ]
    }

    void "validate, when extracted value is valid, then call service for checking"() {
        given:
        MergingPreventionDataDto data = new MergingPreventionDataDto([filledCompletely: true])
        MergingPreventionService mergingPreventionService = Mock {
            0 * _
        }
        MergingPreventionValidator validator = new MergingPreventionValidator([
                mergingPreventionService: mergingPreventionService
        ])
        MetadataValidationContext context = createContext()

        when:
        validator.validate(context)

        then:
        1 * mergingPreventionService.parseMetaData(_) >> data
        1 * mergingPreventionService.checkForMergingWorkPackage(context, _, data)
        1 * mergingPreventionService.checkForSeqTracks(context, _, data)
    }

    void "validate, when extracted value is not valid, then do not call service for checking"() {
        given:
        MergingPreventionDataDto data = new MergingPreventionDataDto([filledCompletely: false])
        MergingPreventionService mergingPreventionService = Mock {
            0 * _
        }
        MergingPreventionValidator validator = new MergingPreventionValidator([
                mergingPreventionService: mergingPreventionService
        ])
        MetadataValidationContext context = createContext()

        when:
        validator.validate(context)

        then:
        1 * mergingPreventionService.parseMetaData(_) >> data
        0 * mergingPreventionService.checkForMergingWorkPackage(context, _, data)
        0 * mergingPreventionService.checkForSeqTracks(context, _, data)
    }

    private MetadataValidationContext createContext() {
        List<String> headers = [
                PROJECT,
                SAMPLE_NAME,
                SEQUENCING_TYPE,
                SEQUENCING_READ_TYPE,
                BASE_MATERIAL,
                ANTIBODY_TARGET,
                INSTRUMENT_PLATFORM,
                INSTRUMENT_MODEL,
                SEQUENCING_KIT,
                LIB_PREP_KIT,
        ]
        String header = headers.join('\t')
        String row = headers.collect { "any-value-${nextId}" }.join('\t')
        String content = [
                header,
                row,
        ].join('\n')
        return MetadataValidationContextFactory.createContext(content)
    }
}
