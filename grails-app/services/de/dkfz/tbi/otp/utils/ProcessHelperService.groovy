package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import groovy.transform.Immutable

class ProcessHelperService {

    @Immutable
    static class ProcessOutput {
        String stdout
        String stderr
        int exitCode
    }

    static Process executeCommand(String cmd) {
        assert cmd : "The input cmd must not be null"
        LogThreadLocal.getThreadLog()?.debug("executing command:\n${cmd}")
        return [ 'bash', '-c', cmd ].execute()
    }

    static ProcessOutput waitForCommand(Process process) {
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

    static ProcessOutput waitForCommand(String cmd) {
        return waitForCommand(executeCommand(cmd))
    }

    static void assertProcessFinishedSuccessful(Process process) {
        assert process : "The input process must not be null"
        assert process.exitValue() == 0 : "The exit value is not 0, but ${process.exitValue()}"
    }

    static ProcessOutput executeCommandAndAssertExistCodeAndReturnProcessOutput(String cmd) {
        Process process = executeCommand(cmd)
        ProcessOutput processOutput = waitForCommand(process)
        assertProcessFinishedSuccessful(process)
        return processOutput
    }

    static String executeAndAssertExitCodeAndErrorOutAndReturnStdout(String cmd) {
        ProcessOutput output = waitForCommand(cmd)
        assert output.stderr.empty
        assert output.exitCode == 0
        return output.stdout
    }
}
