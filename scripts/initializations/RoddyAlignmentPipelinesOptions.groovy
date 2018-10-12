import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")



List<SeqType> wgbs = [
        SeqTypeService.wholeGenomeBisulfitePairedSeqType,
        SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
]


//WES
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
        "AlignmentAndQCWorkflows",
        SeqTypeService.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.2.73-1",
        SeqTypeService.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpAlignmentAndQCWorkflowsWES-1.3",
        SeqTypeService.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqTypeService.exomePairedSeqType.roddyName,
)


processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        "sambamba",
        SeqTypeService.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
        "picard,biobambam,sambamba",
        SeqTypeService.exomePairedSeqType.roddyName,
)


//WGS
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME,
        "AlignmentAndQCWorkflows",
        SeqTypeService.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.2.73-1",
        SeqTypeService.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpAlignmentAndQCWorkflowsWGS-1.1",
        SeqTypeService.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqTypeService.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        "sambamba",
        SeqTypeService.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
        "picard,biobambam,sambamba",
        SeqTypeService.wholeGenomePairedSeqType.roddyName,
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
        SeqTypeService.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.3.0",
        SeqTypeService.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpRnaAlignment-1.2",
        SeqTypeService.rnaPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqTypeService.rnaPairedSeqType.roddyName,
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
        SeqTypeService.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
        "1.2.73-1",
        SeqTypeService.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG,
        "otpAlignmentAndQCWorkflowsChipSeq-1.0",
        SeqTypeService.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME,
        "1KGRef_PhiX",
        SeqTypeService.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL,
        "sambamba",
        SeqTypeService.chipSeqPairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS,
        "sambamba",
        SeqTypeService.chipSeqPairedSeqType.roddyName,
)
