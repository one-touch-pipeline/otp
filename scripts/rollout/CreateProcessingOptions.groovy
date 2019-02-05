/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
        (ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)             : 'otp',
        (ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME)       : '',

        (ProcessingOption.OptionName.GUI_ABOUT_OTP)                    : '',
        (ProcessingOption.OptionName.GUI_IMPRINT)                      : '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_OPERATED_BY)     : '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_PERSON_IN_CHARGE): '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS)  : '',
        (ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL)   : '',

        (ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)       : '',
        (ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC)        : 'module load fastqc/0.11.5',
        (ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY)        : 'module load groovy/2.4.15',
        (ProcessingOption.OptionName.COMMAND_ACTIVATION_SAMTOOLS)      : 'module load samtools/1.2',
        (ProcessingOption.OptionName.COMMAND_GROOVY)                   : 'groovy',
        (ProcessingOption.OptionName.COMMAND_SAMTOOLS)                 : 'samtools',
        (ProcessingOption.OptionName.COMMAND_FASTQC)                   : 'fastqc',

        (ProcessingOption.OptionName.MAXIMUM_EXECUTED_RODDY_PROCESSES) : '10',
        (ProcessingOption.OptionName.MAXIMUM_PARALLEL_SSH_CALLS)       : '30',
        (ProcessingOption.OptionName.STATISTICS_BASES_PER_BYTES_FASTQ) : '2339',
        (ProcessingOption.OptionName.TIME_ZONE)                        : 'Europe/Berlin',

].each {
    processingOptionService.createOrUpdate(
            it.key,
            it.value
    )
}
''
