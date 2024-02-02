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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_LIBRARY
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class TagmentationLibraryValidatorSpec extends Specification {

    void 'validate, add expected custom libraries, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        "lib1\n" +
                        "lib23\n" +
                        "lib50\n" +
                        "libNA\n"
        )

        when:
        new TagmentationLibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when no CUSTOMER_LIBRARY column exists in metadata file, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new TagmentationLibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when CUSTOMER_LIBRARY column is empty, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        ""
        )

        when:
        new TagmentationLibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when CUSTOMER_LIBRARY entry is not a valid path component, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        "TAGMENTATION_LIBRARY!"
        )

        when:
        new TagmentationLibraryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Tagmentation library 'TAGMENTATION_LIBRARY!' contains invalid characters.")
    }

    void 'validate, when CUSTOMER_LIBRARY entry does not match regular expression, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        "TAGMENTATION_LIBRARY"
        )

        when:
        new TagmentationLibraryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Tagmentation library 'TAGMENTATION_LIBRARY' does not match regular expression '^(?:lib(?:[1-9]\\d*|NA)|)\$'.")
    }
}
