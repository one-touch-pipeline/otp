import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import grails.compiler.*
import grails.converters.*

import static de.dkfz.tbi.otp.administration.Document.Name.*

/*
 * show information about OTP
 */

@GrailsCompileStatic
class InfoController {
    DocumentService documentService
    StatisticService statisticService
    ProjectService projectService

    def about() {
        String aboutOtp = ProcessingOptionService.findOptionSafe(OptionName.GUI_ABOUT_OTP, null, null)
        [aboutOtp: aboutOtp,]
    }

    def imprint() {
        String imprint = ProcessingOptionService.findOptionSafe(OptionName.GUI_IMPRINT, null, null)
        [imprint: imprint]
    }

    def numbers() {
        return [projects: projectService.getProjectCount()]
    }

    def faq() {}

    def contact() {}

    def partners() {}

    def templates() {
        List<Document.Name> availableTemplates = [
                PROJECT_FORM,
                METADATA_TEMPLATE,
                PROCESSING_INFORMATION,
        ].findAll {
            documentService.getDocument(it)
        }
        return [
                availableTemplates: availableTemplates,
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
