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
package de.dkfz.tbi.util.spreadsheet

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

class FilteredSpreadsheet extends Spreadsheet {

    private final List<Row> dataRows

    FilteredSpreadsheet(String document,
                        Delimiter delimiter = Delimiter.TAB,
                        @ClosureParams(value = SimpleType, options = ['java.lang.String']) Closure<String> renameHeader = Closure.IDENTITY,
                        @ClosureParams(value = SimpleType, options = ['de.dkfz.tbi.util.spreadsheet.Row']) Closure<Boolean> dataRowFilter = { true }) {
        super(document, delimiter, renameHeader)
        dataRows = super.dataRows.findAll(dataRowFilter).asImmutable()
        dataRows -= dataRows.findAll { it.cells == it.cells.findAll { it.text == "" } }
    }

    @Override
    List<Row> getDataRows() {
        return dataRows
    }
}
