package de.dkfz.tbi.otp.utils

import org.junit.Test

import de.dkfz.tbi.TestCase

class LocalShellHelperUnitTests {


    final String STDOUT_TEXT = "Stdout\nText"
    final String STDERR_TEXT = "Stderr\nText"
    final String COMMAND = "echo '${STDOUT_TEXT}'\n>&2 echo '${STDERR_TEXT}'"
    final String COMMAND_NO_ERROR = "echo '${STDOUT_TEXT}'"

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
