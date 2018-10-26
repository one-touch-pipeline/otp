package workflows.analysis.pair.bamfiles

import de.dkfz.tbi.otp.ngsdata.*

trait SeqTypeAndInputWesBigBamFiles implements SeqTypeAndInputBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.exomePairedSeqType
    }

    @Override
    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'wesbig'),
                'tumor_merged.mdup.bam',
                'tumor_merged.mdup.bam.bai',
                'control_merged.mdup.bam',
                'control_merged.mdup.bam.bai'
        )
    }
}
