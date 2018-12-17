package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

class WgbsRoddyAlignmentCheckerIntegrationSpec extends AbstractRoddyAlignmentCheckerIntegrationSpec {

    @Override
    AbstractRoddyAlignmentChecker createRoddyAlignmentChecker() {
        return new WgbsRoddyAlignmentChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createPanCanPipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddyRnaPipeline()
    }


    void "workflowName, should return WgbsAlignmentWorkflow"() {
        expect:
        'WgbsAlignmentWorkflow' == createRoddyAlignmentChecker().getWorkflowName()
    }

    void "pipeLineName, should return PANCAN_ALIGNMENT"() {
        expect:
        Pipeline.Name.PANCAN_ALIGNMENT == createRoddyAlignmentChecker().getPipeLineName()
    }

    void "seqTypes, should return WGBS and WGBS_TAG"() {
        given:
        List<SeqType> seqTypes = [
                DomainFactory.createWholeGenomeBisulfiteSeqType(),
                DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType(),
        ]

        expect:
        TestCase.assertContainSame(seqTypes, createRoddyAlignmentChecker().getSeqTypes())
    }
}
