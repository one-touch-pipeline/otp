/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

seed = {
    """\
    WHOLE_GENOME,PAIRED,whole_genome_sequencing,WGS,WGS,false,false,false
    EXON,PAIRED,exon_sequencing,EXOME,WES,false,false,true
    WHOLE_GENOME_BISULFITE,PAIRED,whole_genome_bisulfite_sequencing,WGBS,WGBS,false,false,false
    WHOLE_GENOME_BISULFITE_TAGMENTATION,PAIRED,whole_genome_bisulfite_tagmentation_sequencing,WGBS_TAG,WGBSTAG,false,false,false
    RNA,SINGLE,rna_sequencing,RNA,RNA,false,false,false
    RNA,PAIRED,rna_sequencing,RNA,RNA,false,false,false
    ChIP Seq,SINGLE,chip_seq_sequencing,ChIP,CHIPSEQ,false,true,false
    ChIP Seq,PAIRED,chip_seq_sequencing,ChIP,CHIPSEQ,false,true,false
    10x_scRNA,PAIRED,10x_scRNA_sequencing,10x_scRNA,,true,false,false
    """.split('\n')*.trim().findAll().each { String row ->
        List<String> values = row.split(',')
        assert values.size() == 8

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
                hasAntibodyTarget: values[6].toBoolean(),
                needsBedFile: values[7].toBoolean()
        )
    }
}

