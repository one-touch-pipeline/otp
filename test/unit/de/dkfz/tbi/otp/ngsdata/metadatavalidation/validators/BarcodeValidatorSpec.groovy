package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class BarcodeValidatorSpec extends Specification {

    void 'validate, when barcode is valid, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.BARCODE}\n" +
                "\n" +
                 "AGGCAGAA\n" +
                 "AGGCAGAA-AGGCAGAA\n"
        )

        when:
            new BarcodeValidator().validate(context)

        then:
            context.problems.empty
    }


    void 'validate, when barcode use valid chars but does not pass the regular expression, adds warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.BARCODE}\n" +
                "invalidBarcode\n"
        )

        when:
        new BarcodeValidator().validate(context)


        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The barcode 'invalidBarcode' has an unusual format. It should match the regular expression '${BarcodeValidator.SHOULD_REGEX}'.")
    }

    void 'validate, when barcode contains invalid chars, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.BARCODE}\n" +
                "${barcode}\n"
        )

        when:
        new BarcodeValidator().validate(context)


        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("'${barcode}' is not a well-formed barcode. It must match the regular expression '${BarcodeValidator.MUST_REGEX}'. It should match the regular expression '${BarcodeValidator.SHOULD_REGEX}'.")

        where:
        barcode << [
                ' ',
                '_',
        ]
    }


    void 'validate, when no BARCODE column exists in metadata file, adds warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new BarcodeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem([] as Set, Level.WARNING, "Optional column 'BARCODE' is missing. OTP will try to parse the barcodes from the filenames.")
                ]
        containSame(context.problems, expectedProblems)
    }
}
