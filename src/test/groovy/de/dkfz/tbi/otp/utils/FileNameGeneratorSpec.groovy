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

import spock.lang.Specification

class FileNameGeneratorSpec extends Specification {

    void getUniqueFileNameWithTimestamp_shouldReturnUniqueFilenames_whenGivenFileNamesAreEqual() {
        given:
        String fileName = "sample.txt"

        when:
        String generatedName1 = FileNameGenerator.getUniqueFileNameWithTimestamp(fileName)
        Thread.sleep(1)
        String generatedName2 = FileNameGenerator.getUniqueFileNameWithTimestamp(fileName)

        then:
        generatedName1 != generatedName2
    }

    void getUniqueFileNameWithTimestamp_shouldContainOriginalFileName_whenFullFileNameIsGiven() {
        given:
        String origFileName = "sample.txt.temp"

        when:
        String newFileName = FileNameGenerator.getUniqueFileNameWithTimestamp(origFileName)

        then:
        newFileName.contains(origFileName.split("\\.", 2).first())
    }

    void getUniqueFileNameWithTimestamp_shouldContainOriginalFileName_whenNoFileEndingIsGiven() {
        given:
        String fileNameWithoutType = "sample"

        when:
        String generatedName = FileNameGenerator.getUniqueFileNameWithTimestamp(fileNameWithoutType)

        then:
        generatedName.contains(fileNameWithoutType)
    }

    void getUniqueFileNameWithTimestamp_shouldContainOriginalFileName_whenNoPrefixIsGivenInOriginalName() {
        given:
        String fileNameWithoutPrefix = ".sample"

        when:
        String generatedName = FileNameGenerator.getUniqueFileNameWithTimestamp(fileNameWithoutPrefix)

        then:
        generatedName.contains(fileNameWithoutPrefix)
    }

    void getUniqueFileNameWithTimestamp_shouldReturnFileName_whenMultipleSuffixAreGiven() {
        given:
        String fileNameWithMultipleSuffix = "sample.txt.temp.third"

        when:
        String generatedName = FileNameGenerator.getUniqueFileNameWithTimestamp(fileNameWithMultipleSuffix)

        then:
        generatedName != ""
    }

    void getUniqueFileNameWithTimestamp_shouldContainOriginalFileName_whenMultipleSuffixAreGiven() {
        given:
        String fileNameWithMultipleSuffix = "sample.txt.temp.third"

        when:
        String generatedName = FileNameGenerator.getUniqueFileNameWithTimestamp(fileNameWithMultipleSuffix)

        then:
        generatedName.contains(fileNameWithMultipleSuffix.split("\\.", 2)[0])
        generatedName.contains(fileNameWithMultipleSuffix.split("\\.", 2)[1])
    }
}
