/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

import java.util.regex.Matcher

@Transactional
class MultiplexingService {

    static final String BARCODE_DELIMITER = '_'

    /**
     * find and extract barcodes of the file. It uses the following patterns in the given order:
     * <ul>
     * <li><code>/_([GATC]{6,8})_/</code></li>
     * <li><code>/_[0-9]_([0-9]{3})[\._]/</code></li>
     * </ul>
     * The first matching pattern is used and the group 1 is returned as barcode.
     *
     * @param fileName the name of the file to check for
     * @return the extracted barcode or null
     */
    static String barcode(String fileName) {
        final List<String> regExpression = [
                /_([GATC]{6,8})_/,
                /_[0-9]_([0-9]{3})[\._]/,
        ]
        for (String muxRegexp : regExpression) {
            Matcher matcher = (fileName =~ muxRegexp)
            if (matcher) {
                return matcher.group(1)
            }
        }
        return null
    }

    static String combineLaneNumberAndBarcode(String laneNumber, String barcode) {
        assert laneNumber
        if (barcode != null) {
            return "${laneNumber}${BARCODE_DELIMITER}${barcode}"
        }
        return laneNumber
    }
}
