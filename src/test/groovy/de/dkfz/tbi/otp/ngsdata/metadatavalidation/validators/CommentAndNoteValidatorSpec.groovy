/*
 * Copyright 2011-2024 The OTP authors
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

class CommentAndNoteValidatorSpec extends Specification {

    void 'validate adds expected info'() {
        given:
        MetadataValidationContext context = createContext(
                "ComMenT\tnote\timportant NOTe\tasdfasdf\tCOMMENT asdf\n" +
                        "a1\ta2\ta3\ta4\ta5\n" +
                        "a1\ta1\ta3\ta4\t   \n"
        )
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[1].cells[0]] as Set,
                        LogLevel.INFO, "Comment/Note (ComMenT): a1", "Comment/Note."),
                new Problem([context.spreadsheet.dataRows[1].cells[1]] as Set, LogLevel.INFO, "Comment/Note (note): a1", "Comment/Note."),
                new Problem([context.spreadsheet.dataRows[0].cells[1]] as Set, LogLevel.INFO, "Comment/Note (note): a2", "Comment/Note."),
                new Problem([context.spreadsheet.dataRows[0].cells[2], context.spreadsheet.dataRows[1].cells[2]] as Set,
                        LogLevel.INFO, "Comment/Note (important NOTe): a3", "Comment/Note."),
                new Problem([context.spreadsheet.dataRows[0].cells[4]] as Set, LogLevel.INFO, "Comment/Note (COMMENT asdf): a5", "Comment/Note."),
        ]

        when:
        new CommentAndNoteValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
