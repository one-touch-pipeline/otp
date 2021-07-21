/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
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
                        LogLevel.WARNING, "'M\u00e4use' contains an unusual character: '\u00e4' (0xe4)", "At least one value contains an unusual character."),
                new Problem([context.spreadsheet.dataRows[0].cells[1]] as Set,
                        LogLevel.WARNING, "'L\u00f6wen' contains an unusual character: '\u00f6' (0xf6)", "At least one value contains an unusual character."),
                new Problem([context.spreadsheet.dataRows[1].cells[0]] as Set,
                        LogLevel.WARNING, "'Tausendf\u00fc\u00dfler' contains unusual characters: '\u00fc' (0xfc), '\u00df' (0xdf)", "At least one value contains an unusual character."),
        ]

        when:
        new UnusualCharactersValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
