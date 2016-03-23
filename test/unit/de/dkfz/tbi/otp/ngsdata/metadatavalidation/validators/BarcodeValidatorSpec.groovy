package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import spock.lang.Specification

import de.dkfz.tbi.util.spreadsheet.validation.Level

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement


class BarcodeValidatorSpec extends Specification {

    void 'validate, when barcode is valid, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.BARCODE}\n" +
                "\n" +
                 "AGGCAGAA\n"
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
        problem.message.contains("The barcode 'invalidBarcode' does not match the usually used regular expression: '${BarcodeValidator.REGEX}.'")
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
        problem.message.contains("'${barcode}' is not a well-formed barcode. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should match the regular expression '${BarcodeValidator.REGEX}'.")

        where:
        barcode << [
                ' ',
                '_',
                '-',
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
