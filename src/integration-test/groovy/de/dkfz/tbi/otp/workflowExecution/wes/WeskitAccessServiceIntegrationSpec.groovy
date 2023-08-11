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
import io.swagger.client.wes.model.RunId
import org.grails.web.json.JSONObject
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.LocalShellHelper

import java.nio.file.*

@Rollback
@Integration
class WeskitAccessServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    private WeskitAccessService service
    private WorkflowExecutionServiceApi api

    ConfigService configService

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
        service.configService = configService
        service.fileService = new FileService()
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
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
        Mono<RunId> mockedMonoRunId = Mock(Mono) {
            1 * block(_) >> new RunId().runId(givenRunId)
        }
        findOrCreateProcessingOption(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, createRealm().name)

        and:
        Path workDir = baseDirectory.resolve('work').resolve("test")
        Files.createDirectories(workDir)

        JSONObject workflowParamsJson = [
                input    : "${baseDirectory}/input/run1_1_R1.fastq.gz".toString(),
                outputDir: "${baseDirectory}/output".toString(),
        ] as JSONObject

        JSON tagsJson = [
                run_dir: Paths.get('/tmp').relativize(workDir).toString(),
        ] as JSON

        String workflow = "nf-seq-qc-1.0.0/main.nf"

        WesWorkflowParameter wesWorkflowParameter = new WesWorkflowParameter(workflowParamsJson, WesWorkflowType.NEXTFLOW, workDir, workflow)

        when:
        RunId runId = service.runWorkflow(wesWorkflowParameter)

        then:
        1 * api.runWorkflow(_ as String, WesWorkflowType.NEXTFLOW.weskitName, WesWorkflowType.NEXTFLOW.version, _ as String, "{}", workflow, null
        ) >> { String workflowParams, String workflowType, String workflowTypeVersion, String tags, String workflowEngineParameters, String workflowUrl, List<File> workflowAttachment ->
            assert JSON.parse(workflowParams) == workflowParamsJson
            assert JSON.parse(tags) == JSON.parse(tagsJson.toString(true))
            return mockedMonoRunId
        }

        and:
        runId.runId == givenRunId
    }
}
