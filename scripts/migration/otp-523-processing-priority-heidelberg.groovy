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
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

/**
 * Script to adapt the processing priorities. It is not to create new one.
 */

//---------------------------
//input

//processingPriorityName, queue,emailPrefix
String input = """
MINIMAL,prio5-ERROR,prod-prio5
REPROCESSING,prio4-ERROR,prod-prio4
NORMAL,prio3-ERROR,prod-prio3
FASTTRACK,prio2-ERROR,prod-prio2
EXTREME FASTTRACK,prio1-ERROR,prod-prio1
"""

//--------------------------------------------
//processing

ProcessingPriority.withTransaction {
    input.split('\n')*.trim().findAll {
        it && !it.startsWith('#')
    }.each { String line ->
        String[] split = line.split(',')
        assert split.size() == 3: "There are exactly three columns require, not ${split.size()} (${split})"
        ProcessingPriority processingPriority = CollectionUtils.exactlyOneElement(ProcessingPriority.findAllByName(split[0]),
                "Could not find one ProcessingPriority with name ${split[0]}")

        processingPriority.with {
            queue = split[1]
            roddyConfigSuffix = split[1]
            errorMailPrefix = split[2]
            save()
        }
    }
}

println "Current ProcesingPriorities:"
println ProcessingPriority.list().collect {
    [
            it.name,
            it.queue,
            it.roddyConfigSuffix,
            it.errorMailPrefix,
    ].join('\t')
}.join('\n')
