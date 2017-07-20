import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

class ContactDataFilters {

    def filters = {
        all(controller:'*', action:'*') {
            after = { Map model ->
                if (model != null) {
                    model.contactDataOperatedBy = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_OPERATED_BY, null, null)
                    model.contactDataSupportEmail = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, null, null)
                    model.contactDataPersonInCharge = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_PERSON_IN_CHARGE, null, null)
                    model.contactDataPostalAddress = ProcessingOptionService.findOptionSafe(OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS, null, null)
                }
            }
        }
    }
}
