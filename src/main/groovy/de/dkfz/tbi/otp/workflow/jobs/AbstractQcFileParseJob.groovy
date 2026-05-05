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

import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.utils.spreadsheet.Row
import de.dkfz.tbi.otp.utils.spreadsheet.Spreadsheet
import de.dkfz.tbi.otp.workflow.TableColumn
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

/**
 * Abstract base class for parsing a QC file.
 * Implements the Template Method pattern where subclasses define:
 * 1. Which QC file to parse (getQcFileToParse)
 * 2. Which columns to extract (getQcColumns)
 * 3. How to process the parsed data (processQualityAssessment)
 */
abstract class AbstractQcFileParseJob extends AbstractParseJob {

    /**
     * Template method that defines the parsing workflow
     */
    @Override
    void parseOutputs(WorkflowStep workflowStep) {
        // Get the TSV file to parse from subclass
        Path qcFilePath = getQcFileToParse(workflowStep)

        // Parse the TSV file using subclass-specific column definitions
        Map<String, String> parsedData = parseQcFile(qcFilePath)

        // Let subclass process the parsed data
        processQualityAssessment(workflowStep, parsedData)
    }

    /**
     * Parses the TSV file and extracts the specified columns from the first row
     */
    private Map<String, String> parseQcFile(Path qcFilePath) {
        Spreadsheet spreadsheet = new Spreadsheet(Files.readString(qcFilePath), delimiter)
        assert spreadsheet.dataRows: "No data rows found in ${qcFilePath}"
        Row firstRow = spreadsheet.dataRows.first()

        Map<String, String> result = [:]
        qcColumns.each { TableColumn column ->
            Cell cell = firstRow.getCellByColumnTitle(column.columnName)
            assert cell: "${column.columnName} can not be found in ${qcFilePath}"
            result[column.attributeName] = preprocessCellValue(cell.text)
        }

        return result
    }

    /**
     * Template method - subclasses can override to use different delimiters
     * Default is COMMA for CSV files
     * @return Delimiter to use for parsing the TSV file
     */
    protected Delimiter getDelimiter() {
        return Delimiter.COMMA
    }

    /**
     * Template method - subclasses can override to customize text preprocessing
     * Default removes percentage signs at the end of the string and commas
     * @param cellValue Raw cell value from the spreadsheet
     * @return Processed cell value
     */
    protected String preprocessCellValue(String cellValue) {
        return cellValue.replaceAll("%\$", "").replaceAll(/,/, "")
    }

    /**
     * Template method - subclasses must implement this to specify which file to parse
     * @return Path to the TSV file to parse
     */
    abstract Path getQcFileToParse(WorkflowStep workflowStep)

    /**
     * Template method - subclasses must implement this to define their specific TSV columns
     * @return Collection of TsvColumn defining the columns to parse from the TSV file
     */
    abstract Collection<TableColumn> getQcColumns()

    /**
     * Template method - subclasses must implement this to process the parsed data
     * @param workflowStep The workflow step
     * @param parsedData Map of attribute names to parsed values
     */
    abstract void processQualityAssessment(WorkflowStep workflowStep, Map<String, String> parsedData)
}
