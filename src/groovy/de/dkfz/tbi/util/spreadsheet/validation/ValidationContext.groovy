package de.dkfz.tbi.util.spreadsheet.validation

import java.util.logging.Level

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

/**
 * Holds references to the validated {@link Spreadsheet} and the problems found in it
 */
class ValidationContext {

    final Spreadsheet spreadsheet
    final Problems problems

    ValidationContext(Spreadsheet spreadsheet, Problems problems = new Problems()) {
        this.spreadsheet = spreadsheet
        this.problems = problems
    }

    Problem addProblem(Set<Cell> affectedCells, Level level, String message) {
        return problems.addProblem(affectedCells, level, message)
    }

    Level getMaximumProblemLevel() {
        return problems.maximumProblemLevel
    }

    Set<Problem> getProblems() {
        return problems.problems
    }

    Set<Problem> getProblems(Cell cell) {
        return problems.getProblems(cell)
    }
}
