package workflows.analysis.pair.indel

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WgsIndelWorkflowTests extends AbstractIndelWorkflowTests implements SeqTypeAndInputBigBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqType.wholeGenomePairedSeqType
    }

    @Override
    void adaptSampleTypes() {
        SampleType tumor = DomainFactory.createSampleType([name: "TUMOR"])
        SampleType control = DomainFactory.createSampleType([name: "CONTROL03"])
        bamFileTumor.sample.sampleType = tumor
        assert bamFileTumor.save(flush: true)
        bamFileControl.sample.sampleType = control
        assert bamFileControl.save(flush: true)
    }

}
