/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Simple test implementing the Job interface without inheriting
 * AbstractJob.
 */
@Component
@Scope("prototype")
@Slf4j
class DirectTestJob implements Job {
    private ProcessingStep step

    @Override
    void execute() throws Exception {
    }

    @Override
    Set<Parameter> getOutputParameters() throws InvalidStateException {
        return [new Parameter(value: "abcd", type: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinition(step.jobDefinition)))]
    }

    @Override
    void setProcessingStep(ProcessingStep step) {
        this.step = step
    }

    @Override
    ProcessingStep getProcessingStep() {
        return step
    }

    @Override
    void start() throws InvalidStateException {
    }

    @Override
    void end() throws InvalidStateException {
    }
}
