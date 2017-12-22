import de.dkfz.tbi.otp.dataprocessing.*

seed = {
    [
            //alignments
            Pipeline.Name.PANCAN_ALIGNMENT,
            Pipeline.Name.RODDY_RNA_ALIGNMENT,
            Pipeline.Name.EXTERNALLY_PROCESSED,

            //analysis
            Pipeline.Name.RODDY_SNV,
            Pipeline.Name.RODDY_INDEL,
            Pipeline.Name.RODDY_SOPHIA,
            Pipeline.Name.RODDY_ACESEQ,
    ].each { Pipeline.Name name ->
        pipeline(
                meta: [
                        key   : 'name',
                        update: 'false',
                ],
                name: name,
                type: name.type,
        )
    }
}
