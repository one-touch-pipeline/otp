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

    String getLogLikeString() {
        return "[${level.getName()}]: ${messageWithIndentedMultiline}"
    }

    String getMessageWithIndentedMultiline(String indent = "    ") {
        return message.split("\\n").join("\n${indent}")
    }
}
