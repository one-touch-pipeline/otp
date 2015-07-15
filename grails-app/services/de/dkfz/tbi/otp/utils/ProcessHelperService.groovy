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
        LogThreadLocal.getThreadLog().debug("executing command:\n${cmd}")
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
        LogThreadLocal.getThreadLog().debug("exit code:\n${processOutput.exitCode}")
        LogThreadLocal.getThreadLog().debug("stderr:\n${processOutput.stderr}")
        LogThreadLocal.getThreadLog().debug("stdout:\n${processOutput.stdout}")

        return processOutput
    }

    static ProcessOutput waitForCommand(String cmd) {
        return waitForCommand(executeCommand(cmd))
    }

    static String waitForCommandAndReturnStdout(Process process) {
        return waitForCommand(process).stdout
    }


    static void assertProcessFinishedSuccessful(Process process) {
        assert process : "The input process must not be null"
        assert process.exitValue() == 0 : "The exit value is not 0, but ${process.exitValue()}"
    }

    static String executeCommandAndAssertExistCodeAndReturnStdout(String cmd) {
        Process process = executeCommand(cmd)
        String stdout = waitForCommandAndReturnStdout(process)
        assertProcessFinishedSuccessful(process)
        return stdout
    }

}
