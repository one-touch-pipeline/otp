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

class SequencingKitValidatorSpec extends Specification implements DataTest {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                SequencingKitLabel
        ]
    }

    void 'validate, when column is missing, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                "SomeValue\n")

        when:
        new SequencingKitValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
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
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Sequencing kit 'Kit1' is not registered in the OTP database.")
    }
}
