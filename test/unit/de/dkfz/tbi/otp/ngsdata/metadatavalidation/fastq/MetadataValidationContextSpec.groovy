package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.otp.utils.HelperUtils
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MetadataValidationContextSpec extends Specification {

    DirectoryStructure directoryStructure = [:] as DirectoryStructure

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    void 'createFromFile, when file contains undetermined entries, ignores them'() {
        given:
        File file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")
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
}