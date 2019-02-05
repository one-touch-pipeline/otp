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

import groovy.sql.Sql
import org.joda.time.*
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import javax.sql.DataSource

class StatisticService {

    ProjectService projectService
    DataSource dataSource

    private DateTimeFormatter simpleDateFormatter = DateTimeFormat.forPattern("MMM yyyy").withLocale(Locale.ENGLISH)

    List projectDateSortAfterDate(ProjectGroup projectGroup) {
        List seq = Sequence.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                min("dateCreated", "minDate")
                groupProperty("projectName")
            }
            order("minDate")
        }
        return seq
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
            order("seqTypeDisplayName")
        }
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
        List<Project> projects = projectList ?: Project.findAll()

        def sql = new Sql(dataSource)

        String query = '''
SELECT
 date_part('year', seq.date_created)::int as year,
 date_part('month', seq.date_created)::int as month,
 date_part('day', seq.date_created)::int as day,
 count(seq.seq_track_id)::int as laneCount
 FROM sequences as seq
 WHERE seq.seq_track_id NOT IN (
SELECT DISTINCT seq.seq_track_id
 FROM sequences as seq
 JOIN data_file as df
 ON seq.seq_track_id = df.seq_track_id
 WHERE df.file_withdrawn != false) AND
 seq.project_id IN (''' + projects*.id.join(", ") + ''')
 GROUP BY
 date_part('year', seq.date_created),
 date_part('month', seq.date_created),
 date_part('day', seq.date_created)
 ORDER BY
 date_part('year', seq.date_created),
 date_part('month', seq.date_created),
 date_part('day', seq.date_created)
'''
        List laneCountPerDay = []
        if (projects) {
            sql.eachRow(query) {
                laneCountPerDay << ["${it.year}-${it.month}-${it.day}" as String, it.laneCount]
            }
        }

        return laneCountPerDay
    }

    List gigaBasesPerDay(List<Project> projectList) {
        List<Project> projects = projectList ?: Project.findAll()

        def sql = new Sql(dataSource)

        String query = '''
SELECT
    date_part('year', seq.date_created)::int as year,
    date_part('month', seq.date_created)::int as month,
    date_part('day', seq.date_created)::int as day,
    (sum(seq.n_base_pairs) / 10e9)::int as gigaBases
FROM
    sequences as seq
WHERE
    seq.seq_track_id NOT IN (
        SELECT DISTINCT seq.seq_track_id
        FROM sequences as seq
        JOIN data_file as df ON seq.seq_track_id = df.seq_track_id
        WHERE
            df.file_withdrawn != false
    ) AND seq.project_id IN (''' + projects*.id.join(", ") + ''')
 GROUP BY
    date_part('year', seq.date_created),
    date_part('month', seq.date_created),
    date_part('day', seq.date_created)
 ORDER BY
    date_part('year', seq.date_created),
    date_part('month', seq.date_created),
    date_part('day', seq.date_created)
'''
        List gigaBasesPerDay = []

        if (projects) {
            sql.eachRow(query) {
                gigaBasesPerDay << ["${it.year}-${it.month}-${it.day}" as String, it.gigaBases]
            }
        }

        return gigaBasesPerDay
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
        getCountPerDate(data)
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
