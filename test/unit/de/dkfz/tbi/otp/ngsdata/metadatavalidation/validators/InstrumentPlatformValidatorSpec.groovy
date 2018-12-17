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

@Mock([SeqPlatform, SeqPlatformGroup, SeqPlatformModelLabel,])
class InstrumentPlatformValidatorSpec extends Specification {

    void 'validate, when column is missing, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                "SomeValue\n")

        when:
        new InstrumentPlatformValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Mandatory column 'INSTRUMENT_PLATFORM' is missing.")
    }

    void 'validate adds expected error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.INSTRUMENT_PLATFORM}\n" +
                "Platform1\n" +
                "Platform2\n" +
                "Platform1\n")
        DomainFactory.createSeqPlatformWithSeqPlatformGroup(name: 'Platform2')
        DomainFactory.createSeqPlatformWithSeqPlatformGroup(name: 'Platform2')

        when:
        new InstrumentPlatformValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Instrument platform 'Platform1' is not registered in the OTP database.")
    }
}
