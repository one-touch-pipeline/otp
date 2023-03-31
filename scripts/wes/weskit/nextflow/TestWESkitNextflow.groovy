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


/**
 * script to check connection to weskit
 *
 * for setup weskit: see https://gitlab.com/one-touch-pipeline/otp-wes-config
 */

import io.swagger.client.wes.api.WorkflowExecutionServiceApi
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.client.HttpStatusCodeException
import reactor.core.publisher.Mono

/**
 * access token from keycloak to access weskit
 */
String token = ""

/**
 * weskit host
 */
String host = ""

/**
 * protocol to use, either https or http
 */
String protocol = ""
/**
 * port to use, usually 443
 */
String port = ""

/**
 * path where the test data are located
 */
String TEST_INPUT_EXTERN = ""
/**
 * path to use for output of data
 */
String TEST_OUTPUT_EXTERN = ""

//-------------------
//work

assert token
assert host
assert protocol
assert port
assert TEST_INPUT_EXTERN
assert TEST_OUTPUT_EXTERN

WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
apiInstance.apiClient.basePath = "${protocol}://${host}:${port}/ga4gh/wes/v1"
apiInstance.apiClient.accessToken = token

def work = { String message, Closure cl ->
    try {
        println '\n------------------------------------'
        println message
        Mono mono = cl()
        println mono.block()
    } catch (HttpStatusCodeException e) {
        println """
message: ${e.message}
statuscode: ${e.statusCode}
statusText: ${e.statusText}
header: ${e.responseHeaders}
body: ${e.responseBodyAsString}
"""
    } catch (HttpMessageNotReadableException e) {
        println e
        println e.cause
    } catch (Exception e) {
        println e
        e.printStackTrace(System.out)
    }
}

work('serviceinfo') { apiInstance.serviceInfo }
work('list') { apiInstance.listRuns(10, null) }
work('run state') { apiInstance.getRunStatus('24d5d211-234b-4b24-8d8b-70ae7b1b19b0') }
work('run log') { apiInstance.getRunLog('24d5d211-234b-4b24-8d8b-70ae7b1b19b0') }

work('post') {
    String workflowParams = """
{
    "input" : "'$TEST_INPUT_EXTERN'/run1_gerald_D1VCPACXX_1_R1.sorted.fastq.tar.bz2,'$TEST_INPUT_EXTERN'/run1_gerald_D1VCPACXX_1_R1.sorted.fastq.gz", 
    "outputDir" : "'$TEST_OUTPUT_EXTERN'" }
"""
    String workflowType = "NFL"
    String workflowTypeVersion = "22.10.0"
    String tags = """ { "run_dir" : "test_${de.dkfz.tbi.util.TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}" } """
    String workflowEngineParameters = ""
    String workflowUrl = "nf-seq-qc-1.1.0/main.nf"
    List<byte[]> workflowAttachment = null

    apiInstance.runWorkflow(workflowParams, workflowType, workflowTypeVersion, tags, workflowEngineParameters, workflowUrl, workflowAttachment)
}
