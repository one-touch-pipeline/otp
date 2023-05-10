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
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import io.swagger.client.wes.api.WorkflowExecutionServiceApi
import io.swagger.client.wes.model.ServiceInfo
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

@Rollback
@Integration
class WeskitAccessServiceIntegrationSpec extends Specification {

    private WeskitAccessService service
    private WorkflowExecutionServiceApi api

    @TempDir
    Path baseDirectory

    void setupData() {
        api = Mock(WorkflowExecutionServiceApi) {
            0 * _
        }
        service = new WeskitAccessService()
        service.weskitApiService = Mock(WeskitApiService) {
            1 * createApiInstance() >> api
            0 * _
        }
    }

    /**
     * Test were moved to integration test because of JSON problem in unit test.
     *
     * The Json object <code>([:] as JSON)</code> throw an <code>org.grails.web.converters.exceptions.ConverterException: Unconvertable Object of class: java.util.LinkedHashMap</code> in the service on <code>toString()</code> call
     */
    void "runWorkflow, when called, then return run id object"() {
        given:
        setupData()
        String givenRunId = "1"
        Mono<ServiceInfo> mockedMonoRunId = Mock(Mono) {
            1 * block(_) >> givenRunId
        }

        and:
        Path workDir = baseDirectory.resolve('work').resolve("test")
        Files.createDirectories(workDir)

        JSON workflowParamsJson = [
                input    : "${baseDirectory}/input/run1_1_R1.fastq.gz".toString(),
                outputDir: "${baseDirectory}/output".toString(),
        ] as JSON //This JSON has make problems in unit test

        JSON tagsJson = [
                run_dir: workDir.toString(),
        ] as JSON

        String workflow = "nf-seq-qc-1.0.0/main.nf"

        WesWorkflowParameter wesWorkflowParameter = new WesWorkflowParameter(workflowParamsJson, WesWorkflowType.NEXTFLOW, workDir, workflow)

        when:
        String runId = service.runWorkflow(wesWorkflowParameter)

        then:
        1 * api.runWorkflow(_ as String, WesWorkflowType.NEXTFLOW.weskitName, WesWorkflowType.NEXTFLOW.version, _ as String, "{}", workflow, null
        ) >> { String workflowParams, String workflowType, String workflowTypeVersion, String tags, String workflowEngineParameters, String workflowUrl, List<File> workflowAttachment ->
            assert JSON.parse(workflowParams) == JSON.parse(workflowParamsJson.toString(true))
            assert JSON.parse(tags) == JSON.parse(tagsJson.toString(true))
            return mockedMonoRunId
        }

        and:
        runId == givenRunId
    }
}
