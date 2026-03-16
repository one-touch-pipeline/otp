/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.cron

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput

class UpdateDepartmentHeadsJobSpec extends Specification {

    void "getDepartmentInfo, when exit code is non-zero, throws AssertionError"() {
        given:
        UpdateDepartmentHeadsJob job = createJob(new ProcessOutput("", "", 1))

        when:
        job.departmentInfo

        then:
        thrown(AssertionError)
    }

    void "getDepartmentInfo, when stderr is non-empty, throws AssertionError"() {
        given:
        UpdateDepartmentHeadsJob job = createJob(new ProcessOutput("", "some error output", 0))

        when:
        job.departmentInfo

        then:
        thrown(AssertionError)
    }

    private UpdateDepartmentHeadsJob createJob(ProcessOutput processOutput) {
        return new UpdateDepartmentHeadsJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) >> "someScript"
                },
                remoteShellHelper: Mock(RemoteShellHelper) {
                    1 * executeCommandReturnProcessOutput(_) >> processOutput
                },
        ])
    }
}
