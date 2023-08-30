/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.util.spreadsheet

import groovy.transform.TupleConstructor

@TupleConstructor
enum Delimiter {
    COMMA(',' as char, ','),
    SPACE(' ' as char, 'space'),
    SEMICOLON(';' as char, ';'),
    TAB('\t' as char, 'tab'),

    /**
     * Special marker, indicating that the code should try autodetecting the delimiter.
     * <p>
     * Delimiter itself cannot provide the autodetection transparantly, because it doesn't know the context to detect on.
     * Instead, code encountering this value should use {@link #detectDelimiter(Reader)} on the thing it's parsing and use
     * the delimiter returned by that.
     */
    AUTO_DETECT('\0' as char, 'autodetect'),

    final char delimiter
    final String displayName

    char getDelimiter() {
        if (this == AUTO_DETECT) {
            throw new IllegalArgumentException("Attempted to access the delimiter property of special marker value AUTO_DETECT. " +
                    "This is a programmer error because Delimiter doesn't know it's context, so has nothing to detect on. " +
                    "Please (have someone) rewrite the code to use 'detectDelimiter(context)' instead.")
        }

        return this.delimiter
    }

    String getDisplayName() {
        if (this == AUTO_DETECT) {
            // note to the future: this might not be needed. It should be OK for a user
            throw new IllegalArgumentException("Attempted to access the displayName property of special marker value AUTO_DETECT. " +
                    "This is probably a programmer mistake, because not all places in OTP know how to autodetect yet, so you shouldn't" +
                    "be able to select this.")
        }

        return this.displayName
    }

    /** returns all the "normal" values, that don't need special handling. */
    static Delimiter[] simpleValues() {
        return values() - AUTO_DETECT
    }

    static Delimiter detectDelimiter(BufferedReader context) {
        // We really need a BufferedReader for correct `mark`ing, sadly..
        // When trying this with the groovy extension method for StringReader#readLine, reset() only skips back a single line,
        // not all the way to the beginning like it should (confused noises).
        // I *guess* the Groovy method uses .mark internally, messing up our bookkeeping..
        context.mark(1 * 1024 * 1024) // a 1 MB reading window should be enough to look at the first few lines.

        try {
            String header = context.readLine()

            // which delimiters should we try?
            // skip on 'space' for now since it's a bit edge-case-y, if the files contains sentences..
            Delimiter[] candidates = simpleValues() - SPACE
            // which delimiter gives us most columns so far?
            Delimiter bestCandidate = null

            // try candidates
            candidates.each {
                // Is our current candidate a positive match?
                if (header.contains(it.delimiter as String)) {
                    // Header contents are fairly constrained to only alphanumeric chars, so we shouldn't see any other delimiters.
                    Delimiter[] others = candidates - it
                    others.each { other ->
                        if (header.contains(other.delimiter as String)) {
                            throw new MultipleDelimitersDetectedException([it.displayName, other.displayName])
                        }
                    }
                    bestCandidate = it
                }
            }

            if (bestCandidate) {
                return bestCandidate
            }
            return TAB // if we didn't find anything, it's probably a single-column TSV, do the lazy thing.
        } finally {
            context.reset() // return reader to start, so CSV-reader starts from the beginning again and doesn't skip lines used for auto-detection.
        }
    }
}
