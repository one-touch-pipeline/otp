package workflows.analysis.pair.bamfiles

import de.dkfz.tbi.otp.ngsdata.*

trait SeqTypeAndInputWgsBigBamFiles implements SeqTypeAndInputBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }

    @Override
    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'wgsbig'),
                'tumor_merged.bam.rmdup.bam',
                'tumor_merged.bam.rmdup.bam.bai',
                'control_merged.bam.rmdup.bam',
                'control_merged.bam.rmdup.bam.bai'
        )
    }
}
