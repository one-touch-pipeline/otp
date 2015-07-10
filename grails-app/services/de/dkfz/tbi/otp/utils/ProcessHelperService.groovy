package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class ProcessHelperService {

    static Process executeCommand(String cmd) {
        assert cmd : "The input cmd must not be null"
        LogThreadLocal.getThreadLog().debug("executing command:\n${cmd}")
        return [ 'bash', '-c', cmd ].execute()
    }

    static String waitForCommandAndReturnStdout(Process process) {
        assert process : "The input process must not be null"
        StringBuffer stdout = new StringBuffer()
        StringBuffer stderr = new StringBuffer()
        process.waitForProcessOutput(stdout, stderr)

        LogThreadLocal.getThreadLog().debug("stderr:\n${stderr}")
        LogThreadLocal.getThreadLog().debug("stdout:\n${stdout}")
        return stdout.toString()
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
