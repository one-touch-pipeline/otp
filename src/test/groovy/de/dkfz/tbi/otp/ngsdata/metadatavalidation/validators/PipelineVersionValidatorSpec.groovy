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

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([SoftwareTool, SoftwareToolIdentifier])
class PipelineVersionValidatorSpec extends Specification {

    static final String PIPELINE_VERSION = "pipeline version"

    static final String METADATA_CONTENT =
            "${MetaDataColumn.PIPELINE_VERSION}\n" +
            "\n" +
            "${PIPELINE_VERSION}\n"

    void 'validate, when metadata file contains valid pipeline version, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                METADATA_CONTENT
        )

        DomainFactory.createSoftwareToolIdentifier(
                name: PIPELINE_VERSION,
                softwareTool: DomainFactory.createSoftwareTool( type: SoftwareTool.Type.BASECALLING )
        )

        when:
        new PipelineVersionValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file contain invalid pipeline version, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                METADATA_CONTENT
        )

        when:
        new PipelineVersionValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A3'])
        problem.message.contains("Pipeline version '${PIPELINE_VERSION}' is not registered in the OTP database.")
    }
}
