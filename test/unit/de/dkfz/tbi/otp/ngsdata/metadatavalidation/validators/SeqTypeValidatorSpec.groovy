package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import grails.test.mixin.Mock
import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)

        when:
        new SeqTypeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Sequencing type 'SeqType1' is not registered in the OTP database.")
    }

    void 'validate, when column TAGMENTATION_BASED_LIBRARY exists, adds expected error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                        "SeqType1\t\n" +
                        "SeqType1\ttrue\n" +
                        "SeqType2\t\n" +
                        "SeqType2\ttrue\n")
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        DomainFactory.createSeqType(name: 'SeqType1_TAGMENTATION', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)

        when:
        new SeqTypeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A5', 'B5'])
        problem.message.contains("Sequencing type 'SeqType2_TAGMENTATION' is not registered in the OTP database.")
    }
}
