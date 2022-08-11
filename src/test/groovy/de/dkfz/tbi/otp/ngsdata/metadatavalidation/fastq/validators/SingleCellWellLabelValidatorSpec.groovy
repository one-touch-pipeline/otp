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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SingleCellWellLabelValidatorSpec extends Specification {

    @Unroll
    void 'validate valid single cell well label "#validValue", succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                [
                        MetaDataColumn.SINGLE_CELL_WELL_LABEL,
                        validValue,
                ].join('\n')
        )

        when:
        new SingleCellWellLabelValidator().validate(context)

        then:
        context.problems.empty

        where:
        validValue << [
                'G2',
                'H20',
                '1J20',
                '123S20',
        ]
    }

    @Unroll
    void 'validate invalid single cell well label "#invalidValue", adds problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                [
                        MetaDataColumn.SINGLE_CELL_WELL_LABEL,
                        invalidValue,
                ].join('\n')
        )

        when:
        new SingleCellWellLabelValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The single cell well label '${invalidValue}' is not a valid directory name.")

        where:
        invalidValue << [
                '12 34',
                '12#34',
                '12&34',
                '12:34',
        ]
    }
}
