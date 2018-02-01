import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")



List<SeqType> wgbs = [
        SeqType.wholeGenomeBisulfitePairedSeqType,
        SeqType.wholeGenomeBisulfiteTagmentationPairedSeqType,
]


//general

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME,
        null,
        null,
        "AlignmentAndQCWorkflows"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION,
        null,
        null,
        "1.2.73-1"
)

//WES

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "otpAlignmentAndQCWorkflowsWES-1.2"
)

println processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "1KGRef_PhiX"
)


println processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "sambamba"
)

println processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "picard,biobambam,sambamba"
)


//WGS

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "otpAlignmentAndQCWorkflowsWGS-1.2"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "1KGRef_PhiX"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "sambamba"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "picard,biobambam,sambamba"
)


//WGBS && WGBS_TAG

wgbs.each { SeqType seqType ->
    println processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG,
            seqType.roddyName,
            null,
            "otpAlignmentAndQCWorkflowsWGBS-1.2"
    )

    println processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
            seqType.roddyName,
            null,
            "methylCtools_hs37d5_PhiX_Lambda"
    )

    println processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
            seqType.roddyName,
            null,
            "sambamba"
    )

    println processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS,
            seqType.roddyName,
            null,
            "sambamba"
    )
}



//RNA
println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME,
        SeqType.rnaPairedSeqType.roddyName,
        null,
        "RNAseqWorkflow"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION,
        SeqType.rnaPairedSeqType.roddyName,
        null,
        "1.2.22-6"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG,
        SeqType.rnaPairedSeqType.roddyName,
        null,
        "otpRnaAlignment-1.2"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        SeqType.rnaPairedSeqType.roddyName,
        null,
        "1KGRef_PhiX"
)

println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_GENOME_STAR_INDEX,
        SeqType.rnaPairedSeqType.roddyName,
        null,
        "star_200 - 2.5.2b"
)


processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE,
        null,
        null,
        '0.7.8, 0.7.15'
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT,
        null,
        null,
        '0.7.15'
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE,
        null,
        null,
        '0.5.9, 0.6.5'
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT,
        null,
        null,
        '0.6.5'
)


//ChipSeq

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG,
        SeqType.chipSeqPairedSeqType.roddyName,
        null,
        "otpAlignmentAndQCWorkflowsChipSeq-1.0"
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        SeqType.chipSeqPairedSeqType.roddyName,
        null,
        "1KGRef_PhiX"
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        SeqType.chipSeqPairedSeqType.roddyName,
        null,
        "sambamba"
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS,
        SeqType.chipSeqPairedSeqType.roddyName,
        null,
        "sambamba"
)
