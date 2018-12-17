package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class IlseNumberSeqCenterValidatorSpec extends Specification {

    void 'validate, when metadata file contain ILSe number and data comes from DKFZ, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\t${MetaDataColumn.CENTER_NAME}\n" +
                        "1234\tDKFZ\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/1234/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberSeqCenterValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file does not contain ILSe number and data does not come from DKFZ, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.CENTER_NAME}\n" +
                        "another center\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberSeqCenterValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file contain ILSe number and data comes not from DKFZ, adds warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\t${MetaDataColumn.CENTER_NAME}\n" +
                        "1234\tanother center\n",
                        ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/1234/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberSeqCenterValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "ILSe number is available although data was provided by 'another center'.")
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when ILSe number is "" and data comes from DKFZ, adds warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\t${MetaDataColumn.CENTER_NAME}\n" +
                        "\tDKFZ\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberSeqCenterValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "ILSe number is not available although data was provided by 'DKFZ'.")
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when metadata file does not contain an ILSe number column and data comes from DKFZ, add warnings'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.CENTER_NAME}\n" +
                        "DKFZ\n"
        )

        when:
        new IlseNumberSeqCenterValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "ILSe number is not available although data was provided by 'DKFZ'.")
        ]
        containSame(context.problems, expectedProblems)
    }
}
