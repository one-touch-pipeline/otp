package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.impl.SimpleLog
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProcessHelperServiceUnitTests {


    final String STDOUT_TEXT = "Stdout\nText"
    final String STDERR_TEXT = "Stderr\nText"
    final String COMMAND = "echo '${STDOUT_TEXT}'\n>&2 echo '${STDERR_TEXT}'"
    final String COMMAND_NO_ERROR = "echo '${STDOUT_TEXT}'"
    final String INVALID_COMMAND = "--"

    @Before
    void setUp() {
        LogThreadLocal.setThreadLog(new SimpleLog())
    }

    @After
    void tearDown() {
        LogThreadLocal.removeThreadLog()
    }



    @Test
    void testExecuteCommand_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeCommand(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteCommand_AllFine() {
        StringBuffer stdout = new StringBuffer()
        StringBuffer stderr = new StringBuffer()

        Process process = ProcessHelperService.executeCommand(COMMAND)

        process.waitForProcessOutput(stdout, stderr)
        assert stdout.toString().trim() == STDOUT_TEXT
        assert stderr.toString().trim() == STDERR_TEXT
    }

    @Test
    void testWaitForCommand_Process_AllFine() {
        Process process = [ 'bash', '-c', COMMAND ].execute()
        ProcessHelperService.ProcessOutput actual = ProcessHelperService.waitForCommand(process)

        assert actual.stdout.trim() == STDOUT_TEXT
        assert actual.stderr.trim() == STDERR_TEXT
        assert actual.exitCode == 0
    }

    @Test
    void testWaitForCommand_Process_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.waitForCommand(null as Process)
        }.contains("The input process must not be null")
    }

    @Test
    void testWaitForCommand_String_AllFine() {
        ProcessHelperService.ProcessOutput actual = ProcessHelperService.waitForCommand(COMMAND)

        assert actual.stdout.trim() == STDOUT_TEXT
        assert actual.stderr.trim() == STDERR_TEXT
        assert actual.exitCode == 0
    }

    @Test
    void testWaitForCommand_String_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.waitForCommand(null as String)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testAssertProcessFinishedSuccessful_AllFine() {
        Process process = [ 'bash', '-c', COMMAND ].execute()
        process.waitFor()
        ProcessHelperService.assertProcessFinishedSuccessful(process)
    }

    @Test
    void testAssertProcessFinishedSuccessful_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.assertProcessFinishedSuccessful(null)
        }.contains("The input process must not be null")
    }

    @Test
    void testAssertProcessFinishedSuccessful_ProcessEndsNotNormal_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            Process process = [ 'bash', '-c', INVALID_COMMAND ].execute()
            process.waitFor()
            ProcessHelperService.assertProcessFinishedSuccessful(process)
        }.contains("The exit value is not 0")
    }

    @Test
    void testExecuteCommandAndAssertExistCodeAndReturnProcessOutput_AllFine() {
        ProcessHelperService.ProcessOutput processOutput = ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(COMMAND)

        assert processOutput.stdout.toString().trim() == STDOUT_TEXT
        assert processOutput.stderr.toString().trim() == STDERR_TEXT
    }

    @Test
    void testExecuteCommandAndAssertExistCodeAndReturnProcessOutput_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteCommandAndAssertExistCodeAndReturnProcessOutput_ProcessEndsNotNormal_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(INVALID_COMMAND)
        }.contains("The exit value is not 0")
    }


    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_AllFine() {
        String stdout = ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(COMMAND_NO_ERROR)

        assert stdout.toString().trim() == STDOUT_TEXT
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_ProcessEndsNoEmtyError_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(INVALID_COMMAND)
        }.contains("assert output.stderr.empty")
    }

    @Test
    void testExecuteAndAssertExitCodeAndErrorOutAndReturnStdout_ProcessEndsNotNormal_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("exit 1")
        }.contains("assert output.exitCode == 0")
    }

}
