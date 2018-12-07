import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import grails.compiler.*
import grails.converters.*

/*
 * show information about OTP
 */

@GrailsCompileStatic
class InfoController {
    DocumentService documentService
    StatisticService statisticService
    ProjectService projectService
    ProcessingOptionService processingOptionService

    def about() {
        String aboutOtp = processingOptionService.findOptionAsString(OptionName.GUI_ABOUT_OTP)
        return [
                aboutOtp: aboutOtp,
        ]
    }

    def imprint() {
        String imprint = processingOptionService.findOptionAsString(OptionName.GUI_IMPRINT)
        return [
                imprint: imprint,
        ]
    }

    def numbers() {
        return [
                projects: projectService.getProjectCount()
        ]
    }

    def dicom() {
        return [:]
    }

    def faq() { }

    def contact() { }

    def partners() { }

    def templates() {
        return [
                availableTemplates: documentService.listDocuments(),
        ]
    }


    JSON projectCountPerDate() {
        List data = statisticService.projectDateSortAfterDate(null)
        render statisticService.projectCountPerDate(data) as JSON
    }

    JSON laneCountPerDate() {
        List data = statisticService.laneCountPerDay(null)
        render statisticService.dataPerDate(data) as JSON
    }
}
