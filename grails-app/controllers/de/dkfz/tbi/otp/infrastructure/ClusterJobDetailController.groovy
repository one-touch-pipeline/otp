/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.project.Project

@Secured("hasRole('ROLE_OPERATOR')")
class ClusterJobDetailController {

    ClusterJobService clusterJobService

    static allowedMethods = [
            show                     : "GET",
            showLog                  : "GET",
            getStatesTimeDistribution: "GET",
    ]

    def show() {
        ClusterJob clusterJob = ClusterJob.get(params.id as long)
        Individual individual = clusterJob ? clusterJobService.findProcessParameterObjectByClusterJob(clusterJob)?.individual : null
        Project project = clusterJob.oldSystem ? individual?.project : clusterJob.workflowStep.workflowRun.project

        return [
                'job'       : clusterJob,
                'individual': individual,
                'project'   : project,
                'NA'        : ClusterJob.NOT_AVAILABLE,
        ]
    }

    def showLog() {
        ClusterJob clusterJob = ClusterJob.get(params.id as long)
        String content = clusterJobService.getClusterJobLog(clusterJob)
        return [
                job    : clusterJob,
                content: content,
        ]
    }

    def getStatesTimeDistribution() {
        Map dataToRender = [:]

        Map<String, Map<String, Long>> data = clusterJobService.findJobSpecificStatesTimeDistributionByJobId(params.id as Long)

        dataToRender.data = ["queue", "process"].collectEntries {
            return [it, [
                    percentage: data."${it}".percentage,
                    time:       applyPeriodFormat(data."${it}".ms as Long),
                ],
            ]
        }

        render dataToRender as JSON
    }

    private String applyPeriodFormat(Long ms) {
        return PeriodFormat.getDefault().print(new Period(ms))
    }
}
