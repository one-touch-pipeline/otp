package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock(SeqType)
class SeqTypeValidatorSpec extends Specification {

    void 'validate, when column is missing, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                "SomeValue\n")

        when:
        new SeqTypeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Mandatory column 'SEQUENCING_TYPE' is missing.")
    }

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SEQUENCING_TYPE}\n" +
                        "SeqType1\n" +
                        "SeqType2\n" +
                        "SeqType1\n")
        SeqType seqType2 = DomainFactory.createSeqType(name: 'SeqType2', dirName: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        DomainFactory.createSeqType(name: 'SeqType2', dirName: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)

        when:
        SeqTypeValidator validator = new SeqTypeValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqType2') >> seqType2
        }
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Sequencing type 'SeqType1' is not registered in the OTP database.")
    }

    void 'validate adds expected error when SEQUENCING_TYPE is empty'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SEQUENCING_TYPE}\n" +
                        "\n"
        )

        when:
        SeqTypeValidator validator = new SeqTypeValidator()
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Sequencing type must not be empty.")
    }

    void 'validate, when column TAGMENTATION_BASED_LIBRARY exists, adds expected error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                        "SeqType1\t\n" +
                        "SeqType1\ttrue\n" +
                        "SeqType2\t\n" +
                        "SeqType2\ttrue\n" +
                        "SeqType2\tfalse\n")
        SeqType seqType1 = DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        SeqType seqType2 = DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        SeqType seqType1Tag = DomainFactory.createSeqType(name: 'SeqType1_TAGMENTATION', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)

        when:
        SeqTypeValidator validator = new SeqTypeValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqType1') >> seqType1
            1 * findByNameOrImportAlias('SeqType1_TAGMENTATION') >> seqType1Tag
            2 * findByNameOrImportAlias('SeqType2') >> seqType2
            1 * findByNameOrImportAlias('SeqType2_TAGMENTATION') >> null
        }
        validator.validate(context)


        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A5', 'B5'])
        problem.message.contains("Sequencing type 'SeqType2_TAGMENTATION' is not registered in the OTP database.")
    }
}
