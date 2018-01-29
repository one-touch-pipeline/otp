import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*

/*
 * show information about OTP
 */

class InfoController {
    StatisticService statisticService
    ProjectService projectService

    def about() {
        String aboutOtp = ProcessingOptionService.findOptionSafe(OptionName.GUI_ABOUT_OTP, null, null)
        [aboutOtp: aboutOtp,]
    }

    def numbers() {
        return [projects: projectService.getAllProjects().size()]
    }

    def faq() {}

    def contact() {}

    def partners() {}

    JSON projectCountPerDate() {
        List data = statisticService.projectDateSortAfterDate(null)
        render statisticService.projectCountPerDate(data) as JSON
    }

    JSON laneCountPerDate() {
        List data = statisticService.laneCountPerDay(null)
        render statisticService.dataPerDate(data) as JSON
    }
}
