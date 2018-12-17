package workflows.analysis.pair.indel

import org.junit.Ignore
import workflows.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div128

import de.dkfz.tbi.otp.ngsdata.*

@Ignore
class WgsIndelWorkflowTests extends AbstractIndelWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div128 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
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
