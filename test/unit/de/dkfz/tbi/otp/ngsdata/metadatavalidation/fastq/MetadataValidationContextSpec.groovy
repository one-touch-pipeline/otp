package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Mock([
        SeqType,
        Realm,
])
class MetadataValidationContextSpec extends Specification {

    DirectoryStructure directoryStructure = [:] as DirectoryStructure

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    void 'createFromFile, when file contains undetermined entries, ignores them'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = ("c ${FASTQ_FILE} ${SAMPLE_ID} ${BARCODE}\n" +
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
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        context.spreadsheet.dataRows.size() == 6
        context.spreadsheet.dataRows[0].cells[0].text == '0'
        context.spreadsheet.dataRows[1].cells[0].text == '1'
        context.spreadsheet.dataRows[2].cells[0].text == '4'
        context.spreadsheet.dataRows[3].cells[0].text == '5'
        context.spreadsheet.dataRows[4].cells[0].text == '6'
        context.spreadsheet.dataRows[5].cells[0].text == '7'
    }


    void 'test getSummary when problems occurred'()  {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE}\t${SAMPLE_ID}\t${CUSTOMER_LIBRARY}\t${LIBRARY_LAYOUT}
${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\ttestSampleLib\tlib\t${LibraryLayout.PAIRED}
${SeqTypeNames.EXOME.seqTypeName}\ttestSample1Lib\tlib\t${LibraryLayout.PAIRED}
${SeqTypeNames.RNA.seqTypeName}\ttest2SampleLib\t\t${LibraryLayout.SINGLE}
${SeqTypeNames.CHIP_SEQ.seqTypeName}\ttest3SampleLib\t\t${LibraryLayout.PAIRED}
""")
        new SampleLibraryValidator().validate(context)

        expect:
        context.getSummary() == [
                "For samples which contain 'lib', there should be a value in the CUSTOMER_LIBRARY column.",
        ]
    }


    void 'test getSummary when no problem occurred'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${CUSTOMER_LIBRARY}\n" +
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
