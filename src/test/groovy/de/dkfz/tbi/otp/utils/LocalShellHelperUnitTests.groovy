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
package de.dkfz.tbi.otp.utils

import org.junit.Test

import de.dkfz.tbi.TestCase

class LocalShellHelperUnitTests {

    static final String STDOUT_TEXT = "Stdout\nText"
    static final String STDERR_TEXT = "Stderr\nText"
    static final String COMMAND = "echo '${STDOUT_TEXT}'\n>&2 echo '${STDERR_TEXT}'"
    static final String COMMAND_NO_ERROR = "echo '${STDOUT_TEXT}'"

    @Test
    void testExecute_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            LocalShellHelper.execute(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecute_AllFine() {
        StringBuffer stdout = new StringBuffer()
        StringBuffer stderr = new StringBuffer()

        Process process = LocalShellHelper.execute(COMMAND)

        process.waitForProcessOutput(stdout, stderr)
        assert stdout.toString().trim() == STDOUT_TEXT
        assert stderr.toString().trim() == STDERR_TEXT
    }

    @Test
    void testWaitForProcess_AllFine() {
        Process process = [ 'bash', '-c', COMMAND ].execute()
        ProcessOutput actual = LocalShellHelper.waitForProcess(process)

        assert actual.stdout.trim() == STDOUT_TEXT
        assert actual.stderr.trim() == STDERR_TEXT
        assert actual.exitCode == 0
    }

    @Test
    void testWaitForProcess_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            LocalShellHelper.waitForProcess(null as Process)
        }.contains("The input process must not be null")
    }

    @Test
    void testExecuteAndWait_AllFine() {
        ProcessOutput actual = LocalShellHelper.executeAndWait(COMMAND)

        assert actual.stdout.trim() == STDOUT_TEXT
        assert actual.stderr.trim() == STDERR_TEXT
        assert actual.exitCode == 0
    }

    @Test
    void testExecuteAndWait_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            LocalShellHelper.executeAndWait(null as String)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_AllFine() {
        String stdout = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(COMMAND_NO_ERROR)

        assert stdout.toString().trim() == STDOUT_TEXT
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_ProcessEndsNotEmptyError_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(COMMAND)
        }.contains("Expected stderr to be empty, but it is")
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_ProcessEndsNotNormal_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("exit 1")
        }.contains("Expected exit code to be 0, but it is")
    }
}
