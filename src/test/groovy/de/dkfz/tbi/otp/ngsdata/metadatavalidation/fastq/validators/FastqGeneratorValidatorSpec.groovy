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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class FastqGeneratorValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SoftwareTool,
                SoftwareToolIdentifier,
        ]
    }

    static final String FASTQ_GENERATOR = "fastq generator"

    static final String METADATA_CONTENT =
            "${MetaDataColumn.FASTQ_GENERATOR}\n" +
            "\n" +
            "${FASTQ_GENERATOR}\n"

    void 'validate, when metadata file contains valid fastq generator, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                METADATA_CONTENT
        )

        DomainFactory.createSoftwareToolIdentifier(
                name: FASTQ_GENERATOR,
                softwareTool: DomainFactory.createSoftwareTool(type: SoftwareTool.Type.BASECALLING)
        )

        when:
        new FastqGeneratorValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file contain invalid fastq generator, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                METADATA_CONTENT
        )

        when:
        new FastqGeneratorValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A3'])
        problem.message.contains("Fastq generator '${FASTQ_GENERATOR}' is not registered in the OTP database.")
    }
}
