package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.createContext

class UnusualCharactersValidatorSpec extends Specification {

    void 'validate adds expected warnings'() {
        given:
        MetadataValidationContext context = createContext(
                "Eulen\tM\u00e4use\n" +
                        "M\u00e4use\tL\u00f6wen\n" +
                        "Tausendf\u00fc\u00dfler\tEulen\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.header.cells[1], context.spreadsheet.dataRows[0].cells[0]] as Set,
                        Level.WARNING, "'M\u00e4use' contains an unusual character: '\u00e4' (0xe4)", "At least one value contains an unusual character."),
                new Problem([context.spreadsheet.dataRows[0].cells[1]] as Set,
                        Level.WARNING, "'L\u00f6wen' contains an unusual character: '\u00f6' (0xf6)", "At least one value contains an unusual character."),
                new Problem([context.spreadsheet.dataRows[1].cells[0]] as Set,
                        Level.WARNING, "'Tausendf\u00fc\u00dfler' contains unusual characters: '\u00fc' (0xfc), '\u00df' (0xdf)", "At least one value contains an unusual character."),
        ]

        when:
        new UnusualCharactersValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
