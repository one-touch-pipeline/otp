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
package de.dkfz.tbi.otp.cron

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.tracking.DeNbiKpi
import de.dkfz.tbi.otp.tracking.DeNbiKpiService
import de.dkfz.tbi.otp.administration.MailHelperService

import java.time.LocalDate
import java.time.ZoneId

/**
 * Scheduled job to report the monthly KPIs for de.NBI Workflows.
 */
@Component
@Slf4j
class GenerateAndSendKPIsForNBI extends AbstractScheduledJob {

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    ConfigService configService

    @Autowired
    DeNbiKpiService deNbiKpiService

    /**
     * Run this job only once per month.
     * @return true if it is the first day of a month
     */
    @Override
    boolean isAdditionalRunConditionMet() {
        return LocalDate.now().dayOfMonth == 1
    }

    @Override
    void wrappedExecute() {
        LocalDate nowLastMonth = LocalDate.now().minusMonths(1)
        LocalDate fromDate = LocalDate.of(nowLastMonth.year, nowLastMonth.month, 1)
        LocalDate toDate = fromDate.plusMonths(1)

        List<DeNbiKpi> kpiList = generateKpiList(fromDate, toDate)

        String kpiMailContent = kpiList.groupBy { DeNbiKpi kpi ->
            kpi.name
        }.collect { String workflow, List<DeNbiKpi> resultDatasOfWorkflow ->
            String dataOfWorkflow = resultDatasOfWorkflow.collect { DeNbiKpi kpi ->
                int userCount = deNbiKpiService.countUsersInProjects(kpi.projects)
                [
                        kpi.count,
                        userCount,
                        kpi.projectCount,
                ].join(',')
            }.join('\n')

            return [
                    "Workflow: ${workflow}",
                    "WorkflowCount,UserCount,ProjectCount",
                    dataOfWorkflow,
            ].join('\n')
        }.join('\n\n')

        mailHelperService.saveMail("KPIs for de.NBI - ${fromDate.month} ${fromDate.year}", kpiMailContent)
    }

    /**
     * Generate a list of all de.NBI workflow KPIs in a given date range.
     *
     * @return List of de.NBI KPIs
     */
    private List<DeNbiKpi> generateKpiList(LocalDate fromDate, LocalDate toDate) {
        ZoneId zoneId = configService.timeZoneId
        Date dateFrom = Date.from(fromDate.atStartOfDay().atZone(zoneId).toInstant())
        Date dateTo = Date.from(toDate.atStartOfDay().atZone(zoneId).toInstant())

        DeNbiKpi panCanKpi = deNbiKpiService.getPanCanKpi(dateFrom, dateTo)
        DeNbiKpi rnaAlignmentKpi = deNbiKpiService.getRnaAlignmentKpi(dateFrom, dateTo)
        DeNbiKpi cellRangerKpi = deNbiKpiService.getCellRangerKpi(dateFrom, dateTo)
        DeNbiKpi snvCallingKpi = deNbiKpiService.getSnvCallingKpi(dateFrom, dateTo)
        DeNbiKpi indelKpi = deNbiKpiService.getIndelKpi(dateFrom, dateTo)
        DeNbiKpi sophiaKpi = deNbiKpiService.getSophiaKpi(dateFrom, dateTo)
        DeNbiKpi aceseqKpi = deNbiKpiService.getAceseqKpi(dateFrom, dateTo)
        DeNbiKpi runYapsaKpi = deNbiKpiService.getRunYapsaKpi(dateFrom, dateTo)
        DeNbiKpi clusterJobKpi = deNbiKpiService.getClusterJobKpi(dateFrom, dateTo)

        DeNbiKpi otpWorkflowKpi = deNbiKpiService.generateSumKpi("otp workflow", cellRangerKpi, runYapsaKpi)
        DeNbiKpi roddyKpi = deNbiKpiService.generateSumKpi("roddy", panCanKpi, rnaAlignmentKpi, snvCallingKpi, indelKpi, sophiaKpi, aceseqKpi)

        return [
                panCanKpi,
                rnaAlignmentKpi,
                cellRangerKpi,
                snvCallingKpi,
                indelKpi,
                sophiaKpi,
                aceseqKpi,
                runYapsaKpi,
                otpWorkflowKpi,
                roddyKpi,
                clusterJobKpi,
        ]
    }
}
