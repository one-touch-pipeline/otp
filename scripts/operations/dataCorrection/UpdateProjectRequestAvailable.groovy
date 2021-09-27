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
package operations.dataCorrection

import de.dkfz.tbi.otp.project.Project

/**
 * Script to change the attribute projectRequestAvailable for a certain project.
 * The input for this script are the project names and the corresponding boolean value,
 * separated by a tabulator.
 * NOTE:
 * When copying from an excel sheet the "\t" will appear automatically. Do not delete them.
 */

String input = """
#project1 \t 1
#project2 \t TRUE
"""

Map<String, Boolean> projectValueMap = [:]
input.split("\n")*.trim().each {
    if (it && !it.startsWith('#')) {
        List<String> pvPair = it.reverse().split("\t", 2)*.reverse()*.trim()
        println("Project: ${pvPair[1]} -> Value: ${pvPair[0]}")
        projectValueMap << [(pvPair[1]): pvPair[0].toBoolean()]
    }
}

Project.withTransaction {
    projectValueMap.each { String projectName, Boolean value ->
        Project project = Project.findByName(projectName)
        project.projectRequestAvailable = value
        project.save()
    }
    assert(false)
}
