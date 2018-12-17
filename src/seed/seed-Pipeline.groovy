import de.dkfz.tbi.otp.dataprocessing.Pipeline

seed = {
    [
            //alignments
            Pipeline.Name.PANCAN_ALIGNMENT,
            Pipeline.Name.RODDY_RNA_ALIGNMENT,
            Pipeline.Name.EXTERNALLY_PROCESSED,
            Pipeline.Name.CELL_RANGER,

            //analysis
            Pipeline.Name.RODDY_SNV,
            Pipeline.Name.RODDY_INDEL,
            Pipeline.Name.RODDY_SOPHIA,
            Pipeline.Name.RODDY_ACESEQ,
            Pipeline.Name.RUN_YAPSA,
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
