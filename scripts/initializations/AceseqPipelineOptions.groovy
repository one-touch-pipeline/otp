import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        PIPELINE_ACESEQ_DEFAULT_PLUGIN_NAME,
        'ACEseqWorkflow'
)

SeqType.aceseqPipelineSeqTypes.each { SeqType seqType ->
    processingOptionService.createOrUpdate(
            PIPELINE_ACESEQ_DEFAULT_PLUGIN_VERSION,
            '1.2.8-4',
            seqType.roddyName,
    )

    processingOptionService.createOrUpdate(
            PIPELINE_ACESEQ_DEFAULT_BASE_PROJECT_CONFIG,
            'otpACEseq-1.1',
            seqType.roddyName,
    )
}

processingOptionService.createOrUpdate(
        PIPELINE_ACESEQ_REFERENCE_GENOME,
        'hs37d5, 1KGRef_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm'
)
