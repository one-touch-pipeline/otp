package de.dkfz.tbi.util.spreadsheet.validation

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import de.dkfz.tbi.util.spreadsheet.Cell

@EqualsAndHashCode
@ToString
class Problem {

    final Set<Cell> affectedCells
    final java.util.logging.Level level
    final String message
    final String type

    Problem(Set<Cell> affectedCells, java.util.logging.Level level, String message, String type = message) {
        this.affectedCells = new LinkedHashSet<Cell>(affectedCells).asImmutable()
        this.level = level
        this.message = message
        this.type = type
    }

    String getLevelAndMessage() {
        return level.getName() + ": " + message
    }
}
