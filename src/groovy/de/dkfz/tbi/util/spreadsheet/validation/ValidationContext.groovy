package de.dkfz.tbi.util.spreadsheet.validation

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

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

    Problem addProblem(Set<Cell> affectedCells, java.util.logging.Level level, String message, String type = message) {
        return problemsObject.addProblem(affectedCells, level, message, type)
    }

    java.util.logging.Level getMaximumProblemLevel() {
        return problemsObject.maximumProblemLevel
    }

    Set<Problem> getProblems() {
        return problemsObject.problems
    }

    Set<Problem> getProblems(Cell cell) {
        return problemsObject.getProblems(cell)
    }
}
