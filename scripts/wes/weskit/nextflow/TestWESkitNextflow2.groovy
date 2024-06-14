/*
 * Copyright 2011-2024 The OTP authors
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
package wes.weskit.nextflow

import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.utils.TimeFormats
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.nio.file.*

/**
 * script to check connection to weskit via {@link WeskitAccessService}
 *
 * for setup weskit: see https://gitlab.com/one-touch-pipeline/otp-wes-config
 */

// -------------------
// input

/**
 * path where the test data are located
 */
String TEST_INPUT_EXTERN = ""
/**
 * path to use for output of data
 */
String TEST_OUTPUT_EXTERN = ""

// -------------------
// work

assert TEST_INPUT_EXTERN
assert TEST_OUTPUT_EXTERN

String uuid = "24d5d211-234b-4b24-8d8b-70ae7b1b19b0"

WeskitAccessService weskitAccessService = ctx.weskitAccessService
FileSystemService fileSystemService = ctx.fileSystemService
FileSystem fileSystem = fileSystemService.remoteFileSystem

def work = { String message, Closure cl ->
    try {
        println '\n------------------------------------'
        println message
        println cl()
    } catch (Exception e) {
        println e
        e.printStackTrace(System.out)
    }
}

work('serviceInfo') { weskitAccessService.serviceInfo }

work('run state') { weskitAccessService.getRunStatus(uuid) }
work('run log') { weskitAccessService.getRunLog(uuid) }

work('runWorkflow') {
    Path workDir = fileSystem.getPath(TEST_OUTPUT_EXTERN).resolve("test_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}")
    Files.createDirectories(workDir)

    JSONObject workflowParams = [
            "input"    : "$TEST_INPUT_EXTERN/run1_gerald_D1VCPACXX_1_R1.sorted.fastq.tar.bz2,$TEST_INPUT_EXTERN/run1_gerald_D1VCPACXX_1_R1.sorted.fastq.gz",
            "outputDir": "$TEST_OUTPUT_EXTERN"
    ] as JSONObject
    String workflowUrl = "nf-seq-qc-1.1.0/main.nf"

    WesWorkflowParameter parameter = new WesWorkflowParameter(workflowParams, WesWorkflowType.NEXTFLOW, workDir, workflowUrl)

    weskitAccessService.runWorkflow(parameter)
}
