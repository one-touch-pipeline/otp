/*
 * Copyright 2011-2026 The OTP authors
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

import com.sun.net.httpserver.*
import groovy.json.JsonOutput

import java.nio.charset.StandardCharsets
import java.nio.file.*

int port = 8200
String basePath = "/ga4gh/wes/v1"

HttpServer server = HttpServer.create(new InetSocketAddress(port), 0)

def runs = Collections.synchronizedMap([:])

def assertWorkflowEngineParameters(def requestJson) {
    assert requestJson.workflow_engine_parameters["max-memory"]
    assert requestJson.workflow_engine_parameters["max-runtime"]
    assert requestJson.workflow_engine_parameters["job-name"]
    assert requestJson.workflow_engine_parameters["profile"]
    assert requestJson.workflow_engine_parameters["queue"]
    assert requestJson.workflow_engine_parameters.containsKey("accounting-name")
}

def assertNfl(def requestJson) {
    assert requestJson.workflow_type == "NFL"
    assert requestJson.workflow_type_version == "23.10.1"
}

private String fastqcFileNameWithoutZipSuffixHelper(String fileName) {

    /**
     * extension replacement done in weskit (only cases used in test cases)
     */
    fileName = fileName.replaceAll('\\.tar.gz\$', '').replaceAll('\\.tar.bz2\$', '')

    /*
     * The fastqc tool does not allow to specify the output file name, only the output directory.
     * To access the file we need code to create the same name for the output file as the fastqc tool.
     * How the name is created from the input file name is looked up from the fastqc tool. The rule is in:
     * uk.ac.babraham.FastQC.Analysis.OfflineRunner.analysisComplete
     */
    String body = fileName.replaceAll("stdin:", "").replaceAll("\\.gz\$", "")
            .replaceAll("\\.bz2\$", "").replaceAll("\\.txt\$", "")
            .replaceAll("\\.fastq\$", "").replaceAll("\\.fq\$", "")
            .replaceAll("\\.csfastq\$", "").replaceAll("\\.sam\$", "")
            .replaceAll("\\.bam\$", "")
    return "${body}_fastqc.zip"
}

def handleNqSeqQc(def requestJson) {
    println "  - prepare: nq-seq-qc"
    assertNfl(requestJson)

    assert requestJson.workflow_params.input
    assert requestJson.workflow_params.outputDir

    Path zipFile = Paths.get(System.getenv("REFERENCE_DATA"), "fastqFiles/fastqc/stdin_fastqc.zip")
    println "    - reference zipFile: ${zipFile}"

    Path outputDir = Path.of(requestJson.workflow_params.outputDir)
    println "    - outputDir ${outputDir}"

    requestJson.workflow_params.input.split(",").each {
        Path fileName = Paths.get(it.trim()).last()
        Path outputSubDir = outputDir.resolve("${fileName}_reports")
        Files.createDirectory(outputSubDir)
        Path outputFile = outputSubDir.resolve(fastqcFileNameWithoutZipSuffixHelper(fileName.toString()))
        println "    - create ${outputFile}"
        Files.createSymbolicLink(outputFile, zipFile)
    }
}

def handleFileSystem(def requestJson) {
    println "    - requestJson: ${requestJson}"
    assert requestJson.tags?.run_dir: "run_dir missed"
    assert requestJson.workflow_url
    assertWorkflowEngineParameters(requestJson)

    switch (requestJson.workflow_url) {
        case "nf-seq-qc_1.2.2/main.nf":
            handleNqSeqQc(requestJson)
            break
        default:
            println "Unknown workflow or workflow version: ${requestJson.workflow_url}"
            throw new RuntimeException("Unknown workflow or workflow version: ${requestJson.workflow_url}")
    }
}

def writeJsonResponse(HttpExchange exchange, int status, def obj) {
    def response = JsonOutput.toJson(obj)
    println "  response ${status}: ${response}"
    exchange.getResponseHeaders().add("Content-Type", "application/json")
    exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length)
    exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8))
    exchange.getResponseBody().close()
}

// used for the responses without a body, so that they are logged the same way as the responses with a body
def writeEmptyResponse(HttpExchange exchange, int status) {
    println "  response ${status}: no body"
    exchange.sendResponseHeaders(status, 0)
    exchange.getResponseBody().close()
}

def createRunId() {
    return UUID.randomUUID().toString()
}

def handleWithLogging(HttpExchange exchange, Closure handler) {
    String method = exchange.getRequestMethod()
    String path = exchange.getRequestURI().getPath()
    println "START ${method} ${path}"
    try {
        handler()
    } catch (Throwable e) {
        println "ERROR ${method} ${path}"
        e.printStackTrace()
        try {
            // send the error to the client, so OTP can report it: WeskitAccessService.extractInfos adds the response body to the
            // exception message. The response uses the ErrorResponse definition of wes-api/workflow_execution_service.swagger.yaml
            writeJsonResponse(exchange, 500, [
                    msg        : "${e.class.name}: ${e.message}\n${e.stackTrace.join('\n')}".toString(),
                    status_code: 500,
            ])
        } catch (Exception ignored) {
        }
    } finally {
        // the response code of the exchange is -1, if the handler has not sent a response at all
        println "END ${method} ${path} with status ${exchange.getResponseCode()}"
    }
}

