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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import java.nio.file.*

trait DirectoryStructure {

    abstract String getDescription()

    /**
     * The titles of the columns which the paths are constructed from
     */
    abstract List<String> getColumnTitles()

    /**
     * @return The path of the data file or {@code null} if it cannot be constructed
     */
    abstract Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple)

    /**
     * @return The path of the data file or {@code null} if it cannot be constructed
     */
    Path getDataFilePath(MetadataValidationContext context, Row row) {
        Map<String, String> valuesByColumnTitle = [:]
        Set<Cell> cells = new LinkedHashSet<Cell>()
        getColumnTitles().each {
            Cell cell = row.getCellByColumnTitle(it)
            if (cell) {
                valuesByColumnTitle.put(it, cell.text)
                cells.add(cell)
            }
        }
        return getDataFilePath(context, new ValueTuple(valuesByColumnTitle.asImmutable(), cells.asImmutable()))
    }

    private FileSystem fileSystem

    void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem
    }

    FileSystem getFileSystem() {
        return fileSystem ?: FileSystems.getDefault()
    }
}
