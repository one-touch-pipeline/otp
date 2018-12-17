package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock(SeqPlatformModelLabel)
class InstrumentModelValidatorSpec extends Specification {

    void 'validate, when column is missing, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                "SomeValue\n")

        when:
        new InstrumentModelValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Mandatory column 'INSTRUMENT_MODEL' is missing.")
    }


    void 'validate adds expected error when INSTRUMENT_MODEL is empty'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.INSTRUMENT_MODEL}

""")

        when:
        new InstrumentModelValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Instrument model must not be empty.")
    }

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.INSTRUMENT_MODEL}
Model1
Model2
Model1
Alias1
""")
        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', importAlias: ['Alias1', 'Alias2'])

        when:
        InstrumentModelValidator validator = new InstrumentModelValidator()
        validator.seqPlatformModelLabelService = Mock(SeqPlatformModelLabelService) {
            1 * findByNameOrImportAlias('Model1') >> null
            1 * findByNameOrImportAlias('Model2') >> seqPlatformModelLabel
            1 * findByNameOrImportAlias('Alias1') >> seqPlatformModelLabel
        }
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Instrument model 'Model1' is not registered in the OTP database.")
    }
}
