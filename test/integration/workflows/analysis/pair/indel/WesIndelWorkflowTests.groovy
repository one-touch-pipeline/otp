package workflows.analysis.pair.indel

import org.junit.Ignore
import workflows.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div64

import de.dkfz.tbi.otp.ngsdata.*

@Ignore
class WesIndelWorkflowTests extends AbstractIndelWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div64 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.exomePairedSeqType
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
