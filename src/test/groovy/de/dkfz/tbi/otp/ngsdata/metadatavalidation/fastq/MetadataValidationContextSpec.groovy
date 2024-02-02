/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators.SampleLibraryValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MetadataValidationContextSpec extends Specification implements DomainFactoryCore, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                SeqType,
                RawSequenceFile,
        ]
    }

    void 'test getSummary when problems occurred'() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE}\t${SAMPLE_NAME}\t${TAGMENTATION_LIBRARY}\t${SEQUENCING_READ_TYPE}
${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\ttestSampleLib\tlib\t${SequencingReadType.PAIRED}
${SeqTypeNames.EXOME.seqTypeName}\ttestSample1Lib\tlib\t${SequencingReadType.PAIRED}
${SeqTypeNames.RNA.seqTypeName}\ttest2SampleLib\t\t${SequencingReadType.SINGLE}
${SeqTypeNames.CHIP_SEQ.seqTypeName}\ttest3SampleLib\t\t${SequencingReadType.PAIRED}
""")
        new SampleLibraryValidator().validate(context)

        expect:
        context.summary == [
                "For samples which contain 'lib', there should be a value in the TAGMENTATION_LIBRARY column.",
        ]
    }

    void 'test getSummary when no problem occurred'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${TAGMENTATION_LIBRARY}\n" +
                        "testSampleLib\tlib\n" +
                        "testSample\tlib\n" +
                        "testSample\n" +
                        "testLIbSample\tlib\n"
        )
        new SampleLibraryValidator().validate(context)

        expect:
        [] == context.summary
    }
}
