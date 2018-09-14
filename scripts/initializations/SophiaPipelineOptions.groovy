import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME,
        'SophiaWorkflow'
)

processingOptionService.createOrUpdate(
        PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS,
        '1.2.16',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS,
        '1.2.16',
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG,
        'otpSophia-1.1',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG,
        'otpSophia-1.1',
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_SOPHIA_REFERENCE_GENOME,
        'hs37d5, 1KGRef_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm'
)
