seed = {
    """\
    WHOLE_GENOME,PAIRED,whole_genome_sequencing,WGS,WGS,false
    EXON,PAIRED,exon_sequencing,EXOME,WES,false
    WHOLE_GENOME_BISULFITE,PAIRED,whole_genome_bisulfite_sequencing,WGBS,WGBS,false
    WHOLE_GENOME_BISULFITE_TAGMENTATION,PAIRED,whole_genome_bisulfite_tagmentation_sequencing,WGBS_TAG,WGBSTAG,false
    RNA,SINGLE,rna_sequencing,RNA,RNA,false
    RNA,PAIRED,rna_sequencing,RNA,RNA,false
    ChIP Seq,SINGLE,chip_seq_sequencing,ChIP,CHIPSEQ,false
    ChIP Seq,PAIRED,chip_seq_sequencing,ChIP,CHIPSEQ,false
    10x_scRNA,PAIRED,10x_scRNA_sequencing,10x_scRNA,,true
    """.split('\n')*.trim().findAll().each { String row ->
        List<String> values = row.split(',')
        seqType(
                meta: [
                        key   : [
                                'name',
                                'libraryLayout',
                        ],
                        update: 'false',
                ],
                name: values[0],
                libraryLayout: values[1],
                dirName: values[2],
                displayName: values[3],
                roddyName: values[4] ?: null,
                singleCell: values[5].toBoolean(),
        )
    }
}

