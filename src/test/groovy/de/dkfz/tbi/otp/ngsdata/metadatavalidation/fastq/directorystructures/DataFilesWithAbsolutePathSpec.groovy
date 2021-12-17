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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class DataFilesWithAbsolutePathSpec extends Specification {

    void test() {
        given:
        File directory = TestCase.uniqueNonExistentPath
        DirectoryStructure directoryStructure = new DataFilesWithAbsolutePath()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                "${directory.path}/foo.fastq\n" +
                "${directory.path}/foo(bar).fastq\n",
                [metadataFile: Paths.get(directory.path, 'metadata.tsv')]
        )
        Set<Cell> validCells = [context.spreadsheet.dataRows.get(0).cells.get(0)] as Set
        Set<Cell> invalidCells = [context.spreadsheet.dataRows.get(1).cells.get(0)] as Set

        when:
        Path dataFilePath1 = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(MetaDataColumn.FASTQ_FILE.name()): "${directory.path}/foo.fastq"], validCells))

        then:
        context.problems.isEmpty()
        dataFilePath1 == Paths.get(directory.path, 'foo.fastq')
        context.problems.isEmpty()

        when:
        Path dataFilePath2 = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(MetaDataColumn.FASTQ_FILE.name()): "${directory.path}/foo(bar).fastq"], invalidCells))

        then:
        dataFilePath2 == null
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        problem.affectedCells == invalidCells
        problem.message.contains('is not a valid absolute path')
    }
}
