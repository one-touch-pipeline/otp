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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.joda.time.*
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService

@Transactional
class StatisticService {

    ProjectService projectService

    private DateTimeFormatter simpleDateFormatter = DateTimeFormat.forPattern("MMM yyyy").withLocale(Locale.ENGLISH)

    List projectCountPerDay(ProjectGroup projectGroup) {
        return Project.withCriteria {
            projections {
                if (projectGroup) {
                    'in'("id", projectService.projectByProjectGroup(projectGroup)*.id)
                }
                groupProperty("dateCreated")
                count("name")
            }
            order("dateCreated")
        }
    }

    List sampleCountPerSequenceType(ProjectGroup projectGroup) {
        List seq = AggregateSequences.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("seqTypeDisplayName")
                count("sampleId")
            }
        }
        seq.sort { -it[1] }
        return seq
    }

    List patientsCountPerSequenceType(ProjectGroup projectGroup) {
        List seq = AggregateSequences.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("seqTypeDisplayName")
                countDistinct("mockPid")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    List laneCountPerDay(List<Project> projectList) {
        return seqTrackPropertyGroupedByDayAndFilteredByProjects("COUNT(DISTINCT st) as s", projectList)
    }

    List gigaBasesPerDay(List<Project> projectList) {
        return seqTrackPropertyGroupedByDayAndFilteredByProjects("(SUM(st.nBasePairs) / 10e9) as g", projectList)
    }

    /**
     * Query for fastq datafiles and get the aggregate grouped by a timestamp.
     *
     * @param selectAggregateHql an HQL String containing the aggregate function to use in the select
     * @param dataFileProperty the name of the property in the resulting hql
     * @param projectList whitelist for projects to use. Using null optimizes the query to use all projects.
     * @return a List of Lists, with [0] being the date and [1] being the grouped value
     */
    private List seqTrackPropertyGroupedByDayAndFilteredByProjects(String selectAggregateHql, List<Project> projectList) {
        String dataFileTimeStamp = "TO_CHAR(st.dateCreated, 'YYYY-MM-DD')"
        String hql = """\
            |SELECT
            |    ${dataFileTimeStamp},
            |    ${selectAggregateHql}
            |FROM
            |    SeqTrack st
            |WHERE
            |    NOT EXISTS (
            |        SELECT
            |            df.seqTrack
            |        FROM
            |            DataFile df
            |        WHERE
            |            df.fileWithdrawn = true
            |            AND df.seqTrack = st
            |    )
            |    ${projectList == null ? "" : "AND st.sample.individual.project.id IN (${projectList*.id?.join(", ")})"}
            |GROUP BY
            |    ${dataFileTimeStamp}
            |ORDER BY
            |    ${dataFileTimeStamp}""".stripMargin()
        return DataFile.findAll(hql)
    }

    List projectCountPerSequenceType(ProjectGroup projectGroup) {
        List seq = Sequence.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("seqTypeDisplayName")
                countDistinct("projectId")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    List sampleTypeCountBySeqType(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeDisplayName")
                count("sampleId")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    List sampleTypeCountByPatient(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("mockPid")
                countDistinct("sampleId")
            }
            order("mockPid")
        }
        return seq
    }

    /**
     * Convert creation date of data to a format to be used for scatter plots
     * @param data A list containing lists with two elements, where the first element is a date and the second element is an integer
     */
    Map dataPerDate(List data) {
        getCountPerDate(data, true)
    }

    /**
     * Convert creation date of projects to a format to be used for scatter plots
     * @param data A list containing lists with one element which is a date
     */
    Map projectCountPerDate(List data) {
        getCountPerDate(data, true)
    }

    private Map getCountPerDate(List data, boolean multiple = false) {
        List<Integer> values = []

        YearMonth firstDate = new YearMonth(data[0][0])
        YearMonth lastDate = new YearMonth(data[-1][0]).plusMonths(1)
        Days daysCount = Days.daysBetween(firstDate, lastDate)

        int count = 0
        LocalDate firstDateLocal = firstDate.toLocalDate(1)
        data.each {
            values << [
                    Days.daysBetween(firstDateLocal, new LocalDate(it[0])).days,
                    count += multiple ? (it[1] ?: 0) : 1,
            ]
        }

        Map dataToRender = [
                labels   : monthLabels(firstDate, lastDate),
                data     : values,
                count    : count,
                daysCount: daysCount.days,
        ]
        return dataToRender
    }

    private List<String> monthLabels(YearMonth firstDate, YearMonth lastDate) {
        List<String> labels = []
        YearMonth cal = firstDate
        List<Integer> validMonths
        switch (Months.monthsBetween(cal, lastDate).months) {
            case 0..20:
                validMonths = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
                break
            case 21..50:
                validMonths = [1, 4, 7, 10]
                break
            case 51..100:
                validMonths = [1, 7]
                break
            default:
                validMonths = [1]
                break
        }
        while (cal < lastDate) {
            if (validMonths.contains(cal.monthOfYear)) {
                labels << simpleDateFormatter.print(cal)
            } else {
                labels << ""
            }
            cal = cal.plusMonths(1)
        }
        return labels
    }
}
