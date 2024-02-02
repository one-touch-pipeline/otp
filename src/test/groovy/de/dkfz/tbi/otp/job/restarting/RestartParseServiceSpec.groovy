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
package de.dkfz.tbi.otp.job.restarting

import grails.testing.gorm.DataTest
import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.plan.JobErrorDefinition
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils

import java.nio.file.Path

@SuppressWarnings("UnnecessaryGetter")
class RestartParseServiceSpec extends Specification implements DataTest {

    @TempDir
    Path tempDir

    @Unroll
    void "extractMatchingAction, when text is '#text', matches (#match) JobErrorDefinition with errorExpression '#errorExpression'"() {
        given:
        JobErrorDefinition jobErrorDefinition = new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_WF, errorExpression: errorExpression)
        RestartParseService service = new RestartParseService()
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
        }

        expect:
        match == (service.extractMatchingAction(job, 'it', text, [jobErrorDefinition]) == JobErrorDefinition.Action.RESTART_WF)

        where:
        text              | errorExpression   || match
        'test'            | /abc/             || false
        'test'            | /test/            || true
        'bla test bla'    | /abc/             || false
        'bla test bla'    | /test/            || true
        'test1\ntest2'    | /test1\ntest2/    || true
        'bla\ntest\nbla'  | /test/            || true
        'bla\rtest\rbla'  | /test/            || true
        'bla test1 bla'   | /(?:test1|test2)/ || true
        'test bla test'   | /test.*test/      || true
        'test\nbla\ntest' | /test.*test/      || false
        'test\nbla\ntest' | /test(.|\s)*test/ || true
        'test\rbla\rtest' | /test(.|\s)*test/ || true
        'test\tbla\ttest' | /test(.|\s)*test/ || true
    }

    @Unroll
    void "extractMatchingAction returns #expectedAction for actions #actions"() {
        given:
        List<JobErrorDefinition> jobErrorDefinitions = actions.collect {
            new JobErrorDefinition(action: it, errorExpression: 'match')
        }
        RestartParseService service = new RestartParseService()
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: new ProcessingError(errorMessage: /match/)
                )
            }
        }

        expect:
        expectedAction == service.extractMatchingAction(job, 'test', 'match', jobErrorDefinitions)

        where:
        actions                                                                       || expectedAction
        []                                                                            || null
        [JobErrorDefinition.Action.STOP]                                              || JobErrorDefinition.Action.STOP
        [JobErrorDefinition.Action.RESTART_JOB]                                       || JobErrorDefinition.Action.RESTART_JOB
        [JobErrorDefinition.Action.RESTART_WF]                                        || JobErrorDefinition.Action.RESTART_WF
        [JobErrorDefinition.Action.RESTART_JOB, JobErrorDefinition.Action.RESTART_WF] || JobErrorDefinition.Action.STOP
    }

    @Unroll
    void "test handleTypeMessage (#text)"() {
        given:
        List<JobErrorDefinition> jobErrorDefinitions = createJobErrorDefinitions()
        RestartParseService service = new RestartParseService()
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: new ProcessingError(errorMessage: text)
                )
            }
        }

        expect:
        expectedAction == service.handleTypeMessage(job, jobErrorDefinitions)

        where:
        text   || expectedAction
        'abc'  || null
        'Test' || JobErrorDefinition.Action.RESTART_WF
    }

    @Unroll
    void "test handleTypeStackTrace (#text)"() {
        given:
        final String stacktraceIdentifier = HelperUtils.randomMd5sum
        List<JobErrorDefinition> jobErrorDefinitions = createJobErrorDefinitions()
        RestartParseService service = new RestartParseService(
                errorLogService: Mock(ErrorLogService) {
                    1 * getStackTracesFile(_) >> {
                        File file = tempDir.resolve("test.txt").toFile()
                        file.text = text
                        return file
                    }
                }
        )
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: new ProcessingError(stackTraceIdentifier: stacktraceIdentifier)
                )
            }
        }

        expect:
        expectedAction == service.handleTypeStackTrace(job, jobErrorDefinitions)

        where:
        text   || expectedAction
        'abc'  || null
        'Test' || JobErrorDefinition.Action.RESTART_WF
    }

    void "handleTypeStackTrace, when stack trace file does not exist, returns null"() {
        given:
        final String stacktraceIdentifier = HelperUtils.randomMd5sum
        List<JobErrorDefinition> jobErrorDefinitions = createJobErrorDefinitions()
        RestartParseService service = new RestartParseService(
                errorLogService: Mock(ErrorLogService) {
                    1 * getStackTracesFile(_) >> {
                        return TestCase.uniqueNonExistentPath
                    }
                }
        )
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: new ProcessingError(stackTraceIdentifier: stacktraceIdentifier)
                )
            }
        }

        expect:
        null == service.handleTypeStackTrace(job, jobErrorDefinitions)
    }

    @Unroll
    void "test handleTypeClusterLogs '#name' "() {
        given:
        List<JobErrorDefinition> jobErrorDefinitions = [
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_WF, errorExpression: /Test1/),
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_WF, errorExpression: /Test2/),
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_JOB, errorExpression: /Test3/),
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_JOB, errorExpression: /Test4/),
        ]
        RestartParseService service = new RestartParseService()
        Job job = GroovyMock(Job) {
            1 * failedOrNotFinishedClusterJobs() >> {
                if (text.empty) {
                    return []
                }
                return text.collect { String content ->
                    new ClusterJob(
                            jobLog: {
                                if (content) {
                                    Path fileName = tempDir.resolve(HelperUtils.randomMd5sum)
                                    Path file = CreateFileHelper.createFile(fileName, content)
                                    return file.toString()
                                }
                                return TestCase.uniqueNonExistentPath.absolutePath
                            }()
                    )
                }
            }
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: new ProcessingError()
                )
            }
        }

        expect:
        expectedAction == service.handleTypeClusterLogs(job, jobErrorDefinitions)

        where:
        name                                         | text                                            || expectedAction
        'no log file'                                | []                                              || JobErrorDefinition.Action.STOP
        'one file, not existing'                     | [null]                                          || null
        'one file, not matching'                     | ['abc']                                         || null
        'one file, match case wf'                    | ['Test2']                                       || JobErrorDefinition.Action.RESTART_WF
        'one file, match case job'                   | ['Test3']                                       || JobErrorDefinition.Action.RESTART_JOB
        'one file, match both cases'                 | ['Test2 Test3']                                 || JobErrorDefinition.Action.STOP
        'two files, no matches'                      | ['abc', 'def']                                  || null
        'two files, one matching, case 1'            | ['Test1', 'def']                                || JobErrorDefinition.Action.RESTART_WF
        'two files, one matching, case 2'            | ['abc', 'Test3']                                || JobErrorDefinition.Action.RESTART_JOB
        'two files, both matches case 1'             | ['Test1', 'Test2']                              || JobErrorDefinition.Action.RESTART_WF
        'two files, both matches both cases'         | ['Test2', 'Test3']                              || JobErrorDefinition.Action.STOP
        'two files, only one exist and match'        | ['Test1', null]                                 || JobErrorDefinition.Action.RESTART_WF
        'many files, some exist, matching one cases' | [null, 'abc', null, 'Test1', null, 'abc', null] || JobErrorDefinition.Action.RESTART_WF
    }

    @Unroll
    void "detectAndHandleType calls correct method for #type"() {
        given:
        List<JobErrorDefinition> jobErrorDefinitions = [
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_WF, errorExpression: /Test/, type: type),
        ]
        RestartParseService service = new RestartParseService(
                errorLogService: Mock(ErrorLogService) {
                    countStackTrace * getStackTracesFile(_) >> {
                        return TestCase.uniqueNonExistentPath
                    }
                },
        )
        Job job = GroovyMock(Job) {
            countClusterLog * failedOrNotFinishedClusterJobs() >> [
                    new ClusterJob([
                            jobLog: TestCase.uniqueNonExistentPath.absolutePath,
                    ])
            ]
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: Mock(ProcessingError) {
                            countMessage * getErrorMessage() >> ''
                            countStackTrace * getStackTraceIdentifier() >> ''
                        }
                )
            }
        }

        expect:
        null == service.detectAndHandleType(job, jobErrorDefinitions)

        where:
        type                                || countMessage || countStackTrace || countClusterLog
        JobErrorDefinition.Type.MESSAGE     || 1            || 0               || 0
        JobErrorDefinition.Type.STACKTRACE  || 0            || 1               || 0
        JobErrorDefinition.Type.CLUSTER_LOG || 0            || 0               || 1
    }

    @Unroll
    void "test detectAndHandleType with recursion (message=#message, stacktrace=#stacktrace, cluster=#cluster, action=#action)"() {
        given:
        final String stacktraceIdentifier = HelperUtils.randomMd5sum
        List<JobErrorDefinition> jobErrorDefinitions = [
                new JobErrorDefinition(
                        type: JobErrorDefinition.Type.MESSAGE,
                        action: JobErrorDefinition.Action.CHECK_FURTHER,
                        errorExpression: /m1/,
                        checkFurtherJobErrors: [
                                new JobErrorDefinition(
                                        type: JobErrorDefinition.Type.STACKTRACE,
                                        action: JobErrorDefinition.Action.CHECK_FURTHER,
                                        errorExpression: /s1/,
                                        checkFurtherJobErrors: [
                                                new JobErrorDefinition(
                                                        type: JobErrorDefinition.Type.CLUSTER_LOG,
                                                        action: JobErrorDefinition.Action.RESTART_WF,
                                                        errorExpression: /c1/,
                                                ),
                                                new JobErrorDefinition(
                                                        type: JobErrorDefinition.Type.CLUSTER_LOG,
                                                        action: JobErrorDefinition.Action.RESTART_JOB,
                                                        errorExpression: /c2/,
                                                ),
                                        ]
                                ),
                                new JobErrorDefinition(
                                        type: JobErrorDefinition.Type.STACKTRACE,
                                        action: JobErrorDefinition.Action.CHECK_FURTHER,
                                        errorExpression: /s2/,
                                        checkFurtherJobErrors: [
                                                new JobErrorDefinition(
                                                        type: JobErrorDefinition.Type.CLUSTER_LOG,
                                                        action: JobErrorDefinition.Action.RESTART_JOB,
                                                        errorExpression: /c1/,
                                                ),
                                                new JobErrorDefinition(
                                                        type: JobErrorDefinition.Type.CLUSTER_LOG,
                                                        action: JobErrorDefinition.Action.RESTART_WF,
                                                        errorExpression: /c2/,
                                                ),
                                        ]
                                ),
                        ]
                ),
                new JobErrorDefinition(
                        type: JobErrorDefinition.Type.MESSAGE,
                        action: JobErrorDefinition.Action.CHECK_FURTHER,
                        errorExpression: /m2/,
                        checkFurtherJobErrors: [
                                new JobErrorDefinition(
                                        type: JobErrorDefinition.Type.CLUSTER_LOG,
                                        action: JobErrorDefinition.Action.RESTART_JOB,
                                        errorExpression: /c1/,
                                ),
                                new JobErrorDefinition(
                                        type: JobErrorDefinition.Type.CLUSTER_LOG,
                                        action: JobErrorDefinition.Action.RESTART_WF,
                                        errorExpression: /c2/,
                                ),
                        ]
                ),
        ]

        RestartParseService service = new RestartParseService(
                errorLogService: Mock(ErrorLogService) {
                    getStackTracesFile(_) >> {
                        File file = tempDir.resolve("test.txt").toFile()
                        file.text = stacktrace
                        return file
                    }
                }
        )

        Job job = GroovyMock(Job) {
            _ * failedOrNotFinishedClusterJobs() >> [
                    new ClusterJob([
                            jobLog: CreateFileHelper.createFile(tempDir.resolve("text2.txt").toFile(), cluster).absolutePath,
                    ])
            ]
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> Mock(ProcessingStep) {
                _ * getLatestProcessingStepUpdate() >> new ProcessingStepUpdate(
                        error: Mock(ProcessingError) {
                            getErrorMessage() >> message
                            getStackTraceIdentifier() >> stacktraceIdentifier
                        }
                )
            }
        }

        expect:
        action == service.detectAndHandleType(job, jobErrorDefinitions)

        where:
        message   | stacktrace | cluster   || action
        'm1'      | 's1'       | 'c1'      || JobErrorDefinition.Action.RESTART_WF
        'm1'      | 's1'       | 'c2'      || JobErrorDefinition.Action.RESTART_JOB
        'm1'      | 's2'       | 'c1'      || JobErrorDefinition.Action.RESTART_JOB
        'm1'      | 's2'       | 'c2'      || JobErrorDefinition.Action.RESTART_WF
        'm2'      | 'unknown'  | 'c1'      || JobErrorDefinition.Action.RESTART_JOB
        'm2'      | 'unknown'  | 'c2'      || JobErrorDefinition.Action.RESTART_WF
        'm1'      | 's1'       | 'unknown' || null
        'm1'      | 'unknown'  | 'c1'      || null
        'unknown' | 's1'       | 'c1'      || null
    }

    private List<JobErrorDefinition> createJobErrorDefinitions() {
        return [
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_WF, errorExpression: /Test/),
                new JobErrorDefinition(action: JobErrorDefinition.Action.RESTART_JOB, errorExpression: /Something/),
        ]
    }
}
