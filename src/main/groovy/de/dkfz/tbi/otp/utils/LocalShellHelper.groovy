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

package de.dkfz.tbi.otp.utils

import groovy.transform.Immutable

import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * @see de.dkfz.tbi.otp.job.processing.RemoteShellHelper
 */
class LocalShellHelper {

    @Immutable
    static class ProcessOutput {
        String stdout
        String stderr
        int exitCode

        ProcessOutput assertExitCodeZero() {
            assert exitCode == 0 : "Expected exit code to be 0, but it is ${exitCode}\nstdout: ${stdout}\nstderr: ${stderr}"
            return this
        }

        ProcessOutput assertStderrEmpty() {
            assert stderr.isEmpty() : "Expected stderr to be empty, but it is ${stderr}"
            return this
        }

        ProcessOutput assertExitCodeZeroAndStderrEmpty() {
            assertStderrEmpty()
            assertExitCodeZero()
            return this
        }
    }

    private static Process execute(String cmd) {
        assert cmd : "The input cmd must not be null"
        LogThreadLocal.getThreadLog()?.debug("executing command:\n${cmd}")
        return [ 'bash', '-c', cmd ].execute()
    }

    private static ProcessOutput waitForProcess(Process process) {
        assert process : "The input process must not be null"
        StringBuffer stdout = new StringBuffer()
        StringBuffer stderr = new StringBuffer()
        process.waitForProcessOutput(stdout, stderr)

        ProcessOutput processOutput = new ProcessOutput(
                stdout: stdout,
                stderr: stderr,
                exitCode: process.exitValue()
        )
        LogThreadLocal.getThreadLog()?.debug("exit code:\n${processOutput.exitCode}")
        LogThreadLocal.getThreadLog()?.debug("stderr:\n${processOutput.stderr}")
        LogThreadLocal.getThreadLog()?.debug("stdout:\n${processOutput.stdout}")

        return processOutput
    }

    static ProcessOutput executeAndWait(String cmd) {
        return waitForProcess(execute(cmd))
    }

    static String executeAndAssertExitCodeAndErrorOutAndReturnStdout(String cmd) {
        return executeAndWait(cmd).assertExitCodeZeroAndStderrEmpty().stdout
    }
}
