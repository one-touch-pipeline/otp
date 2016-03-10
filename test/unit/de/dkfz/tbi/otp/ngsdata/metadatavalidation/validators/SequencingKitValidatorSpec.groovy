package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SequencingKitLabel
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import grails.test.mixin.Mock
import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock(SequencingKitLabel)
class SequencingKitValidatorSpec extends Specification {

    void 'validate, when column is missing, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                "SomeValue\n")

        when:
        new SequencingKitValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Optional column 'SEQUENCING_KIT' is missing.")
    }

    void 'validate adds expected error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_KIT}
Kit1
Kit2
Kit1
Alias1

""")
        DomainFactory.createSequencingKitLabel(name: 'Kit2', alias: ['Alias1', 'Alias2'])

        when:
        new SequencingKitValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Sequencing kit 'Kit1' is not registered in the OTP database.")
    }
}
