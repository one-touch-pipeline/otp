package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class BarcodeFilenameValidatorSpec extends Specification {


    void 'validate, when column barcode does not exist in the metadata file and barcode can not be parsed from filename, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "testFileName.fastq.gz\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when column barcode does not exist in the metadata file and barcode can be parsed from filename, adds warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "testFileName_AGGCAGAA_1.fastq.gz\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "The ${BARCODE} column is missing. OTP will use the barcode 'AGGCAGAA' parsed from filename 'testFileName_AGGCAGAA_1.fastq.gz'. (For multiplexed lanes the ${BARCODE} column should be filled.)")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when column barcode is empty and barcode can not be parsed from filename, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${BARCODE}\n" +
                        "testFileName.fastq.gz\t\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column barcode is empty and barcode can be parsed from filename, adds warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${BARCODE}\n" +
                        "testFileName_AGGCAGAA_1.fastq.gz\t\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "A barcode can be parsed from the filename 'testFileName_AGGCAGAA_1.fastq.gz', but the ${BARCODE} cell is empty. OTP will ignore the barcode parsed from the filename.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when barcode exists and barcode can not be parsed from filename, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${BARCODE}\n" +
                        "testFileName.fastq.gz\tAGGCAGAA\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }

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

    void 'validate, when barcode and barcode in filename do not match, adds warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${BARCODE}\n" +
                        "testFileName_AGGCAGGG_1.fastq.gz\tAGGCAGAA\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "The barcode parsed from the filename 'testFileName_AGGCAGGG_1.fastq.gz' ('AGGCAGGG') is different from the value in the ${BARCODE} cell ('AGGCAGAA'). OTP will ignore the barcode parsed from the filename and use the barcode 'AGGCAGAA'.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
