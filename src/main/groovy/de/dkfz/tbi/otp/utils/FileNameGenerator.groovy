/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.utils

import java.text.SimpleDateFormat

class FileNameGenerator {

    /**
     * Appends a timestamp with milliseconds to a given filename before its type suffix.
     * The result should be a unique filename.
     *
     * @param filename to append the timestamp
     * @return unique filename with timestamp
     */
    static String getUniqueFileNameWithTimestamp(String filename) {
        final int SPLITS = 2
        String dateString = new SimpleDateFormat("yyyy-MM-dd-HHmmssSSSS", Locale.ENGLISH).format(new Date())
        String[] splittedFileName = filename.split("\\.", SPLITS)

        if (splittedFileName.size() == SPLITS) {
            return splittedFileName[0] + "_" + dateString + "." + splittedFileName[1]
        }
        return filename + "_" + dateString
    }
}
