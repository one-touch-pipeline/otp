import de.dkfz.tbi.otp.dataprocessing.*

/**
 * The list contains customized values.
 *
 * Please fill the values.
 */

ProcessingOptionService processingOptionService = ctx.processingOptionService

[
        (ProcessingOption.OptionName.EMAIL_SENDER)                     : '',
        (ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)     : '',
        (ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)           : '',
        (ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)             : '',
        (ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME)       : '',

        (ProcessingOption.OptionName.GUI_ABOUT_OTP)                    : '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_OPERATED_BY)     : '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_PERSON_IN_CHARGE): '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS)  : '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL)   : '',


        (ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)       : '',
        (ProcessingOption.OptionName.COMMAND_FASTQC)                   : 'fastqc',
        (ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC)        : '',

        (ProcessingOption.OptionName.MAXIMUM_EXECUTED_RODDY_PROCESSES) : '10',
        (ProcessingOption.OptionName.MAXIMUM_PARALLEL_SSH_CALLS)       : '30',
        (ProcessingOption.OptionName.STATISTICS_BASES_PER_BYTES_FASTQ) : '2339',
        (ProcessingOption.OptionName.TIME_ZONE)                        : 'Europe/Berlin',

].each {
    println processingOptionService.createOrUpdate(
            it.key,
            null,
            null,
            it.value
    )
}
''
