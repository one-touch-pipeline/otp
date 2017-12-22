seed = {
    """\
    WHOLE_GENOME,PAIRED,whole_genome_sequencing,WGS,WGS
    EXON,PAIRED,exon_sequencing,EXOME,WES
    WHOLE_GENOME_BISULFITE,PAIRED,whole_genome_bisulfite_sequencing,WGBS,WGBS
    WHOLE_GENOME_BISULFITE_TAGMENTATION,PAIRED,whole_genome_bisulfite_tagmentation_sequencing,WGBS_TAG,WGBSTAG
    RNA,SINGLE,rna_sequencing,RNA,RNA
    RNA,PAIRED,rna_sequencing,RNA,RNA
    ChIP Seq,SINGLE,chip_seq_sequencing,ChIP,CHIPSEQ
    ChIP Seq,PAIRED,chip_seq_sequencing,ChIP,CHIPSEQ
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
                roddyName: values[4],
        )
    }
}

