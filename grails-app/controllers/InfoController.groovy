import de.dkfz.tbi.otp.ngsdata.ProjectService
import de.dkfz.tbi.otp.ngsdata.StatisticService
import grails.converters.JSON

/*
 * show information about OTP
 */

class InfoController {
    StatisticService statisticService
    ProjectService projectService

    def about() { }

    def numbers() { }

    def faq() { }

    def contact() { }

    def partners() { }

    JSON projectCountPerDate() {
        List data = statisticService.projectDateSortAfterDate(null)
        render statisticService.projectCountPerDate(data) as JSON
    }

    JSON laneCountPerDate() {
        List data = statisticService.laneCountPerDay(null)
        render statisticService.laneCountPerDate(data) as JSON
    }
}
