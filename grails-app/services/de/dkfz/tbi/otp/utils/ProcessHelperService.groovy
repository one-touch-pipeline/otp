package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import groovy.transform.Immutable

class ProcessHelperService {

    @Immutable
    static class ProcessOutput {
        String stdout
        String stderr
        int exitCode

        ProcessOutput assertExitCodeZero() {
            assert exitCode == 0 : "Expected exit code to be 0, but it is ${exitCode}"
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

    static Process execute(String cmd) {
        assert cmd : "The input cmd must not be null"
        LogThreadLocal.getThreadLog()?.debug("executing command:\n${cmd}")
        return [ 'bash', '-c', cmd ].execute()
    }

    static ProcessOutput waitForProcess(Process process) {
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
