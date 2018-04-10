package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

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
