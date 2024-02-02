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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.TimeUtils
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.time.LocalDate

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_DATE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID

class RunRunDateValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
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
                "${RUN_DATE}\t${RUN_ID}\n" +
                        "2016-01-01\tInconsistentMetadata\n" +
                        "2016-01-02\tInconsistentMetadata\n" +
                        "2016-01-02\tInconsistentDatabaseAndMetadata\n" +
                        "2016-01-01\tConsistentDatabaseAndMetadata\n" +
                        "2016-01-01\tRunWithoutDateInDataBaseButInSheet\n" +
                        "\tRunWithoutDateInDataBaseAndInSheet\n" +
                        "\tRunWithDateInDataBaseButNotInSheet\n"

        )
        Date date = TimeUtils.toDate(LocalDate.of(2016, 1, 1))
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(name: "Illumina")
        createRun(name: 'InconsistentDatabaseAndMetadata', dateExecuted: date)
        createRun(name: 'ConsistentDatabaseAndMetadata', dateExecuted: date)
        createRun(name: '20160102InconsistentRunName', dateExecuted: date, seqPlatform: seqPlatform)
        createRun(name: '160101tConsistentDatabaseAndMetadata', dateExecuted: date, seqPlatform: seqPlatform)
        createRun(name: 'RunWithoutDateInDataBaseButInSheet', dateExecuted: null)
        createRun(name: 'RunWithoutDateInDataBaseAndInSheet', dateExecuted: null)
        createRun(name: 'RunWithDateInDataBaseButNotInSheet', dateExecuted: date)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        "All entries for run 'InconsistentMetadata' must have the same value in the column '${RUN_DATE}'.", "All entries of one run must have the same value in the column 'RUN_DATE'."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.ERROR,
                        "Run 'InconsistentDatabaseAndMetadata' is already registered in the OTP database with run date '2016-01-01', not with '2016-01-02'.", "At least one run is already registered in the OTP database but with another date."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, LogLevel.ERROR,
                        "Run 'RunWithoutDateInDataBaseButInSheet' is already registered in the OTP database without a date, not with '2016-01-01'.", "At least one run is already registered in the OTP database but with another date."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, LogLevel.ERROR,
                        "Run 'RunWithDateInDataBaseButNotInSheet' is already registered in the OTP database with run date '2016-01-01', not with ''.", "At least one run is already registered in the OTP database but with another date."),
        ]

        RunRunDateValidator validator = new RunRunDateValidator()

        when:
        validator.validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}
