import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.roddy.RoddyConstants

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")



List<SeqType> wgbs = [
        SeqType.wholeGenomeBisulfitePairedSeqType,
        SeqType.wholeGenomeBisulfiteTagmentationPairedSeqType,
]


//general

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_RODDY_ALIGNMENT_PLUGIN_NAME,
        null,
        null,
        "AlignmentAndQCWorkflows",
        "Name of the alignment plugin, used in configure alignment",
)

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_RODDY_ALIGNMENT_PLUGIN_VERSION,
        null,
        null,
        "1.1.73",
        "The version of the roddy alignment plugin"
)

//WES

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_BASE_PROJECT_CONFIG,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "otpAlignmentAndQCWorkflowsWES-1.1",
        "The base project file for WES alignment"
)

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_DEFAULT_REFERENCE_GENOME,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "1KGRef_PhiX",
        "Default reference genome for WES"
)


println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_DEFAULT_MERGE_TOOL,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "sambamba",
        "Default merge tool for WES"
)

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_ALL_MERGE_TOOLS,
        SeqType.exomePairedSeqType.roddyName,
        null,
        "picard,biobambam,sambamba",
        "All merge tools for WES"
)


//WGS

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_BASE_PROJECT_CONFIG,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "otpAlignmentAndQCWorkflowsWGS-1.1",
        "The base project file for WGS alignment"
)

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_DEFAULT_REFERENCE_GENOME,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "1KGRef_PhiX",
        "Default reference genome for WGS"
)

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_DEFAULT_MERGE_TOOL,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "sambamba",
        "Default merge tool for WGS"
)

println processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_ALL_MERGE_TOOLS,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        "picard,biobambam,sambamba",
        "All merge tools for WGS"
)


//WGBS && WGBS_TAG

wgbs.each { SeqType seqType ->
    println processingOptionService.createOrUpdate(
            RoddyConstants.OPTION_KEY_BASE_PROJECT_CONFIG,
            seqType.roddyName,
            null,
            "otpAlignmentAndQCWorkflowsWGBS-1.1",
            "The base project file for alignment"
    )

    println processingOptionService.createOrUpdate(
            RoddyConstants.OPTION_KEY_DEFAULT_REFERENCE_GENOME,
            seqType.roddyName,
            null,
            "methylCtools_hs37d5_PhiX_Lambda",
            "Default reference genome for ${seqType}"
    )

    println processingOptionService.createOrUpdate(
            RoddyConstants.OPTION_KEY_DEFAULT_MERGE_TOOL,
            seqType.roddyName,
            null,
            "sambamba",
            "Default merge tool for ${seqType}"
    )

    println processingOptionService.createOrUpdate(
            RoddyConstants.OPTION_KEY_ALL_MERGE_TOOLS,
            seqType.roddyName,
            null,
            "sambamba",
            "All merge tools for ${seqType}"
    )
}



processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_BWA_VERSION_AVAILABLE,
        null,
        null,
        '0.7.8, 0.7.15',
        'Available versions for alignment with bwa_mem'
)
processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_BWA_VERSION_DEFAULT,
        null,
        null,
        '0.7.15',
        'Default version for alignment with bwa_mem'
)
processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SAMBAMBA_VERSION_AVAILABLE,
        null,
        null,
        '0.5.9, 0.6.5',
        'Available versions for merging and duplication marking with sambamba'
)
processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SAMBAMBA_VERSION_DEFAULT,
        null,
        null,
        '0.6.5',
        'Default version for merging and duplication marking with sambamba'
)

processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SAMBAMBA_PATHS,
        "0.5.9",
        null,
        '/path/to/programs/sambamba/sambamba-0.5.9/bin/sambamba',
        'Path to sambamba 0.5.9'
)

processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SAMBAMBA_PATHS,
        "0.6.5",
        null,
        '/path/to/programs/sambamba/sambamba-0.6.5/bin/sambamba',
        'Path to sambamba 0.6.5'
)

processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_BWA_PATHS,
        "0.7.8",
        null,
        '/path/to/programs/bwa/bwa-0.7.8/bwa',
        'Path to bwa_mem 0.7.8'
)
processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_BWA_PATHS,
        "0.7.15",
        null,
        '/path/to/programs/bwa/bwa-0.7.15/bin/bwa',
        'Path to bwa_mem 0.7.15'
)
