package workflows.analysis.pair.indel

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WesIndelWorkflowTests extends AbstractIndelWorkflowTests implements SeqTypeAndInputBigBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqType.exomePairedSeqType
    }

    @Override
    void adaptSampleTypes() {
        SampleType tumor = DomainFactory.createSampleType([name: "PLASMA"])
        SampleType control = DomainFactory.createSampleType([name: "BLOOD"])
        bamFileTumor.sample.sampleType = tumor
        assert bamFileTumor.save(flush: true)
        bamFileControl.sample.sampleType = control
        assert bamFileControl.save(flush: true)
    }
}
