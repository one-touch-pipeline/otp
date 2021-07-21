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

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell

class Problems {

    private final Set<Problem> allProblems = new LinkedHashSet<Problem>()
    private final Map<Cell, Set<Problem>> problemsByCell = [:]

    Problem addProblem(Set<Cell> affectedCells, java.util.logging.Level level, String message, String type = message) {
        assert !affectedCells.contains(null)
        Problem problem = new Problem(affectedCells, level, message, type)
        allProblems.add(problem)
        affectedCells.each {
            CollectionUtils.getOrPut(problemsByCell, it, new LinkedHashSet<Problem>()).add(problem)
        }
        return problem
    }

    java.util.logging.Level getMaximumProblemLevel() {
        return getMaximumProblemLevel(allProblems)
    }

    static java.util.logging.Level getMaximumProblemLevel(Collection<Problem> problems) {
        return problems*.level.max { it.intValue() } ?: LogLevel.ALL
    }

    Set<Problem> getProblems() {
        return allProblems.asImmutable()
    }

    Set<Problem> getProblems(Cell cell) {
        return problemsByCell.get(cell)?.asImmutable() ?: Collections.emptySet()
    }

    String getSortedProblemListString() {
        return allProblems.sort { a, b ->
            b.level.intValue() <=> a.level.intValue() ?: a.message <=> b.message
        }.collect { Problem problem ->
            "- ${problem.logLikeString}"
        }.join("\n")
    }

    @Override
    String toString() {
        return problems.empty ? 'no problems' : "${problems.size()} problems:\n${sortedProblemListString}"
    }
}
