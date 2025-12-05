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

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BJobs {

    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM d HH:mm:ss yyyy", Locale.ENGLISH)

    static void main(String[] args) {
        if (!args) {
            System.err.println("No args")
            System.exit(1)
        }
        boolean all = args.length == 7

        assert args.length in [7, 8]
        assert args[0] == '-a'
        assert args[1] == '-hms'
        assert args[2] == '-json'
        assert args[3] == '-o'
        assert args[4] == all ?
                "jobid job_name stat user queue job_description proj_name job_group job_priority pids exit_code from_host exec_host submit_time start_time finish_time cpu_used run_time user_group swap max_mem runtimelimit sub_cwd pend_reason exec_cwd output_file input_file effective_resreq exec_home slots error_file command dependency" :
                "jobid job_name stat finish_time"
        assert args[5] == '-u'
        String user = args[6]
        String clusterId = all ? null : args[7]

        Path basePath = Path.of(System.getenv("HOME"), 'jobs')

        if (all) {
            monitor(basePath)
        } else {
            statistic(basePath, user, clusterId)
        }
    }

    static void monitor(Path basePath) {
        List<Map> records = []

        Files.list(basePath).withCloseable { stream ->
            stream.each { Path path ->
                Path state = path.resolve('state')
                Path exitCode = path.resolve("exitCode")

                String stateText = state.text.trim()

                Map recordMap = [
                        "JOBID"      : "${path.fileName}",
                        "JOB_NAME"   : "jobName",
                        "STAT"       : "${stateText}",
                        "FINISH_TIME": "${stateText == 'DONE' || stateText == 'EXIT' ? FORMATTER.format(LocalDateTime.now()) : ''}"
                ]
                String recordsString = recordMap.collect {
                    "            \"${it.key}\": \"${it.value}\""
                }.join(",\n")

                records << "        {\n${recordsString}\n        }"

                //update state, if not finished
                if (stateText == "PEND") {
                    state.text = "RUN\n"
                } else if (stateText == "RUN") {
                    state.text = (Files.exists(exitCode) && exitCode.text.trim() == "0") ? "DONE\n" : "EXIT\n"
                }
            }
        }

        String recordsString = records.join(",\n")

        Map completeMap = [
                "COMMAND": "bjobs",
                "JOBS"   : records.size().toString(),
                "RECORDS": "[\n${recordsString}\n    ]"

        ]
        String all = createValidJson(completeMap)

        String jsonOutput = "{\n${all}\n}"

        println jsonOutput
    }

    static void statistic(Path basePath, String user, String clusterId) {
        Path state = basePath.resolve(clusterId).resolve('state')
        Path exitCode = basePath.resolve(clusterId).resolve("exitCode")
        Path output = basePath.resolve(clusterId).resolve("output")

        String stateText = state.text.trim()

        Map jobResource = [
                JOBID           : clusterId,
                JOB_NAME        : "JOB_NAME",
                STAT            : stateText,
                USER            : user,
                QUEUE           : "queue",
                JOB_DESCRIPTION : "",
                PROJ_NAME       : "default",
                JOB_GROUP       : "",
                JOB_PRIORITY    : "",
                PIDS            : "",
                EXIT_CODE       : "",
                FROM_HOST       : "FROM_HOST",
                EXEC_HOST       : "",
                SUBMIT_TIME     : FORMATTER.format(LocalDateTime.now().minusHours(2)),
                START_TIME      : "",
                FINISH_TIME     : "",
                CPU_USED        : "",
                RUN_TIME        : "00:00:00",
                USER_GROUP      : "USER",
                SWAP            : "",
                MAX_MEM         : "",
                RUNTIMELIMIT    : "01:00:00",
                SUB_CWD         : "\$HOME",
                PEND_REASON     : "New job is waiting for scheduling;",
                EXEC_CWD        : "",
                OUTPUT_FILE     : output.toString(),
                INPUT_FILE      : "",
                EFFECTIVE_RESREQ: "",
                EXEC_HOME       : "",
                SLOTS           : "",
                ERROR_FILE      : "",
                COMMAND         : "#!\\/bin\\/bash ;# OTP command",
                DEPENDENCY      : ""
        ]

        if (state.text in ["DONE", "EXIT"]) {
            jobResource.putAll([
                    JOB_PRIORITY    : "25",
                    PIDS            : "49461,49492,49496",
                    EXIT_CODE       : exitCode.text.trim(),
                    EXEC_HOST       : "EXEC_HOST",
                    START_TIME      : FORMATTER.format(LocalDateTime.now().minusHours(1)),
                    FINISH_TIME     : FORMATTER.format(LocalDateTime.now().minusMinutes(5)),
                    CPU_USED        : "00:01:00",
                    RUN_TIME        : "00:05:00",
                    MAX_MEM         : "100 Mbytes",
                    PEND_REASON     : "",
                    EXEC_CWD        : "\\/home\\/otp",
                    EFFECTIVE_RESREQ: "select[type == local] order[r15s:pg] rusage[mem=5120.00] ",
                    EXEC_HOME       : "\\/home\\/otp",
                    SLOTS           : "1",
            ])
        }

        String jobResourcesString = jobResource.collect {
            "        \"${it.key}\": \"${it.value}\""
        }.join(",\n")

        Map<String, String> completeMap = [
                "COMMAND": "bjobs",
                "JOBS"   : "1",
                "RECORDS": "[\n      {\n${jobResourcesString}\n      }\n    ]"
        ]

        String all = createValidJson(completeMap)

        String jsonOutput = "{\n${all}\n}"

        println jsonOutput
    }

    static String createValidJson(Map map) {
        return map.collect { String key, String value ->
            String valueWrappedIfNeeded = value.startsWith('[') ? value : "\"${value}\""
            "    \"${key}\": ${valueWrappedIfNeeded}"
        }.join(",\n")
    }
}
