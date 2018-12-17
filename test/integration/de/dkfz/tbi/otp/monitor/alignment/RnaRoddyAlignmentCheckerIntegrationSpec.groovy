package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

class RnaRoddyAlignmentCheckerIntegrationSpec extends AbstractRoddyAlignmentCheckerIntegrationSpec implements RoddyRnaFactory {

    @Override
    AbstractRoddyAlignmentChecker createRoddyAlignmentChecker() {
        return new RnaRoddyAlignmentChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return RoddyRnaFactory.super.findOrCreatePipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createPanCanPipeline()
    }

    @Override
    RoddyBamFile createRoddyBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        createBamFile([
                workPackage: mergingWorkPackage,
        ] + properties)
    }


    void "workflowName, should return RnaAlignmentWorkflow"() {
        expect:
        'RnaAlignmentWorkflow' == createRoddyAlignmentChecker().getWorkflowName()
    }

    void "pipeLineName, should return RODDY_RNA_ALIGNMENT"() {
        expect:
        Pipeline.Name.RODDY_RNA_ALIGNMENT == createRoddyAlignmentChecker().getPipeLineName()
    }

    void "seqTypes, should return RNA"() {
        given:
        List<SeqType> seqTypes = DomainFactory.createRnaAlignableSeqTypes()

        expect:
        TestCase.assertContainSame(seqTypes, createRoddyAlignmentChecker().getSeqTypes())
    }
}
