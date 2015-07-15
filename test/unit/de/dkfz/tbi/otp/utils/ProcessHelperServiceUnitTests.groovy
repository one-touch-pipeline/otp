package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.impl.SimpleLog
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProcessHelperServiceUnitTests {


    final String COMMAND_TEXT = "Hallo\nTest"
    final String COMMAND = "echo '${COMMAND_TEXT}'"
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
        assert stdout.toString().trim() == COMMAND_TEXT
    }



    @Test
    void testWaitForCommand_Process_AllFine() {
        Process process = [ 'bash', '-c', COMMAND ].execute()
        ProcessHelperService.ProcessOutput actual = ProcessHelperService.waitForCommand(process)
        String expected = COMMAND_TEXT

        assert actual.stdout.trim() == expected
        assert actual.stderr.trim().isEmpty()
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
        String expected = COMMAND_TEXT

        assert actual.stdout.trim() == expected
        assert actual.stderr.trim().isEmpty()
        assert actual.exitCode == 0
    }

    @Test
    void testWaitForCommand_String_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.waitForCommand(null as String)
        }.contains("The input cmd must not be null")
    }


    @Test
    void testWaitForCommandAndReturnStdout_AllFine() {
        Process process = [ 'bash', '-c', COMMAND ].execute()
            String actual = ProcessHelperService.waitForCommandAndReturnStdout(process)
            String expected = COMMAND_TEXT
            assert actual.trim() == expected
    }

    @Test
    void testWaitForCommandAndReturnStdout_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.waitForCommandAndReturnStdout(null)
        }.contains("The input process must not be null")
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
    void testExecuteCommandAndAssertExistCodeAndReturnStdout_AllFine() {
        String stdout = ProcessHelperService.executeCommandAndAssertExistCodeAndReturnStdout(COMMAND)

        assert stdout.toString().trim() == COMMAND_TEXT
    }

    @Test
    void testExecuteCommandAndAssertExistCodeAndReturnStdout_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeCommandAndAssertExistCodeAndReturnStdout(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteCommandAndAssertExistCodeAndReturnStdout_ProcessEndsNotNormal_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            ProcessHelperService.executeCommandAndAssertExistCodeAndReturnStdout(INVALID_COMMAND)
        }.contains("The exit value is not 0")
    }

}
