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

import spock.lang.Specification

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class ProblemsSpec extends Specification {

    void 'test all methods'() {
        given:
        Spreadsheet spreadsheet = new Spreadsheet('A1\tB1')
        def (Cell a1, Cell b1) = spreadsheet.header.cells
        String msgA1 = 'msgA1'
        String msgA1B1 = 'msgA1B1'
        String msgB1 = 'msgB1'

        when:
        Problems problems = new Problems()
        then:
        problems.maximumProblemLevel == LogLevel.ALL
        problems.problems.empty
        problems.getProblems(a1).empty
        problems.getProblems(b1).empty

        when:
        Problem problemA1 = problems.addProblem([a1] as Set, LogLevel.WARNING, msgA1)
        then:
        problemA1.affectedCells == [a1] as Set
        problemA1.level == LogLevel.WARNING
        problemA1.message == msgA1
        problems.maximumProblemLevel == LogLevel.WARNING
        containSame(problems.problems, [problemA1])
        containSame(problems.getProblems(a1), [problemA1])
        problems.getProblems(b1).empty

        when:
        Problem problemA1B1 = problems.addProblem([a1, b1] as Set, LogLevel.ERROR, msgA1B1)
        then:
        problemA1B1.affectedCells == [a1, b1] as Set
        problemA1B1.level == LogLevel.ERROR
        problemA1B1.message == msgA1B1
        problems.maximumProblemLevel == LogLevel.ERROR
        containSame(problems.problems, [problemA1, problemA1B1])
        containSame(problems.getProblems(a1), [problemA1, problemA1B1])
        containSame(problems.getProblems(b1), [problemA1B1])

        when:
        Problem problemB1 = problems.addProblem([b1] as Set, LogLevel.INFO, msgB1)
        then:
        problemB1.affectedCells == [b1] as Set
        problemB1.level == LogLevel.INFO
        problemB1.message == msgB1
        problems.maximumProblemLevel == LogLevel.ERROR
        containSame(problems.problems, [problemA1, problemA1B1, problemB1])
        containSame(problems.getProblems(a1), [problemA1, problemA1B1])
        containSame(problems.getProblems(b1), [problemA1B1, problemB1])
    }
}
