package de.dkfz.tbi.util.spreadsheet.validation

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import spock.lang.Specification

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
        problems.maximumProblemLevel == Level.ALL
        problems.problems.empty
        problems.getProblems(a1).empty
        problems.getProblems(b1).empty

        when:
        Problem problemA1 = problems.addProblem([a1] as Set, Level.WARNING, msgA1)
        then:
        problemA1.affectedCells == [a1] as Set
        problemA1.level == Level.WARNING
        problemA1.message == msgA1
        problems.maximumProblemLevel == Level.WARNING
        containSame(problems.problems, [problemA1])
        containSame(problems.getProblems(a1), [problemA1])
        problems.getProblems(b1).empty

        when:
        Problem problemA1B1 = problems.addProblem([a1, b1] as Set, Level.ERROR, msgA1B1)
        then:
        problemA1B1.affectedCells == [a1, b1] as Set
        problemA1B1.level == Level.ERROR
        problemA1B1.message == msgA1B1
        problems.maximumProblemLevel == Level.ERROR
        containSame(problems.problems, [problemA1, problemA1B1])
        containSame(problems.getProblems(a1), [problemA1, problemA1B1])
        containSame(problems.getProblems(b1), [problemA1B1])

        when:
        Problem problemB1 = problems.addProblem([b1] as Set, Level.INFO, msgB1)
        then:
        problemB1.affectedCells == [b1] as Set
        problemB1.level == Level.INFO
        problemB1.message == msgB1
        problems.maximumProblemLevel == Level.ERROR
        containSame(problems.problems, [problemA1, problemA1B1, problemB1])
        containSame(problems.getProblems(a1), [problemA1, problemA1B1])
        containSame(problems.getProblems(b1), [problemA1B1, problemB1])
    }
}
