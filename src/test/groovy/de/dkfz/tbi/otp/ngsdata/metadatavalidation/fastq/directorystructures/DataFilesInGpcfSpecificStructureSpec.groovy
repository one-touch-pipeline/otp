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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class DataFilesInGpcfSpecificStructureSpec extends Specification {

    @Unroll
    void "getDataFilePath, when file is #fileName, then check is #valid"() {
        given:
        File directory = TestCase.uniqueNonExistentPath
        DirectoryStructure directoryStructure = new DataFilesInGpcfSpecificStructure()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${RUN_ID}\n" +
                        "${fileName}\trun_name\n",
                [metadataFile: Paths.get(directory.path, 'metadata.tsv')]
        )
        Set<Cell> invalidCells = context.spreadsheet.dataRows.get(0).cells as Set

        when:
        Path dataFilePath = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(FASTQ_FILE.name()): fileName, (RUN_ID.name()): "run_name"], invalidCells)
        )

        then:
        if (valid) {
            assert dataFilePath == Paths.get(directory.path, "run_name/asdf/fastq/${fileName}")
            assert context.problems.isEmpty()
        } else {
            assert dataFilePath == null
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == LogLevel.ERROR
            assert problem.affectedCells == invalidCells
            assert problem.message.contains('Cannot construct a valid GPCF midterm storage path')
        }

        where:
        fileName            | valid
        'asdf_R1.fastq.gz'  | true
        'asdf_R2.fastq.gz'  | true
        'asdf_I1.fastq.gz'  | true
        'asdf_I2.fastq.gz'  | true
        'asdf_I3.fastq.gz'  | true
        'asdf_R1.fastq'     | false
        '**asdf**.fastq.gz' | false
        '.._R1.fastq.gz'    | false
    }
}
