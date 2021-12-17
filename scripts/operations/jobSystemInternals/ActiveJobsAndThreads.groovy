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

import de.dkfz.tbi.otp.job.processing.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
println "SCHEDULER ACTIVE"
println ctx.schedulerService.active
println ""
println "SCHEDULER QUEUE"
ctx.schedulerService.queue.each {
    println it
}
println ""
println "RUNNING JOBS"
ctx.schedulerService.running.each {
    println "${it}\t(ProcessingStep ID ${it.processingStep.id}, ${it.processingStep.process.jobExecutionPlan.name} on ${atMostOneElement(ProcessParameter.findAllByProcess(it.processingStep.process))?.toObject()}"
}
println ""
println "THREADS"
Thread.allStackTraces.each {
    println "${it.key} (ID ${it.key.id})"
    it.value.each {
        println "  ${it}"
    }
}
"EOF"
