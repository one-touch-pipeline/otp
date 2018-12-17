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
        return problems*.level.max { it.intValue() } ?: Level.ALL
    }

    Set<Problem> getProblems() {
        return allProblems.asImmutable()
    }

    Set<Problem> getProblems(Cell cell) {
        return problemsByCell.get(cell)?.asImmutable() ?: Collections.emptySet()
    }
}
