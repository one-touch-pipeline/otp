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
package de.dkfz.tbi.otp.workflow.scheduler

import grails.gorm.transactions.Transactional
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.swagger.client.wes.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflowExecution.JobService
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
@Slf4j
class WesMonitor {

    @Autowired
    ConfigService configService

    @Autowired
    JobService jobService

    @Autowired
    RunStatusService runStatusService

    @Autowired
    WeskitAccessService weskitAccessService

    @Autowired
    WesRunService wesRunService

    @Autowired
    WorkflowSystemService workflowSystemService

    @Scheduled(fixedDelay = 30000L)
    void check() {
        if (!workflowSystemService.enabled) {
            return // job system is inactive
        }

        List<WesRun> wesRuns = LogUsedTimeUtils.logUsedTime(log, "fetch wesRuns to monitor from database") {
            wesRunService.monitoredRuns()
        }
        LogUsedTimeUtils.logUsedTimeStartEnd(log, "Check ${wesRuns.size()} WesRuns") {
            wesRuns.each { WesRun wesRun ->
                checkJob(wesRun)
            }
        }
    }

    private void checkJob(WesRun wesRun) {
        RunStatus runStatus
        try {
            runStatus = weskitAccessService.getRunStatus(wesRun.wesIdentifier)
        } catch (AbstractWeskitException e) {
            log.debug("Fail to fetch state for ${wesRun.wesIdentifier} (${e.message}), retry next round")
            return
        }
        boolean endState = runStatusService.isInEndState(runStatus)
        log.debug("state for ${wesRun.wesIdentifier}: ${endState ? 'Finished' : 'Running'} (${runStatus.state})")
        if (endState) {
            handleFinishedJob(wesRun)
        }
    }

    @Transactional
    private void handleFinishedJob(WesRun wesRun) {
        RunLog runLog = weskitAccessService.getRunLog(wesRun.wesIdentifier)

        wesRun.wesRunLog = createWesRunLog(runLog)
        wesRun.state = WesRun.MonitorState.FINISHED
        wesRun.save(flush: true)

        List<WesRun> wesRuns = wesRunService.allByWorkflowStep(wesRun.workflowStep)
        if (wesRuns.every {
            it.state == WesRun.MonitorState.FINISHED
        }) {
            log.debug("all WesRuns finished for ${wesRun.workflowStep}")
            jobService.createNextJob(wesRun.workflowStep.workflowRun)
        }
    }

    private WesRunLog createWesRunLog(RunLog runLog) {
        List<WesLog> taskLogs = runLog.taskLogs.collect {
            new WesLog([
                    name     : it.name,
                    cmd      : it.cmd.join('\n'),
                    startTime: LocalDateTime.parse(it.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(configService.timeZoneId),
                    endTime  : LocalDateTime.parse(it.endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(configService.timeZoneId),
                    stdout   : it.stdout,
                    stderr   : it.stderr,
                    exitCode : it.exitCode,
            ]).save(flush: true)
        }

        Log logLog = runLog.runLog
        WesLog wesLog = new WesLog([
                name     : logLog.name,
                cmd      : logLog.cmd.join('\n'),
                startTime: LocalDateTime.parse(logLog.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(configService.timeZoneId),
                endTime  : LocalDateTime.parse(logLog.endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(configService.timeZoneId),
                stdout   : logLog.stdout,
                stderr   : logLog.stderr,
                exitCode : logLog.exitCode,
        ]).save(flush: true)

        WesRunLog wesRunLog = new WesRunLog([
                state     : runLog.state,
                taskLogs  : taskLogs,
                runLog    : wesLog,
                runRequest: new JsonBuilder(runLog.request).toPrettyString(),
        ]).save(flush: true)

        return wesRunLog
    }
}
