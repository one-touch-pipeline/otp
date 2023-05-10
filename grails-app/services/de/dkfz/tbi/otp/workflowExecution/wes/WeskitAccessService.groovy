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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.converters.JSON
import io.swagger.client.wes.api.WorkflowExecutionServiceApi
import io.swagger.client.wes.model.*
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

import java.nio.file.Files
import java.time.Duration

/**
 * Service to access weskit.
 *
 * This service provides simplified access to the Weskit method used in OTP.
 */
class WeskitAccessService {

    /**
     * Timeout for the weskit call.
     */
    static final Duration TIMEOUT = Duration.ofMinutes(30)

    WeskitApiService weskitApiService

    /**
     * return the server info.
     */
    ServiceInfo getServiceInfo() {
        return doApiCall { WorkflowExecutionServiceApi api ->
            api.serviceInfo
        }
    }

    /**
     * return the state of the given runId
     */
    RunStatus getRunStatus(String runId) {
        assert runId
        return doApiCall { WorkflowExecutionServiceApi api ->
            api.getRunStatus(runId)
        }
    }

    /**
     * return the log of the given runId.
     *
     * Usually, they values only available after the run ends.
     */
    RunLog getRunLog(String runId) {
        assert runId
        return doApiCall { WorkflowExecutionServiceApi api ->
            api.getRunLog(runId)
        }
    }

    /**
     * Send a new run to the workflow system
     * @param wesWorkflowParameter Parameter to the workflow
     * @return the id of the workflow run.
     */
    String runWorkflow(WesWorkflowParameter wesWorkflowParameter) {
        checkWesWorkflowParameter(wesWorkflowParameter)
        log.debug("Create new WES Run for: ${wesWorkflowParameter}")

        String tags = ([
                run_dir: wesWorkflowParameter.workDirectory.toString()
        ] as JSON).toString(true)

        return doApiCall { WorkflowExecutionServiceApi api ->
            api.runWorkflow(
                    wesWorkflowParameter.workflowParams.toString(true),
                    wesWorkflowParameter.workflowType.weskitName,
                    wesWorkflowParameter.workflowType.version,
                    tags,
                    "{}",
                    wesWorkflowParameter.workflowUrl,
                    null
            )
        }
    }

    //library use directly RuntimeException
    @SuppressWarnings('CatchRuntimeException')
    private <T> T doApiCall(Closure<Mono<T>> closure) {
        WorkflowExecutionServiceApi api = weskitApiService.createApiInstance()
        Mono<T> mono
        try {
            mono = closure(api)
        } catch (WebClientResponseException e) {
            throw new WeskitAccessException("Failed to call weskit: ${e.message}", e)
        }
        T data
        try {
            data = mono.block(TIMEOUT)
        } catch (RuntimeException e) {
            throw new WeskitHandleResponseException("Failed to handle the response of weskit: ${e.message}", e)
        }
        return data
    }

    private void checkWesWorkflowParameter(WesWorkflowParameter parameter) {
        assert parameter
        assert parameter.workflowParams
        assert parameter.workflowType
        assert parameter.workDirectory
        assert parameter.workflowUrl

        assert parameter.workDirectory.absolute
        assert Files.exists(parameter.workDirectory)
        assert Files.isDirectory(parameter.workDirectory)
        assert Files.isReadable(parameter.workDirectory)
    }
}
