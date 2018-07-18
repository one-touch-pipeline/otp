import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        PIPELINE_RUNYAPSA_REFERENCE_GENOME,
        'hg19, hs37d5, 1KGRef_PhiX'
)
