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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class LibPrepKitValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                LibraryPreparationKit,
        ]
    }

    LibPrepKitValidator validator

    static final String VALID_METADATA =
            "${MetaDataColumn.LIB_PREP_KIT}\n" +
                    "lib_prep_kit\n" +
                    "lib_prep_kit_import_alias\n"

    void setup() {
        validator = new LibPrepKitValidator()
        validator.libraryPreparationKitService = new LibraryPreparationKitService()
        DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit', importAlias: ["lib_prep_kit_import_alias"])
    }

    void 'validate, when metadata file contains valid LibPrepKit'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file contains invalid LibPrepKit, adds error'() {
        given:
        String invalid = "totally_invalid_lib_prep_kit"
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA + "${invalid}\n"
        )

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A4'])
        problem.message.contains("The library preparation kit '${invalid}' is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.")
    }

    @Unroll
    void 'validate, when metadata file column LibPrepKit is #message, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.LIB_PREP_KIT}\tother
${libPrepKit}\tabc
${libPrepKit}\tdef
"""
        )

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A3'])
        problem.message.contains("The library preparation kit column is ${message}")

        where:
        libPrepKit                                       | message
        ''                                               | 'empty'
        InformationReliability.UNKNOWN_VERIFIED.rawValue | InformationReliability.UNKNOWN_VERIFIED.rawValue
    }

    void 'validate, when metadata file contains no LibPrepKit column, adds one warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Optional column 'LIB_PREP_KIT' is missing.")
    }
}
