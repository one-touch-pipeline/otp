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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class InstrumentModelValidatorSpec extends Specification implements DataTest {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                SeqPlatformModelLabel
        ]
    }

    void 'validate, when column is missing, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                "SomeValue\n")

        when:
        new InstrumentModelValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Required column 'INSTRUMENT_MODEL' is missing.")
    }

    void 'validate adds expected error when INSTRUMENT_MODEL is empty'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.INSTRUMENT_MODEL}

""")

        when:
        new InstrumentModelValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Instrument model must not be empty.")
    }

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.INSTRUMENT_MODEL}
Model1
Model2
Model1
Alias1
""")
        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', importAlias: ['Alias1', 'Alias2'])

        when:
        InstrumentModelValidator validator = new InstrumentModelValidator()
        validator.seqPlatformModelLabelService = Mock(SeqPlatformModelLabelService) {
            1 * findByNameOrImportAlias('Model1') >> null
            1 * findByNameOrImportAlias('Model2') >> seqPlatformModelLabel
            1 * findByNameOrImportAlias('Alias1') >> seqPlatformModelLabel
        }
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Instrument model 'Model1' is not registered in the OTP database.")
    }
}
