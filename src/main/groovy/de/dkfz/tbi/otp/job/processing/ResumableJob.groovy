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
package de.dkfz.tbi.otp.job.processing

import java.lang.annotation.*

/**
 * Annotation for any Job that can be suspended and resumed safely.
 *
 * After a Job gets started it is normally in a limbo state. The Job has performed some processing
 * but is not yet finished. The execution is a critical path. Depending on what the Job performs
 * it is not possible to start the same process again. E.g. a process which triggers a long
 * computation can not be resumed if two running computation processes would interfere with each
 * other.
 *
 * By default the framework assumes that no Job can be resumed. This means when the application
 * performs a non-clean shutdown the administrator has to decide manually for each Job running at
 * the time of the crash whether the Job is still running, has succeeded or failed.
 *
 * This interface can be used by a Job which does not have a critical path and which can always be
 * resumed automatically after a non-clean application shutdown. Examples are jobs which just look
 * for a file and stop processing when the file is available.
 *
 * @see SometimesResumableJob
 */
// Produce false of UnnecessaryPackageReference and suppress warning is not allowed for annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface ResumableJob {
}
