package workflows.analysis.pair.bamfiles

import de.dkfz.tbi.otp.ngsdata.*

trait SeqTypeAndInputWesBamFiles implements SeqTypeAndInputBamFiles {


    SeqType seqTypeToUse() {
        return SeqType.exomePairedSeqType
    }


    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'wes'),
                'PLASMA_SOMEPID_EXON_PAIRED_merged.mdup.bam',
                'PLASMA_SOMEPID_EXON_PAIRED_merged.mdup.bai',
                'BLOOD_SOMEPID_EXON_PAIRED_merged.mdup.bam',
                'BLOOD_SOMEPID_EXON_PAIRED_merged.mdup.bai'
        )
    }
}
