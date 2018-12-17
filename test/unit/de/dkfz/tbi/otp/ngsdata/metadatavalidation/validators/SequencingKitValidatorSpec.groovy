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
ImportAlias1

""")
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel(name: 'Kit2', importAlias: ['ImportAlias1', 'ImportAlias2'])

        when:
        SequencingKitValidator validator = new SequencingKitValidator()
        validator.sequencingKitLabelService = Mock(SequencingKitLabelService) {
            1 * findByNameOrImportAlias('Kit1') >> null
            1 * findByNameOrImportAlias('Kit2') >> sequencingKitLabel
            1 * findByNameOrImportAlias('ImportAlias1') >> sequencingKitLabel
        }
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Sequencing kit 'Kit1' is not registered in the OTP database.")
    }
}
