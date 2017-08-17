package workflows.analysis.pair.indel

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WgsIndelWorkflowTests extends AbstractIndelWorkflowTests implements SeqTypeAndInputWgsBamFiles {
    @Override
    void adaptSampleTypes() {
        SampleType tumor = DomainFactory.createSampleType([name: "TUMOR"])
        SampleType control = DomainFactory.createSampleType([name: "CONTROL03"])
        bamFileTumor.sample.sampleType = tumor
        assert bamFileTumor.save(flush: true)
        bamFileControl.sample.sampleType = control
        assert bamFileControl.save(flush: true)
    }

    @Override
    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'wgs-indel'),
                'tumor_SOMEPID_merged.mdup.chr1.bam',
                'tumor_SOMEPID_merged.mdup.chr1.bam.bai',
                'control03_SOMEPID_merged.mdup.chr1.bam',
                'control03_SOMEPID_merged.mdup.chr1.bam.bai'
        )
    }
}
