import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.ProjectService
import de.dkfz.tbi.otp.ngsdata.StatisticService
import grails.converters.JSON

/*
 * show information about OTP
 */

class InfoController {
    StatisticService statisticService
    ProjectService projectService

    def about() {
        String aboutOtp = ProcessingOptionService.findOptionSafe(ProcessingOptionService.GUI_ABOUT_OTP, null, null)
        [aboutOtp: aboutOtp,]
    }

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
        render statisticService.dataPerDate(data) as JSON
    }
}
