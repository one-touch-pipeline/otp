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
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.MetaDataKey
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

class MetadataColumnValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MetaDataKey,
        ]
    }

    void setupData() {
        DomainFactory.createMetaDataKey([name: 'FASTQ_FILE'])
        DomainFactory.createMetaDataKey([name: 'ILSE_NO'])
        DomainFactory.createMetaDataKey([name: 'PROJECT'])
    }

    @Unroll
    void 'validate, when columns not registered in OTP, returns a warning'() {
        given:
        setupData()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(document: 'FASTQ_FILE\tFASTQ_FILE_PREFIX\nI am data!')

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING, "Column 'FASTQ_FILE_PREFIX' is not registered in OTP.",
                        "At least one metadata column is not registered in OTP.")
        ]

        when:
        new MetadataColumnValidator().validate(context)

        then:
        context.problems.first().message == expectedProblems.first().message
    }

    @Unroll
    void 'validate, when columns are registered in OTP, does not return a warning'() {
        given:
        setupData()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(document: 'FASTQ_FILE\tILSE_NO\tPROJECT\nI am data!')

        when:
        new MetadataColumnValidator().validate(context)

        then:
        context.problems.empty
    }
}