// GET /service-info
def serviceInfo = [
        workflow_type_versions            : [CWL: [workflow_type_version: ["v1.0"]]],
        supported_wes_versions            : ["1.0.0"],
        supported_filesystem_protocols    : ["http", "https", "file"],
        workflow_engine_versions          : [Cromwell: "v55"],
        default_workflow_engine_parameters: [],
        system_state_counts               : [RUNNING: 1, COMPLETE: 2],
        auth_instructions_url             : "https://example.com/auth",
        contact_info_url                  : "mailto:support@example.com",
        tags                              : [mock: "true"]
]
server.createContext("${basePath}/service-info", { exchange ->
    handleWithLogging(exchange) {
        if (exchange.getRequestMethod() == 'GET') {
            writeJsonResponse(exchange, 200, serviceInfo)
        } else {
            writeEmptyResponse(exchange, 405)
        }
    }
} as HttpHandler)

// GET /runs
server.createContext("${basePath}/runs", { exchange ->
    handleWithLogging(exchange) {
        def method = exchange.getRequestMethod()
        if (method == 'GET') {
            def runListResponse = [
                    runs           : runs.collect { k, v -> [run_id: k, state: v.state] },
                    next_page_token: ""
            ]
            writeJsonResponse(exchange, 200, runListResponse)
        } else if (method == 'POST') {
            def contentType = exchange.getRequestHeaders().getFirst("Content-Type")
            def requestJson = [:]
            def body = exchange.getRequestBody().getText('UTF-8')
            if (contentType?.toLowerCase()?.contains('application/json')) {
                requestJson = body ? groovy.json.JsonSlurper.newInstance().parseText(body) : [:]
            } else if (contentType?.toLowerCase()?.contains('application/x-www-form-urlencoded')) {
                def params = [:]
                body.split('&').each { pair ->
                    def idx = pair.indexOf('=')
                    if (idx > 0) {
                        def key = URLDecoder.decode(pair.substring(0, idx), 'UTF-8')
                        def value = URLDecoder.decode(pair.substring(idx + 1), 'UTF-8')
                        params[key] = value
                    }
                }
                // Felder, die JSON enthalten, dekodieren
                ["workflow_params", "tags", "workflow_engine_parameters"].each { k ->
                    if (params[k]) {
                        try {
                            params[k] = groovy.json.JsonSlurper.newInstance().parseText(params[k])
                        } catch (Exception e) {
                            println "WARNING: Field ${k} cannot be parsed as JSON: ${params[k]}"
                        }
                    }
                }
                requestJson = params
            } else {
                throw new RuntimeException("WARNING: Unknown Content-Type: ${contentType}")
            }
            def runId = createRunId()
            runs[runId] = [
                    run_id   : runId,
                    request  : requestJson,
                    state    : "QUEUED",
                    run_log  : [name: "mock", cmd: ["echo", "hello"], start_time: null, end_time: null, stdout: "", stderr: "", exit_code: null],
                    task_logs: [],
                    outputs  : [:]
            ]
            handleFileSystem(requestJson)
            writeJsonResponse(exchange, 200, [run_id: runId])
        } else {
            writeEmptyResponse(exchange, 405)
        }
    }
} as HttpHandler)

// GET /runs/{run_id}
server.createContext("${basePath}/runs/", { exchange ->
    handleWithLogging(exchange) {
        def method = exchange.getRequestMethod()
        def path = exchange.getRequestURI().getPath()
        def matcher = path =~ /${basePath.replaceAll("/", "\\/")}\/runs\/([^\/]+)(\/status|\/cancel)?$/
        if (matcher.matches()) {
            def runId = matcher[0][1]
            def subPath = matcher[0][2]
            def run = runs[runId]
            if (!run) {
                writeEmptyResponse(exchange, 404)
                return
            }
            if (subPath == null && method == 'GET') {
                writeJsonResponse(exchange, 200, run)
            } else if (subPath == "/status" && method == 'GET') {
                writeJsonResponse(exchange, 200, [run_id: runId, state: run.state])
                if (run.state == "QUEUED") {
                    run.state = "RUNNING"
                    run.run_log.stdout = "Workflow is running..."
                    run.run_log.start_time = new Date().format("yyyy-MM-dd'T'HH:mm:ss")
                } else if (run.state == "RUNNING") {
                    run.state = "COMPLETE"
                    run.run_log.stdout = "Workflow completed successfully."
                    run.run_log.exit_code = 0
                    run.run_log.end_time = new Date().format("yyyy-MM-dd'T'HH:mm:ss")
                }
            } else if (subPath == "/cancel" && method == 'POST') {
                run.state = "CANCELED"
                run.run_log.end_time = new Date().format("yyyy-MM-dd'T'HH:mm:ss")
                writeJsonResponse(exchange, 200, [run_id: runId])
            } else {
                writeEmptyResponse(exchange, 405)
            }
        } else {
            writeEmptyResponse(exchange, 404)
        }
    }
} as HttpHandler)

server.start()
println "WESKit Mock Server running on http://localhost:${port}${basePath}"
