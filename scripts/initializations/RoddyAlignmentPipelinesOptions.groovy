import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")



List<SeqType> wgbs = [
        SeqType.wholeGenomeBisulfitePairedSeqType,
        SeqType.wholeGenomeBisulfiteTagmentationPairedSeqType,
]


//WES
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
        "AlignmentAndQCWorkflows",
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.2.73-1",
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpAlignmentAndQCWorkflowsWES-1.3",
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqType.exomePairedSeqType.roddyName,
)


processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        "sambamba",
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
        "picard,biobambam,sambamba",
        SeqType.exomePairedSeqType.roddyName,
)


//WGS
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
        "AlignmentAndQCWorkflows",
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.2.73-1",
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpAlignmentAndQCWorkflowsWGS-1.1",
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        "sambamba",
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
        "picard,biobambam,sambamba",
        SeqType.wholeGenomePairedSeqType.roddyName,
)


//WGBS && WGBS_TAG

wgbs.each { SeqType seqType ->
    processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
            "AlignmentAndQCWorkflows",
            seqType.roddyName,
    )

    processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
            "1.2.73-1",
            seqType.roddyName,
    )

    processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
            "otpAlignmentAndQCWorkflowsWGBS-1.2",
            seqType.roddyName,
    )

    processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
            "methylCtools_hs37d5_PhiX_Lambda",
            seqType.roddyName,
    )

    processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
            "sambamba",
            seqType.roddyName,
    )

    processingOptionService.createOrUpdate(
            OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
            "sambamba",
            seqType.roddyName,
    )
}



//RNA
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
        "RNAseqWorkflow",
        SeqType.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.3.0",
        SeqType.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpRnaAlignment-1.2",
        SeqType.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqType.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_RNA_DEFAULT_GENOME_STAR_INDEX,
        "star_200 - 2.5.2b",
)


processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE,
        '0.7.8, 0.7.15',
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT,
        '0.7.15',
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE,
        '0.5.9, 0.6.5',
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT,
        '0.6.5',
)


//ChipSeq
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
        "AlignmentAndQCWorkflows",
        SeqType.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.2.73-1",
        SeqType.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpAlignmentAndQCWorkflowsChipSeq-1.0",
        SeqType.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqType.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        "sambamba",
        SeqType.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
        "sambamba",
        SeqType.chipSeqPairedSeqType.roddyName,
)
