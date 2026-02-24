/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.workflow.TableColumn
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class AbstractQcFileParseJobSpec extends Specification {

    @TempDir
    Path tempDir

    TestQcFileParseJob testJob
    WorkflowStep workflowStep
    Path testQcFile

    void setup() {
        testJob = new TestQcFileParseJob()
        testJob.customDelimiter = Delimiter.SEMICOLON
        workflowStep = Mock(WorkflowStep)

        // Create test TSV file
        testQcFile = tempDir.resolve("test_metrics.csv")
        testQcFile.text = """\
Column One;Column Two;Percentage Value;Number with Comma
value1;value2;85.5%;1,234.56
"""
    }

    void "test parseOutputs calls template methods in correct order"() {
        given:
        testJob.testQcFile = testQcFile
        testJob.testColumns = [
                new TableColumn("Column One", "attr1"),
                new TableColumn("Column Two", "attr2"),
                new TableColumn("Percentage Value", "attr3"),
                new TableColumn("Number with Comma", "attr4"),
        ]
        testJob.expectedParsedData = ["attr1": "value1", "attr2": "value2", "attr3": "85.5", "attr4": "1234.56"]

        when:
        testJob.parseOutputs(workflowStep)

        then:
        testJob.getQcFileToParseCallCount == 1
        testJob.getQcColumnsCallCount == 1
        testJob.processQualityAssessmentCallCount == 1
        testJob.actualWorkflowStep == workflowStep
        testJob.actualParsedData == testJob.expectedParsedData
    }

    void "test parseTsvFile extracts correct column values with default preprocessing"() {
        given:
        testJob.testQcFile = testQcFile
        testJob.testColumns = [
                new TableColumn("Column One", "attr1"),
                new TableColumn("Column Two", "attr2"),
                new TableColumn("Percentage Value", "attr3"),
                new TableColumn("Number with Comma", "attr4"),
        ]

        when:
        testJob.parseOutputs(workflowStep)

        then:
        testJob.actualParsedData == [
            "attr1": "value1",
            "attr2": "value2",
            "attr3": "85.5",     // percentage sign removed
            "attr4": "1234.56",  // comma removed
        ]
    }

    void "test parseTsvFile with custom delimiter"() {
        given:
        // Create TAB-separated file
        Path tabFile = tempDir.resolve("test_tab.tsv")
        tabFile.text = "Column One\tColumn Two\nvalue1\tvalue2"

        testJob.testQcFile = tabFile
        testJob.customDelimiter = Delimiter.TAB
        testJob.testColumns = [
                new TableColumn("Column One", "attr1"),
                new TableColumn("Column Two", "attr2"),
        ]

        when:
        testJob.parseOutputs(workflowStep)

        then:
        testJob.actualParsedData == ["attr1": "value1", "attr2": "value2"]
    }

    void "test parseTsvFile with custom text preprocessing"() {
        given:
        testJob.testQcFile = testQcFile
        testJob.customPreprocessing = { String value -> value.toUpperCase() }
        testJob.testColumns = [
                new TableColumn("Column One", "attr1"),
                new TableColumn("Column Two", "attr2"),
        ]

        when:
        testJob.parseOutputs(workflowStep)

        then:
        testJob.actualParsedData == ["attr1": "VALUE1", "attr2": "VALUE2"]
    }

    void "test parseTsvFile throws assertion error when column not found"() {
        given:
        testJob.testQcFile = testQcFile
        testJob.testColumns = [
            new TableColumn("Non Existent Column", "attr1")
        ]

        when:
        testJob.parseOutputs(workflowStep)

        then:
        AssertionError e = thrown()
        e.message.contains("Non Existent Column can not be found in")
    }

    void "test getDelimiter returns SEMICOLON"() {
        expect:
        testJob.delimiter == Delimiter.SEMICOLON
    }

    void "test default preprocessCellValue removes percentage and comma"() {
        expect:
        testJob.preprocessCellValue("100%") == "100"
        testJob.preprocessCellValue("1,234.56") == "1234.56"
        testJob.preprocessCellValue("test,85.5%") == "test85.5"
        testJob.preprocessCellValue("normal_value") == "normal_value"
    }

    // Test implementation class for AbstractQcFileParseJob
    class TestQcFileParseJob extends AbstractQcFileParseJob {
        Path testQcFile
        Collection<TableColumn> testColumns = []
        Map<String, String> expectedParsedData = [:]
        Map<String, String> actualParsedData
        WorkflowStep actualWorkflowStep
        Delimiter customDelimiter
        Closure<String> customPreprocessing

        int getQcFileToParseCallCount = 0
        int getQcColumnsCallCount = 0
        int processQualityAssessmentCallCount = 0

        @Override
        Path getQcFileToParse(WorkflowStep workflowStep) {
            // increment call counter for verification in tests
            getQcFileToParseCallCount++
            return testQcFile
        }

        @Override
        Collection<TableColumn> getQcColumns() {
            getQcColumnsCallCount++
            return testColumns
        }

        @Override
        void processQualityAssessment(WorkflowStep workflowStep, Map<String, String> parsedData) {
            processQualityAssessmentCallCount++
            this.actualWorkflowStep = workflowStep
            this.actualParsedData = parsedData
        }

        @Override
        protected Delimiter getDelimiter() {
            return customDelimiter ?: super.delimiter
        }

        @Override
        protected String preprocessCellValue(String cellValue) {
            return customPreprocessing ? customPreprocessing(cellValue) : super.preprocessCellValue(cellValue)
        }
    }
}
