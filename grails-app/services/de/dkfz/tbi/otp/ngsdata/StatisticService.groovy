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

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.TimeFormats

import java.time.*

@Transactional
class StatisticService {

    private String createDateTimeFormatString(String entity) {
        return "TO_CHAR(${entity}.dateCreated, 'YYYY-MM-DD')"
    }

    ProjectService projectService

    List projectCountPerDay(ProjectGroup projectGroup) {
        String dateTimeFormatString = createDateTimeFormatString('project')
        String hql = """\
            |SELECT
            |    ${dateTimeFormatString},
            |    COUNT(DISTINCT project.id)
            |FROM
            |    Project project
            |${projectGroup == null ? "" : "WHERE project.projectGroup.id = ${projectGroup.id}"}
            |GROUP BY
            |    ${dateTimeFormatString}
            |ORDER BY
            |    ${dateTimeFormatString}""".stripMargin()

        return Project.findAll(hql)
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
        String dateTimeFormatString = createDateTimeFormatString('st')
        String hql = """\
            |SELECT
            |    ${dateTimeFormatString},
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
            |    ${dateTimeFormatString}
            |ORDER BY
            |    ${dateTimeFormatString}""".stripMargin()
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
        return getCountPerDate(data)
    }

    /**
     * Convert creation date of projects to a format to be used for scatter plots
     * @param data A list containing lists with one element which is a date
     */
    Map projectCountPerDate(List data) {
        return getCountPerDate(data)
    }

    private Map getCountPerDate(List data) {
        LocalDate firstDate = LocalDate.parse(data[0][0])
        LocalDate lastDate = LocalDate.parse(data[-1][0]).plusMonths(1)

        List<Map<String, String>> initialLabels = monthLabels(firstDate, lastDate).collect { [x: it, y: 0] }

        List<Map<String, String>> result = data
                .collect { [x: TimeFormats.MONTH_YEAR.getFormattedLocalDate(LocalDate.parse(it[0])), y: it[1] ?: 0] }
                .inject(initialLabels) { List<Map<String, String>> acc, Map<String, String> element ->
                    Map<String, String> duplicateEntry = acc.find({ el -> el?.x == element.x })
                    if (duplicateEntry) {
                    duplicateEntry.y += element.y
                    } else {
                    acc += element
                    }
                    return acc
                }

        List<Map<String, String>> injectLabels2 = [[x: result.first().x, y: 0]]
        result = result.inject(injectLabels2) { List<Map<String, String>> acc, Map<String, String> element ->
            element.y = acc.last().y + element.y
            return acc + element
        }

        Map dataToRender = [
                data  : result,
        ]
        return dataToRender
    }

    @SuppressWarnings('DuplicateNumberLiteral')
    private List<String> monthLabels(LocalDate firstDate, LocalDate lastDate) {
        List<Integer> validMonths
        Period period = Period.between(firstDate, lastDate)
        int months = period.toTotalMonths()

        switch (months) {
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

        List<String> labels = []
        for (LocalDate currentDate = firstDate; currentDate < lastDate; currentDate = currentDate.plusMonths(1)) {
            if (validMonths.contains(currentDate.month.value)) {
                labels << TimeFormats.MONTH_YEAR.getFormattedLocalDate(currentDate)
            } else {
                labels << ""
            }
        }
        return labels
    }
}
