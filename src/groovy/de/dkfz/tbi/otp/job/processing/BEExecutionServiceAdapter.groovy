package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.execution.*
import de.dkfz.roddy.execution.io.*
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*


class BEExecutionServiceAdapter implements BEExecutionService {

    private final ExecutionService executionService
    private final Realm realm

    BEExecutionServiceAdapter(ExecutionService executionService, Realm realm) {
        this.executionService = executionService
        this.realm = realm
    }

    @Override
    ExecutionResult execute(String s) {
        ProcessHelperService.ProcessOutput p = executionService.executeCommandReturnProcessOutput(realm, s)
        new ExecutionResult((p.exitCode == 0), p.exitCode, p.stdout.split("\n") as List, null)
    }

    @Override
    ExecutionResult execute(String s, boolean b) {
        return execute(s)
    }

    @Override
    ExecutionResult execute(String s, boolean b, OutputStream outputStream) {
        return execute(s)
    }

    @Override
    ExecutionResult execute(Command command) {
        return execute(command.toString())
    }

    @Override
    ExecutionResult execute(Command command, boolean b) {
        return execute(command.toString())
    }

    @Override
    boolean isAvailable() {
        return true
    }

    @Override
    String handleServiceBasedJobExitStatus(Command command, ExecutionResult executionResult, OutputStream outputStream) {
        return ""
    }

    @Override
    File queryWorkingDirectory() {
        return null
    }
}
