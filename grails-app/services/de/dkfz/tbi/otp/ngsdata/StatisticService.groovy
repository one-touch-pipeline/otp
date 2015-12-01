package de.dkfz.tbi.otp.ngsdata

import groovy.sql.Sql
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import javax.sql.DataSource

class StatisticService {

    ProjectService projectService
    DataSource dataSource

    private DateTimeFormatter simpleDateFormatter = DateTimeFormat.forPattern("MMM yy").withLocale(Locale.ENGLISH)

    public List projectDateSortAfterDate(ProjectGroup projectGroup) {
        List seq = Sequence.withCriteria {
            projections {
                if (projectGroup) {
                    List<Project> projects = projectService.projectByProjectGroup(projectGroup)
                    'in'("projectId", projects*.id)
                }
                groupProperty("projectName")
                min("dateCreated", "minDate")
            }
            order("minDate")
        }
        return seq
    }

    public List sampleCountPerSequenceType(ProjectGroup projectGroup) {
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

    public List patientsCountPerSequenceType(ProjectGroup projectGroup) {
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

    public List laneCountPerDay(List<Project> projectList) {
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

        sql.eachRow(query) {
            laneCountPerDay << ["${it.year}-${it.month}-${it.day}" as String, it.laneCount]
        }

        return laneCountPerDay
    }

    public List projectCountPerSequenceType(ProjectGroup projectGroup) {
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

    public List sampleTypeCountBySeqType(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("seqTypeDisplayName")
                count("sampleId")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    public List sampleTypeCountByPatient(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
                countDistinct("sampleId")
            }
            order("mockPid")
        }
        return seq
    }

    /**
     * Convert creation date of lanes to a format to be used for scatter plots
     * @param data A list containing lists with two elements, where the first element is a date and the second element is an integer
     */
    public Map laneCountPerDate(List data) {
        List<Integer> values = []

        YearMonth firstDate = new YearMonth(data[0][0])
        YearMonth lastDate = new YearMonth(data[-1][0]).plusMonths(1)
        Days daysCount = Days.daysBetween(firstDate, lastDate)

        int count = 0
        LocalDate firstDateLocal = firstDate.toLocalDate(1)
        data.each {
            values << [
                Days.daysBetween(firstDateLocal, new LocalDate(it[0])).days,
                count += it[1]
            ]
        }

        Map dataToRender = [
            labels: monthLabels(firstDate, lastDate),
            data: values,
            count: count,
            daysCount: daysCount.days
        ]
        return dataToRender
    }

    /**
     * Convert creation date of projects to a format to be used for scatter plots
     * @param data A list containing lists with one element which is a date
     */
    public Map projectCountPerDate(List data) {
        List<Integer> values = []

        YearMonth firstDate = new YearMonth(data[0][1])
        YearMonth lastDate = new YearMonth(data[-1][1]).plusMonths(1)
        Days daysCount = Days.daysBetween(firstDate, lastDate)

        int count = 1
        LocalDate firstDateLocal = firstDate.toLocalDate(1)
        data.each {
            values << [
                Days.daysBetween(firstDateLocal, new LocalDate(it[1])).days,
                count++
            ]
        }

        Map dataToRender = [
            labels: monthLabels(firstDate, lastDate),
            data: values,
            count: count,
            daysCount: daysCount.days,
        ]
        return dataToRender
    }

    private List<String> monthLabels(YearMonth firstDate, YearMonth lastDate) {
        List<String> labels = []
        YearMonth cal = firstDate
        while (cal < lastDate) {
            labels << simpleDateFormatter.print(cal)
            cal = cal.plusMonths(1)
        }
        return labels
    }
}
