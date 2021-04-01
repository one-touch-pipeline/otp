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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq


import grails.testing.gorm.DataTest
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators.SampleLibraryValidator
import de.dkfz.tbi.otp.utils.HelperUtils

import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MetadataValidationContextSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                SeqType,
                Realm,
        ]
    }

    DirectoryStructure directoryStructure = [:] as DirectoryStructure

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    void 'createFromFile, when file contains undetermined entries, ignores them'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = ("c ${FASTQ_FILE} ${SAMPLE_NAME} ${INDEX}\n" +
                "0 Undetermined_1.fastq.gz x x\n" +
                "1 Undetermined_1.fastq.gz x Undetermined\n" +
                "2 Undetermined_1.fastq.gz Undetermined_1 x\n" +
                "3 Undetermined_1.fastq.gz Undetermined_1 Undetermined\n" +
                "4 x x x\n" +
                "5 x x Undetermined\n" +
                "6 x Undetermined_1 x\n" +
                "7 x Undetermined_1 Undetermined\n" +
                "").replaceAll(' ', '\t').getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure, "")

        then:
        context.spreadsheet.dataRows.size() == 4
        context.spreadsheet.dataRows[0].cells[0].text == '4'
        context.spreadsheet.dataRows[1].cells[0].text == '5'
        context.spreadsheet.dataRows[2].cells[0].text == '6'
        context.spreadsheet.dataRows[3].cells[0].text == '7'
    }

    void 'createFromFile, when file header contains alias, replace it'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = ("UNKNOWN ${ALIGN_TOOL} ${SEQUENCING_READ_TYPE.importAliases.first()}\n" +
                "1 2 3"
        ).replaceAll(' ', '\t').getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure, "")

        then:
        context.spreadsheet.header.cells[0].text == "UNKNOWN"
        context.spreadsheet.header.cells[1].text == ALIGN_TOOL.name()
        context.spreadsheet.header.cells[2].text == SEQUENCING_READ_TYPE.name()
    }


    void 'test getSummary when problems occurred'()  {
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
        context.getSummary() == [
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
        [] == context.getSummary()
    }
}
