package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.validation.Validateable
import java.text.SimpleDateFormat

class StatisticController {

    StatisticService statisticService

    ProjectService projectService

    SeqTypeService seqTypeService

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM yy", Locale.ENGLISH)

    JSON projectCountPerDate() {
        List<String> labels = []
        List<Integer> values = []

        List data = statisticService.projectDateSortAfterDate()
        Date firstDate = new Date(data[0][1].year, data[0][1].month, 1)
        Date lastDate = new Date(data[data.size() - 1][1].year, data[data.size() - 1][1].month + 1, 1)
        int daysCount = daysBetween(firstDate, lastDate)

        int count = 1
        data.each {
            values << [
                daysBetween(firstDate, it[1]),
                count++
            ]
        }

        Calendar cal = new GregorianCalendar(firstDate.year, firstDate.month, 1)
        Calendar calend = new GregorianCalendar(lastDate.year, lastDate.month, 0)
        while (cal <= calend) {
            labels << simpleDateFormat.format(cal.getTime())
            cal.add(Calendar.MONTH, 1)
        }

        Map dataToRender = [
            labels: labels,
            data: values,
            count: count,
            gridCount: labels.size() * 4,
            daysCount: daysCount
        ]
        render dataToRender as JSON
    }

    private int daysBetween(Date date1, Date date2) {
        return (date2.getTime() - date1.getTime()) / (1000 * 60 * 60 * 24)
    }


    JSON laneCountPerDate() {
        List<String> labels = []
        List<Integer> values = []

        List data = statisticService.laneCountPerDay()
        Calendar firstDate = new GregorianCalendar(data[0][0], data[0][1] - 1, 1)
        Calendar lastDate = new GregorianCalendar(data[data.size() - 1][0], data[data.size() - 1][1], 0)
        int daysCount = daysBetween(firstDate, lastDate)

        int count = 0
        data.each {
            values << [
                daysBetween(firstDate, new GregorianCalendar(it[0], it[1] - 1, it[2])),
                count += it[3]
            ]
        }

        Calendar cal = firstDate.clone()
        while (cal <= lastDate) {
            labels << simpleDateFormat.format(cal.getTime())
            cal.add(Calendar.MONTH, 1)
        }

        Map dataToRender = [
            labels: labels,
            data: values,
            count: count,
            gridCount: labels.size() * 4,
            daysCount: daysCount
        ]
        render dataToRender as JSON
    }

    private int daysBetween(Calendar date1, Calendar date2) {
        return (date2.getTimeInMillis() - date1.getTimeInMillis()) / (1000 * 60 * 60 * 24)
    }

    public JSON projectCountPerSequenceType() {
        List<String> labels = []
        List<String> labelsProzent = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List projectCountPerSequenceType = statisticService.projectCountPerSequenceType()

        projectCountPerSequenceType.each {
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        projectCountPerSequenceType.each {
            labelsProzent << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %"
        }

        Map dataToRender = [
            labels: labels,
            labelsProzent: labelsProzent,
            data: values,
            count: values.size()
        ]
        render dataToRender as JSON
    }

    JSON sampleTypeCountBySeqType(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<String> labelsProzent = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List sampleTypeCount = statisticService.sampleTypeCountBySeqType(project)

        sampleTypeCount.each {
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        sampleTypeCount.each { labelsProzent << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %" }

        Map dataToRender = [
            labels: labels,
            labelsProzent: labelsProzent,
            data: values,
            count: values.size()
        ]
        render dataToRender as JSON
    }

    JSON sampleTypeCountByPatient(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<String> labelsProzent = []
        List<Integer> values = []

        statisticService.sampleTypeCountByPatient(project).each {
            labels << it[0]
            values << it[1]
        }

        Map dataToRender = [
            projectId: project.id,
            project: project.name,
            labels: labels,
            data: values,
            count: values.size()
        ]
        render dataToRender as JSON
    }

    JSON laneCountPerDateByProject(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<Integer> values = []

        List data = statisticService.laneCountPerDateByProject(project)
        Calendar firstDate = new GregorianCalendar(data[0][0], data[0][1]-1, 1)
        Calendar lastDate = new GregorianCalendar(data[data.size() - 1][0], data[data.size() - 1][1], 0)
        int daysCount = daysBetween(firstDate, lastDate)

        int count = 0
        data.each {
            values << [
                daysBetween(firstDate, new GregorianCalendar(it[0], it[1] - 1, it[2])),
                count += it[3]
            ]
        }

        Calendar cal = firstDate.clone()
        while (cal <= lastDate) {
            labels << simpleDateFormat.format(cal.getTime())
            cal.add(Calendar.MONTH, 1)
        }

        Map dataToRender =  [
            labels: labels,
            data: values,
            count: count,
            gridCount: labels.size() * 4,
            daysCount: daysCount
        ]
        render dataToRender as JSON
    }
}

@Validateable
class ProjectCommand implements Serializable {

    ProjectService projectService

    String projectName

    static constraints = {
        projectName(nullable: false, validator: { val, ProjectCommand obj ->
            return val && (obj.projectService.getProjectByName(val) != null)
        })
    }
}

