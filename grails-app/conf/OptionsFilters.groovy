import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

class OptionsFilters {
    ProcessingOptionService processingOptionService

    def filters = {
        all(controller:'*', action:'*') {
            after = { Map model ->
                if (model != null) {
                    model.contactDataOperatedBy = processingOptionService.findOptionAsString(OptionName.GUI_CONTACT_DATA_OPERATED_BY)
                    model.contactDataSupportEmail = processingOptionService.findOptionAsString(OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL)
                    model.contactDataPersonInCharge = processingOptionService.findOptionAsString(OptionName.GUI_CONTACT_DATA_PERSON_IN_CHARGE)
                    model.contactDataPostalAddress = processingOptionService.findOptionAsString(OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS)
                    model.piwikUrl = processingOptionService.findOptionAsString(OptionName.GUI_TRACKING_PIWIK_URL)
                    model.piwikSiteId = processingOptionService.findOptionAsInteger(OptionName.GUI_TRACKING_SITE_ID)
                    model.piwikEnabled = processingOptionService.findOptionAsBoolean(OptionName.GUI_TRACKING_ENABLED)
                    model.showPartners = processingOptionService.findOptionAsBoolean(OptionName.GUI_SHOW_PARTNERS)

                    InstanceLogo logo = InstanceLogo.NONE
                    try {
                        logo = InstanceLogo.valueOf(processingOptionService.findOptionAsString(OptionName.GUI_LOGO))
                    } catch (IllegalArgumentException e) {}
                    model.logo = logo.fileName
                }
            }
        }
    }
}

