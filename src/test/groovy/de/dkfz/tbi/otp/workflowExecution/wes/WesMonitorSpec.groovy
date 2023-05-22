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

import grails.testing.gorm.DataTest
import io.swagger.client.wes.model.*
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.scheduler.WesMonitor
import de.dkfz.tbi.otp.workflowExecution.JobService
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

import java.time.ZonedDateTime

class WesMonitorSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private WesMonitor monitor

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                WesRun,
        ]
    }

    void setupData() {
        monitor = new WesMonitor()
        monitor.jobService = Mock(JobService) {
            0 * _
        }
        monitor.runStatusService = Mock(RunStatusService) {
            0 * _
        }
        monitor.weskitAccessService = Mock(WeskitAccessService) {
            0 * _
        }
        monitor.wesRunService = Mock(WesRunService) {
            0 * _
        }
        monitor.workflowSystemService = Mock(WorkflowSystemService) {
            0 * _
        }
    }

    void "check, when job system is disabled, then return without fetching runs"() {
        given:
        setupData()

        when:
        monitor.check()

        then:
        1 * monitor.workflowSystemService.enabled >> false
    }

    @Unroll
    void "check, when job system is enabled and one wesRun is in checking with state #state, then call methods for checking, but not for finished"() {
        given:
        setupData()

        and: 'data'
        WesRun wesRun = createWesRun()
        RunStatus runStatus = new RunStatus(state: state)

        when:
        monitor.check()

        then:
        1 * monitor.workflowSystemService.enabled >> true
        1 * monitor.wesRunService.monitoredRuns() >> [wesRun]
        1 * monitor.weskitAccessService.getRunStatus(wesRun.wesIdentifier) >> runStatus
        1 * monitor.runStatusService.isInEndState(runStatus) >> false

        where:
        state              | _
        State.UNKNOWN      | _
        State.QUEUED       | _
        State.INITIALIZING | _
        State.RUNNING      | _
        State.PAUSED       | _
        State.CANCELING    | _
    }

    @Unroll
    void "check, when job system is enabled and one wesRun is in checking and state #state and no other WesRun for that workflowStep exist, then call methods for checking and finished, but not for next job"() {
        given:
        setupData()

        and: 'data'
        WesRun wesRun = createWesRun([wesRunLog: null])
        RunStatus runStatus = new RunStatus(state: state)
        Log wesLog = createLog("runLog")
        List<Log> taskLogs = (1..3).collect {
            createLog("taskLog ${it}")
        }

        RunRequest runRequest = new RunRequest()
        RunLog runLog = new RunLog([
                runId   : wesRun.wesIdentifier,
                request : runRequest,
                state   : state,
                runLog  : wesLog,
                taskLogs: taskLogs,
        ])

        when:
        monitor.check()

        then:
        1 * monitor.workflowSystemService.enabled >> true
        1 * monitor.wesRunService.monitoredRuns() >> [wesRun]
        1 * monitor.weskitAccessService.getRunStatus(wesRun.wesIdentifier) >> runStatus
        1 * monitor.runStatusService.isInEndState(runStatus) >> true
        1 * monitor.weskitAccessService.getRunLog(wesRun.wesIdentifier) >> runLog
        1 * monitor.wesRunService.allByWorkflowStep(wesRun.workflowStep) >> [wesRun]
        1 * monitor.jobService.createNextJob(wesRun.workflowStep.workflowRun)

        WesLog.count() == 1 + taskLogs.size()
        Map<String, WesLog> wesLogMap = WesLog.list().collectEntries {
            [(it.name): it]
        }
        assertLog(wesLogMap, wesLog)
        taskLogs.each {
            assertLog(wesLogMap, it)
        }

        WesRunLog.count() == 1
        WesRunLog wesRunLog = WesRunLog.first()
        wesRunLog.state == runLog.state
        wesRunLog.runLog == wesLogMap[wesLog.name]
        wesRunLog.taskLogs == taskLogs.collect {
            wesLogMap[it.name]
        } as Set

        wesRun.state == WesRun.MonitorState.FINISHED
        wesRun.wesRunLog == wesRunLog

        where:
        state                | _
        State.COMPLETE       | _
        State.EXECUTOR_ERROR | _
        State.SYSTEM_ERROR   | _
        State.CANCELED       | _
    }

    @Unroll
    void "check, when job system is enabled and one wesRun is in checking and state #state and other finished WesRun for that workflowStep exist, then call methods for checking and finished"() {
        given:
        setupData()

        and: 'data'
        WesRun wesRun = createWesRun([wesRunLog: null])
        RunStatus runStatus = new RunStatus(state: state)
        Log wesLog = createLog("runLog")
        List<Log> taskLogs = (1..3).collect {
            createLog("taskLog ${it}")
        }

        RunRequest runRequest = new RunRequest()
        RunLog runLog = new RunLog([
                runId   : wesRun.wesIdentifier,
                request : runRequest,
                state   : state,
                runLog  : wesLog,
                taskLogs: taskLogs,
        ])

        WesRun finishedWesRun = createWesRun([
                workflowStep: wesRun.workflowStep,
                state       : WesRun.MonitorState.FINISHED,
                wesRunLog   : createWesRunLog([
                        state: State.COMPLETE,
                ]),
        ])

        when:
        monitor.check()

        then:
        1 * monitor.workflowSystemService.enabled >> true
        1 * monitor.wesRunService.monitoredRuns() >> [wesRun]
        1 * monitor.weskitAccessService.getRunStatus(wesRun.wesIdentifier) >> runStatus
        1 * monitor.runStatusService.isInEndState(runStatus) >> true
        1 * monitor.weskitAccessService.getRunLog(wesRun.wesIdentifier) >> runLog
        1 * monitor.wesRunService.allByWorkflowStep(wesRun.workflowStep) >> [wesRun, finishedWesRun]
        1 * monitor.jobService.createNextJob(wesRun.workflowStep.workflowRun)

        where:
        state                | _
        State.COMPLETE       | _
        State.EXECUTOR_ERROR | _
        State.SYSTEM_ERROR   | _
        State.CANCELED       | _
    }

    @Unroll
    void "check, when job system is enabled and one wesRun is in checking and state #state and other not finished WesRun for that workflowStep exist, then call methods till checking"() {
        given:
        setupData()

        and: 'data'
        WesRun wesRun = createWesRun([wesRunLog: null])
        RunStatus runStatus = new RunStatus(state: state)
        Log wesLog = createLog("runLog")
        List<Log> taskLogs = (1..3).collect {
            createLog("taskLog ${it}")
        }

        RunRequest runRequest = new RunRequest()
        RunLog runLog = new RunLog([
                runId   : wesRun.wesIdentifier,
                request : runRequest,
                state   : state,
                runLog  : wesLog,
                taskLogs: taskLogs,
        ])

        WesRun finishedWesRun = createWesRun([
                workflowStep: wesRun.workflowStep,
                state       : WesRun.MonitorState.CHECKING,
                wesRunLog   : createWesRunLog([
                        state: State.RUNNING,
                ]),
        ])

        when:
        monitor.check()

        then:
        1 * monitor.workflowSystemService.enabled >> true
        1 * monitor.wesRunService.monitoredRuns() >> [wesRun]
        1 * monitor.weskitAccessService.getRunStatus(wesRun.wesIdentifier) >> runStatus
        1 * monitor.runStatusService.isInEndState(runStatus) >> true
        1 * monitor.weskitAccessService.getRunLog(wesRun.wesIdentifier) >> runLog
        1 * monitor.wesRunService.allByWorkflowStep(wesRun.workflowStep) >> [wesRun, finishedWesRun]

        where:
        state                | _
        State.COMPLETE       | _
        State.EXECUTOR_ERROR | _
        State.SYSTEM_ERROR   | _
        State.CANCELED       | _
    }

    private Log createLog(String name) {
        return new Log([
                name     : name,
                cmd      : [
                        "cmd_${nextId}",
                        "cmd_${nextId}",
                        "cmd_${nextId}",
                ],
                startTime: ZonedDateTime.now().toString(),
                endTime  : ZonedDateTime.now().toString(),
                stdout   : "stdout_${nextId}",
                stderr   : "stderr_${nextId}",
                exitCode : 0,
        ])
    }

    private void assertLog(Map<String, WesLog> wesLogMap, Log log) {
        assert wesLogMap.containsKey(log.name)
        WesLog wesLog = wesLogMap[log.name]
        assert wesLog.name == log.name
        assert wesLog.cmd == log.cmd.join('\n')
        assert wesLog.startTime == ZonedDateTime.parse(log.startTime)
        assert wesLog.endTime == ZonedDateTime.parse(log.endTime)
        assert wesLog.stdout == log.stdout
        assert wesLog.stderr == log.stderr
        assert wesLog.exitCode == log.exitCode
    }
}
