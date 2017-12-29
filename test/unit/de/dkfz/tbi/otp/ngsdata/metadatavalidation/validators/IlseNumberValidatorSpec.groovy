package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
    IlseSubmission
])
class IlseNumberValidatorSpec extends Specification {

    void 'validate, when metadata fields contain valid ILSe number, succeeds'() {
        given:
        int ILSE_NO = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" +
                        "${ILSE_NO}\n" +
                        "${ILSE_NO}\n",
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when metadata does not contain a column ILSE_NO, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when column ILSE_NO is empty, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                "\n"
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when metadata fields contain more than one ILSe number, adds warnings'() {
        given:
        int ILSE_NO_1 = 5461
        int ILSE_NO_2 = 5462
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO_1}\n" +
                        "${ILSE_NO_2}\n",
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/${ILSE_NO_1}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.WARNING, "There are multiple ILSe numbers in the metadata file.", "There are multiple ILSe numbers in the metadata file."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING, "The metadata file path '${context.metadataFile.path}' does not contain the ILSe number '${ILSE_NO_2}'.", "At least one metadata file path does not contain the ILSe number.")
                ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when ILSe number is not an int, adds errors'() {
        given:
        String ILSE_NO = "ilseNu"
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                "${ILSE_NO}\n" +
                "${ILSE_NO}\n",
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.ERROR, "The ILSe number 'ilseNu' is not an integer.", "At least one ILSe number is not an integer."),
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when ILSe number already exist, adds warnings'() {
        given:
        int ILSE_NO = 5464
        DomainFactory.createIlseSubmission(
                ilseNumber: ILSE_NO,
        )
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" +
                        "${ILSE_NO}\n",
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.WARNING, "The ILSe number '${ILSE_NO}' already exists.","At least one ILSe number already exists.")
        ]
        containSame(context.problems, expectedProblems)
    }


    void 'validate, when file path does not contain the ILSe number, adds warnings'() {
        given:
        int ILSE_NO = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" ,
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]

        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "The metadata file path '${context.metadataFile.path}' does not contain the ILSe number '${ILSE_NO}'.", "At least one metadata file path does not contain the ILSe number.")
        ]
        containSame(context.problems, expectedProblems)
    }


    void 'validate, when file path contains the ILSe number, succeeds'() {
        given:
        int ILSE_NO = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" ,
                ["metadataFile": new File("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}, metadata_fastq.tsv")]

        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }

}
