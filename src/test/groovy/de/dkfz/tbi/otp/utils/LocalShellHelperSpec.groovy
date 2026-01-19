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
package de.dkfz.tbi.otp.utils

import spock.lang.Specification

class LocalShellHelperSpec extends Specification {

    static final String STDOUT_TEXT = "Stdout\nText"
    static final String STDERR_TEXT = "Stderr\nText"
    static final String COMMAND = "echo '${STDOUT_TEXT}'\n>&2 echo '${STDERR_TEXT}'"
    static final String COMMAND_NO_ERROR = "echo '${STDOUT_TEXT}'"

    void "test execute with null input command should fail"() {
        when:
        LocalShellHelper.execute(null)

        then:
        AssertionError e = thrown()
        e.message.contains("The input cmd must not be null")
    }

    void "test execute works correctly"() {
        given:
        StringBuffer stdout = new StringBuffer()
        StringBuffer stderr = new StringBuffer()

        when:
        Process process = LocalShellHelper.execute(COMMAND)
        process.waitForProcessOutput(stdout, stderr)

        then:
        stdout.toString().trim() == STDOUT_TEXT
        stderr.toString().trim() == STDERR_TEXT
    }

    void "test waitForProcess works correctly"() {
        given:
        Process process = [ 'bash', '-c', COMMAND ].execute()

        when:
        ProcessOutput actual = LocalShellHelper.waitForProcess(process)

        then:
        actual.stdout.trim() == STDOUT_TEXT
        actual.stderr.trim() == STDERR_TEXT
        actual.exitCode == 0
    }

    void "test waitForProcess with null input should fail"() {
        when:
        LocalShellHelper.waitForProcess(null as Process)

        then:
        AssertionError e = thrown()
        e.message.contains("The input process must not be null")
    }

    void "test executeAndWait works correctly"() {
        when:
        ProcessOutput actual = LocalShellHelper.executeAndWait(COMMAND)

        then:
        actual.stdout.trim() == STDOUT_TEXT
        actual.stderr.trim() == STDERR_TEXT
        actual.exitCode == 0
    }

    void "test executeAndWait with null input should fail"() {
        when:
        LocalShellHelper.executeAndWait(null as String)

        then:
        AssertionError e = thrown()
        e.message.contains("The input cmd must not be null")
    }

    void "test executeAndAssertExitCodeAndErrorOutAndReturnStdout works correctly"() {
        when:
        String stdout = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(COMMAND_NO_ERROR)

        then:
        stdout.toString().trim() == STDOUT_TEXT
    }

    void "test executeAndAssertExitCodeAndErrorOutAndReturnStdout with null input command should fail"() {
        when:
        LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(null)

        then:
        AssertionError e = thrown()
        e.message.contains("The input cmd must not be null")
    }

    void "test executeAndAssertExitCodeAndErrorOutAndReturnStdout with process ending with non-empty error should fail"() {
        when:
        LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(COMMAND)

        then:
        AssertionError e = thrown()
        e.message.contains("Expected stderr to be empty, but it is")
    }

    void "test executeAndAssertExitCodeAndErrorOutAndReturnStdout with process ending abnormally should fail"() {
        when:
        LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("exit 1")

        then:
        AssertionError e = thrown()
        e.message.contains("Expected exit code to be 0, but it is")
    }
}
