/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

/**
 * Prepares the file system
 *  - creates a working directory
 *  - set permissions so that users can't access the work directory while the job is running
 *  - link input files, if the job requires a certain file structure
 *  - create configuration files, if required
 */
abstract class AbstractPrepareJob implements Job {
    @Override
    void execute(WorkflowStep workflowStep) {
    }

    abstract Path buildWorkDirectoryPath(WorkflowStep workflowStep)
    abstract Collection<LinkEntry> generateMapForLinking(WorkflowStep workflowStep)
}
