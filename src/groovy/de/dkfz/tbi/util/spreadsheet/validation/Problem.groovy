package de.dkfz.tbi.util.spreadsheet.validation

import java.util.logging.Level

import de.dkfz.tbi.util.spreadsheet.Cell
import groovy.transform.*

@EqualsAndHashCode
@ToString
class Problem {

    final Set<Cell> affectedCells
    final Level level
    final String message
    final String type

    Problem(Set<Cell> affectedCells, Level level, String message, String type = message) {
        this.affectedCells = new LinkedHashSet<Cell>(affectedCells).asImmutable()
        this.level = level
        this.message = message
        this.type = type
    }

    String getLevelAndMessage() {
        return level.getName() + ": " + message
    }
}
