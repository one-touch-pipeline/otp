import de.dkfz.tbi.otp.dataprocessing.AceseqService

println ctx.processingOptionService.createOrUpdate(
        AceseqService.PROCESSING_OPTION_REFERENCE_KEY,
        null,
        null,
        'hs37d5, hs37d5_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm',
        "Name of reference genomes for aceseq",
)