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

package de.dkfz.tbi.otp.job.jobs.cellRanger

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils

@Rollback
@Integration
class CellRangerServiceIntegrationSpec extends Specification implements CellRangerFactory {

    @Rule
    public TemporaryFolder temporaryFolder

    CellRangerService cellRangerService

    SingleCellBamFile singleCellBamFile
    File file

    void setup() {
        cellRangerService = new CellRangerService()
        cellRangerService.fileSystemService = new TestFileSystemService()
        singleCellBamFile = createBamFile()
        file = new File(temporaryFolder.newFolder(), "${HelperUtils.uniqueString}_metrics_summary.csv")
        singleCellBamFile.metaClass.getQualityAssessmentCsvFile = { return file }
    }

    void "parseCellRangerQaStatistics, all values are parsed and stored in CellRangerQualityAssessment"() {
        given:
        createQaFileOnFileSystem(file)

        when:
        CellRangerQualityAssessment cellRangerQualityAssessment = cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)

        then:
        CellRangerService.MetricsSummaryCsvColumn.values().every {
            cellRangerQualityAssessment."${it.attributeName}" != null
        }
    }

    @Unroll
    void "parseCellRangerQaStatistics, unparsable value #value cause an exception"() {
        given:
        createQaFileOnFileSystem(file, [(key): value])

        when:
        cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)

        then:
        NumberFormatException e = thrown()
        e.message.contains("Failed to parse '${value}'")

        where:
        key                         | value
        "Estimated Number of Cells" | "12%34"
        "Fraction Reads in Cells"   | "77.7%%"
        "Mean Reads per Cell"       | "string"
    }

    void "parseCellRangerQaStatistics, missing columns cause an exception"() {
        given:
        file = CreateFileHelper.createFile(
                new File(temporaryFolder.newFolder(), "${HelperUtils.uniqueString}_metrics_summary.csv"),
                "column1,column2\ncontent1,content2"
        )

        when:
        cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains("can not be found in")
    }
}
