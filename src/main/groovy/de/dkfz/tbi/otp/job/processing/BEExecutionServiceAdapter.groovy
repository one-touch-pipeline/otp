/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.execution.BEExecutionService
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.tbi.otp.utils.ProcessOutput

import java.time.Duration

class BEExecutionServiceAdapter implements BEExecutionService {

    private final RemoteShellHelper remoteShellHelper

    BEExecutionServiceAdapter(RemoteShellHelper remoteShellHelper) {
        this.remoteShellHelper = remoteShellHelper
    }

    @Override
    ExecutionResult execute(String command) {
        ProcessOutput p = remoteShellHelper.executeCommandReturnProcessOutput(command)
        return new ExecutionResult([command], (p.exitCode == 0), p.exitCode, p.stdout.split("\n") as List, p.stderr.split("\n") as List)
    }

    @Override
    ExecutionResult execute(String command, boolean b) {
        return execute(command)
    }

    @Override
    ExecutionResult execute(String command, Duration duration) {
        return execute(command)
    }

    @Override
    ExecutionResult execute(String command, boolean b, Duration duration) {
        return execute(command)
    }

    @Override
    ExecutionResult execute(Command command) {
        return execute(command.toBashCommandString())
    }

    @Override
    ExecutionResult execute(Command command, boolean b) {
        return execute(command.toBashCommandString())
    }

    @Override
    ExecutionResult execute(Command command, Duration duration) {
        return execute(command.toBashCommandString())
    }

    @Override
    ExecutionResult execute(Command command, boolean b, Duration duration) {
        return execute(command.toBashCommandString())
    }

    @Override
    boolean isAvailable() {
        return true
    }

    @Override
    File queryWorkingDirectory() {
        return null
    }
}
