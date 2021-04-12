/*
 * Copyright 2011-2021 The OTP authors
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

import groovy.transform.ToString
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.utils.MailHelperService

import java.time.LocalDate
import java.time.ZoneId

/**
 * Scheduled job to report the monthly KPIs for de.NBI Workflows.
 */
@Scope("singleton")
@Component
@Slf4j
class GenerateAndSendKPIsForNBI extends ScheduledJob {

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    ConfigService configService

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

        List<ResultData> resultDataList = generateKpiResultList(fromDate, toDate)

        String kpiMailReceiver = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_MONTHLY_KPI_RECEIVER)
        String kpiMailContent = resultDataList.groupBy { ResultData resultData ->
            resultData.name
        }.collect { String workflow, List<ResultData> resultDatasOfWorkflow ->
            String dataOfWorkflow = resultDatasOfWorkflow.collect {
                int userCount = usersForProjects(it.projects)
                [
                        it.count,
                        userCount,
                        it.projectCount,
                ].join(',')
            }.join('\n')

            return [
                    "Workflow: ${workflow}",
                    "WorkflowCount,UserCount,ProjectCount",
                    dataOfWorkflow,
            ].join('\n')
        }.join('\n\n')

        mailHelperService.sendEmail("KPIs for de.NBI - " + fromDate.month.name(), kpiMailContent, kpiMailReceiver)
    }

    List<ResultData> generateKpiResultList(LocalDate fromDate, LocalDate toDate) {
        ZoneId zoneId = configService.timeZoneId
        List<ResultData> resultDataList = []

        Date dateFrom = Date.from(fromDate.atStartOfDay().atZone(zoneId).toInstant())
        Date dateTo = Date.from(toDate.atStartOfDay().atZone(zoneId).toInstant())

        WorkingDataSet panCan = new WorkingDataSet("PanCan", panCanData(dateFrom, dateTo))
        WorkingDataSet rna = new WorkingDataSet("Rna alignment", rna(dateFrom, dateTo))
        WorkingDataSet singleCell = new WorkingDataSet("CellRanger", singleCell(dateFrom, dateTo))

        WorkingDataSet snv = new WorkingDataSet("Snv", analysis(dateFrom, dateTo, AbstractSnvCallingInstance))
        WorkingDataSet indel = new WorkingDataSet("Indel", analysis(dateFrom, dateTo, IndelCallingInstance))
        WorkingDataSet sophia = new WorkingDataSet("Sophia", analysis(dateFrom, dateTo, SophiaInstance))
        WorkingDataSet aceseq = new WorkingDataSet("Aceseq", analysis(dateFrom, dateTo, AceseqInstance))
        WorkingDataSet runyapsa = new WorkingDataSet("runYapsa", analysis(dateFrom, dateTo, RunYapsaInstance))

        WorkingDataSet otp = new WorkingDataSet("otp workflow", singleCell, runyapsa)
        WorkingDataSet roddy = new WorkingDataSet("roddy", panCan, rna, snv, indel, sophia, aceseq)
        WorkingDataSet otpClusterJobs = new WorkingDataSet("otp cluster jobs", clusterJobs(dateFrom, dateTo))

        [
            panCan,
            rna,
            singleCell,
            snv,
            indel,
            sophia,
            aceseq,
            runyapsa,
            otp,
            roddy,
            otpClusterJobs,
        ].each {
            resultDataList << new ResultData(it)
        }

        return resultDataList
    }

    List<Object[]> panCanData(Date from, Date to) {
        return RoddyBamFile.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            workPackage {
                seqType {
                    ne("name", "RNA")
                    eq("singleCell", false)
                }
            }
            projections {
                property('id')
                workPackage {
                    sample {
                        individual {
                            project {
                                property('name')
                            }
                        }
                    }
                }
            }
        } as List<Object[]>
    }

    List<Object[]> rna(Date from, Date to) {
        return RnaRoddyBamFile.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            workPackage {
                seqType {
                    eq("singleCell", false)
                    eq("name", "RNA")
                }
            }
            projections {
                property('id')
                workPackage {
                    sample {
                        individual {
                            project {
                                property('name')
                            }
                        }
                    }
                }
            }
        } as List<Object[]>
    }

    List<Object[]> singleCell(Date from, Date to) {
        return AbstractMergedBamFile.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            workPackage {
                seqType {
                    eq("singleCell", true)
                }
            }
            projections {
                property('id')
                workPackage {
                    sample {
                        individual {
                            project {
                                property('name')
                            }
                        }
                    }
                }
            }
        } as List<Object[]>
    }

    List<Object[]> analysis(Date from, Date to, Class<BamFilePairAnalysis> instance) {
        return instance.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            projections {
                property('id')
                samplePair {
                    mergingWorkPackage1 {
                        sample {
                            individual {
                                project {
                                    property('name')
                                }
                            }
                        }
                    }
                }
            }
        } as List<Object[]>
    }

    List<Object[]> clusterJobs(Date from, Date to) {
        return ClusterJob.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            projections {
                property('id')
                individual {
                    project {
                        property('name')
                    }
                }
            }
        } as List<Object[]>
    }

    int usersForProjects(List<String> projectNames) {
        if (!projectNames) {
            return 0
        }

        return UserProjectRole.createCriteria().list {
            eq("enabled", true)
            user {
                eq("enabled", true)
            }
            project {
                'in'('name', projectNames)
            }
        }*.user.unique().size()
    }
}

@ToString
@TupleConstructor
class WorkingDataSet {
    String name
    int workflowCount
    List<String> projects

    WorkingDataSet(String name, List<Object[]> result) {
        this.name = name
        workflowCount = result.size()
        projects = result.collect { it[1] }.unique() as List<String>
    }

    WorkingDataSet(String name, WorkingDataSet... dataList) {
        this.name = name
        workflowCount = dataList*.workflowCount.sum() as int
        projects = dataList*.projects.flatten().unique() as List<String>
    }
}

@ToString
class ResultData {
    String name
    int count
    int projectCount
    List<String> projects

    ResultData(WorkingDataSet dataSet) {
        name = dataSet.name
        count = dataSet.workflowCount
        projectCount = dataSet.projects.size()
        projects = dataSet.projects
    }
}
