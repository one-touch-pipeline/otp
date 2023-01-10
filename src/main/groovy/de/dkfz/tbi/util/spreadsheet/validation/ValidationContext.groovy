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
package de.dkfz.tbi.util.spreadsheet.validation

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import java.util.logging.Level

/**
 * Holds references to the validated {@link Spreadsheet} and the problems found in it
 */
class ValidationContext {

    final Spreadsheet spreadsheet
    final Problems problemsObject

    ValidationContext(Spreadsheet spreadsheet, Problems problems = new Problems()) {
        this.spreadsheet = spreadsheet
        this.problemsObject = problems
    }

    Problem addProblem(Set<Cell> affectedCells, Level level, String message, String type = message) {
        return problemsObject.addProblem(affectedCells, level, message, type)
    }

    Level getMaximumProblemLevel() {
        return problemsObject.maximumProblemLevel
    }

    Set<Problem> getProblems() {
        return problemsObject.problems
    }

    Set<Problem> getProblems(Cell cell) {
        return problemsObject.getProblems(cell)
    }
}
