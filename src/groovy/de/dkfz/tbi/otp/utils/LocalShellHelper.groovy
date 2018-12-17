package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * @see de.dkfz.tbi.otp.job.processing.RemoteShellHelper
 */
class LocalShellHelper {

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
