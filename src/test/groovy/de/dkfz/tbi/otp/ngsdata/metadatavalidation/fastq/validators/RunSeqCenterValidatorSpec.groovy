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

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class RunSeqCenterValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Run,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
        ]
    }

    void 'validate adds expected errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CENTER_NAME}\t${RUN_ID}\n" +
                        "Center1\tInconsistentMetadata\n" +
                        "Center2\tInconsistentMetadata\n" +
                        "Center2\tInconsistentDatabaseAndMetadata\n" +
                        "Center1\tConsistentDatabaseAndMetadata\n" +
                        "Center1\tRunNotRegistered\n")
        SeqCenter center1 = DomainFactory.createSeqCenter(name: 'Center1')
        DomainFactory.createRun(name: 'InconsistentDatabaseAndMetadata', seqCenter: center1)
        DomainFactory.createRun(name: 'ConsistentDatabaseAndMetadata', seqCenter: center1)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        "All entries for run 'InconsistentMetadata' must have the same value in the column '${CENTER_NAME}'.", "All entries for one run must have the same value in the column 'CENTER_NAME'."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.ERROR,
                        "Run 'InconsistentDatabaseAndMetadata' is already registered in the OTP database with sequencing center 'Center1', not with 'Center2'.", "At least one run is already registered in the OTP database with another sequencing center."),
        ]

        when:
        new RunSeqCenterValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
