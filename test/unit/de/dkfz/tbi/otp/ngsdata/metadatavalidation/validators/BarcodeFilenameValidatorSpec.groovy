package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import spock.lang.Specification
import de.dkfz.tbi.util.spreadsheet.validation.Level

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.BARCODE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame


class BarcodeFilenameValidatorSpec extends Specification {

    void 'validate, when barcode and barcode in filename match, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${BARCODE}\n" +
                        "testFileName.fastq.gz\t\n" +
                        "testFileName_AGGCAGAA.fastq.gz\tAGGCAGAA\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when barcode and barcode in filename do not match, adds warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${BARCODE}\n" +
                "testFileName_AGGCAGGG_1.fastq.gz\tAGGCAGAA\n" +
                "testFileName_AGGCAGGG_1.fastq.gz\t\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "The barcodes in the filename 'testFileName_AGGCAGGG_1.fastq.gz' ('AGGCAGGG') and in the '${BARCODE}' column ('AGGCAGAA') are different. OTP will use both barcodes."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING, "There is no value in the '${BARCODE}' column, but the barcode can be parsed from the filename 'testFileName_AGGCAGGG_1.fastq.gz'. OTP will use the parsed barcode 'AGGCAGGG'.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when column barcode does not exists in the metadata file, adds warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "testFileName_AGGCAGAA_1.fastq.gz\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "There is no value in the '${BARCODE}' column, but the barcode can be parsed from the filename 'testFileName_AGGCAGAA_1.fastq.gz'. OTP will use the parsed barcode 'AGGCAGAA'.")
        ]
        containSame(context.problems, expectedProblems)
    }
}
