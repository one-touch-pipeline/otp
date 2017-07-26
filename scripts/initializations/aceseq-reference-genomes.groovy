import de.dkfz.tbi.otp.dataprocessing.AceseqService

println ctx.processingOptionService.createOrUpdate(
        Names.pipelineAceseqReferenceGenome,
        null,
        null,
        'hs37d5, 1KGRef_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm'
)
