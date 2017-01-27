package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class IlseNumberSeqCenterValidatorSpec extends Specification {

    void 'validate, when metadata file contain ILSe number and data comes from DKFZ, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\t${MetaDataColumn.CENTER_NAME}\n" +
                        "1234\tDKFZ\n",
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/1234/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
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
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
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
                        ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/1234/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
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
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
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
