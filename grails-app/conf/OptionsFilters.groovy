import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import groovy.transform.*

class OptionsFilters {

    def filters = {
        all(controller:'*', action:'*') {
            after = { Map model ->
                if (model != null) {
                    model.contactDataOperatedBy = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_OPERATED_BY, null, null)
                    model.contactDataSupportEmail = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, null, null)
                    model.contactDataPersonInCharge = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_PERSON_IN_CHARGE, null, null)
                    model.contactDataPostalAddress = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS, null, null)

                    Logo logo = Logo.NONE
                    try {
                        logo = Logo.valueOf(ProcessingOptionService.findOptionSafe(OptionName.GUI_LOGO, null, null))
                    } catch (IllegalArgumentException e) {}
                    model.logo = logo.fileName
                }
            }
        }
    }
}

@TupleConstructor
enum Logo {
    NONE("header-empty.png"),
    CHARITE("header-charite.png"),
    DKFZ("header-dkfz.png"),

    final String fileName
}
