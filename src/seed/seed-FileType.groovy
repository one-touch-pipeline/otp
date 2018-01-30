import de.dkfz.tbi.otp.ngsdata.*

seed = {
    [
            '.fastq',
            '_fastq',
    ].each { String pattern ->
        fileType(
                meta: [
                        key   : [
                                'type',
                                'subType',
                                'vbpPath',
                                'signature',
                        ],
                        update: 'false',
                ],

                type: FileType.Type.SEQUENCE,
                subType: 'fastq',
                vbpPath: '/sequence/',
                signature: pattern,
        )
    }
}
