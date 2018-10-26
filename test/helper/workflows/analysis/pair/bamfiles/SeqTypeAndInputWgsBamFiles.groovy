package workflows.analysis.pair.bamfiles

import de.dkfz.tbi.otp.ngsdata.*

trait SeqTypeAndInputWgsBamFiles implements SeqTypeAndInputBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }

    @Override
    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'wgs'),
                'tumor_SOMEPID_merged.mdup.bam',
                'tumor_SOMEPID_merged.mdup.bam.bai',
                'control_SOMEPID_merged.mdup.bam',
                'control_SOMEPID_merged.mdup.bam.bai'
        )
    }
}
