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
String description= """

This script compares two Codenarc reports in text format, and shows all violations in the second report that are not in the first report.
It is used by the codenarc-check.sh script, but can also be used manually by executing

    groovy scripts/codenarc-diff.groovy first-report.txt second-report.txt

"""

assert args.size() == 2 : description
File fileM = new File(args[0])
File fileNew = new File(args[1])

String FILE = "File: "
String VIOLATION = "    Violation: "

Map<String, Map<String, List<String>>> violationsM = [:]
List<String> lines = fileM.readLines()
for (int i = 0; i < lines.size(); i++) {
    if (lines[i].startsWith(FILE)) {
        String fileName = lines[i].substring(FILE.length())

        Map<String, List<String>> fileViolations = [:]

        for (i++; !lines[i].empty; i++) {
            if (lines[i].startsWith(VIOLATION)) {//some messages contain newlines
                String violation = lines[i].substring(VIOLATION.length())

                String[] violationFields = violation.split(" ")
                String violationName = violationFields.first().split("=")[1]
                String violationLine = violationFields[2].split("=")[1]
                if (fileViolations.containsKey(violationName)) {
                    fileViolations.get(violationName).add(violationLine)
                } else {
                    fileViolations.put(violationName, [violationLine])
                }
            }
        }
        violationsM.put(fileName, fileViolations)
    }
}

lines = fileNew.readLines()
for (int i = 0; i < lines.size(); i++) {
    if (lines[i].startsWith(FILE)) {
        String fileName = lines[i].substring(FILE.length())

        Map<String, List<String>> fileViolations = [:]

        for (i++; !lines[i].empty; i++) {
            if (lines[i].startsWith(VIOLATION)) {//some messages contain newlines
                String violation = lines[i].substring(VIOLATION.length())

                String[] violationFields = violation.split(" ")
                String violationName = violationFields.first().split("=")[1]
                String violationLine = violationFields[2].split("=")[1]
                if (fileViolations.containsKey(violationName)) {
                    fileViolations.get(violationName).add(violationLine)
                } else {
                    fileViolations.put(violationName, [violationLine])
                }
            }
        }
        Map<String, List<String>> fileViolationsM = violationsM.get(fileName)
        if (fileViolationsM) {
            fileViolations.each {
                if (fileViolationsM.get(it.key)) {
                    if (fileViolationsM.get(it.key).size() != it.value.size()) {
                        println fileName
                        println "   " + it
                    }
                } else {
                    println fileName
                    println "   " + it
                }
            }
        } else {
            println fileName
            fileViolations.each {
                println "   " + it
            }
        }
    }
}
