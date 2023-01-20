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
package de.dkfz.tbi.util

import groovy.transform.CompileDynamic

import java.text.NumberFormat

/**
 * This class is the central class to use when printing out numbers with their unit.
 */
@CompileDynamic
class UnitHelper {

    private static final Locale LOCALE = Locale.US
    private static final NUMBER_INSTANCE = NumberFormat.getNumberInstance(LOCALE)

    private static final List<String> IBI_BYTE_UNITS = ["Ki", "Mi", "Gi", "Ti", "Pi", "Ei"]
    private static final List<String> SI_UNITS = ["k", "M", "G", "T", "P", "E"]

    private static final String NULL_VALUE = "N/A"

    /**
     * Makes the given number human readable.
     *
     * An adapted version of a function provided by 'aioobe' on stackoverflow:
     * https://stackoverflow.com/a/3758880/6921511
     *
     * @param number The number to make human readable
     * @param unitSymbol The base symbol of the unit, e.g. B, Bases
     * @param unitPrefixes A List containing all the prefixes that can be used in ascending order
     * @param unitStep Multiplication step between units, e.g. 1000 -> 1000m = 1km
     * @return The number formatted to two decimals
     */
    private static String makeNumberHumanReadable(long number, String unitSymbol, List<String> unitPrefixes, int unitStep) {
        if (number < unitStep) {
            return number + " " + unitSymbol
        }
        int exp = (int) (Math.log(number) / Math.log(unitStep))
        String pre = unitPrefixes[exp - 1]
        return String.format(LOCALE, "%.2f %s%s", number / Math.pow(unitStep, exp), pre, unitSymbol)
    }

    static String asBytes(Long numberOfBytes, boolean humanReadable = false, boolean nullable = true) {
        if (numberOfBytes == null && nullable) {
            return NULL_VALUE
        }
        if (!humanReadable) {
            return NUMBER_INSTANCE.format(numberOfBytes) + " B"
        }
        return makeNumberHumanReadable(numberOfBytes, "B", IBI_BYTE_UNITS, 1024)
    }

    static String asNucleobases(Long numberOfBases, boolean humanReadable = false, boolean nullable = true) {
        if (numberOfBases == null && nullable) {
            return NULL_VALUE
        }
        if (!humanReadable) {
            return NUMBER_INSTANCE.format(numberOfBases) + " Bases"
        }
        return makeNumberHumanReadable(numberOfBases, "Bases", SI_UNITS, 1000)
    }

    static String asReads(Long numberOfReads, boolean humanReadable = false, boolean nullable = true) {
        if (numberOfReads == null && nullable) {
            return NULL_VALUE
        }
        if (!humanReadable) {
            return NUMBER_INSTANCE.format(numberOfReads) + " Reads"
        }
        return makeNumberHumanReadable(numberOfReads, "Reads", SI_UNITS, 1000)
    }
}
