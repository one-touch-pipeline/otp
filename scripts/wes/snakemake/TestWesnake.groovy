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

/**
 * setup wesnake: see README
 */

import io.swagger.client.wes.api.WorkflowExecutionServiceApi
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.client.HttpStatusCodeException

WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
apiInstance.apiClient.basePath = 'http://localhost:9000/ga4gh/wes/v1'

def work = { String message, Closure cl ->
    try {
        println '\n------------------------------------'
        println message
        println cl()
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
work('run state') { apiInstance.getRunStatus('1') }
work('run log') { apiInstance.getRunLog('1') }

work('post') {
    String workflowParams = """
{
}
""" //TODO
    String workflowType = "snakemake"
    String workflowTypeVersion = "5.8.2"
    String tags = ""
    String workflowEngineParameters = ""
    String workflowUrl = "" //TODO
    List<byte[]> workflowAttachment = null

    apiInstance.runWorkflow(workflowParams, workflowType, workflowTypeVersion, tags, workflowEngineParameters, workflowUrl, workflowAttachment)
}
