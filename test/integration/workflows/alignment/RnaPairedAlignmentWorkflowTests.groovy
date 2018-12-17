package workflows.alignment

import org.junit.Ignore

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackageAlignmentProperty
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

@Ignore
class RnaPairedAlignmentWorkflowTests extends AbstractRnaAlignmentWorkflowTests {

    @Override
    SeqType findSeqType() {
        DomainFactory.createRnaPairedSeqType()
    }

    void createProjectConfigRna(MergingWorkPackage workPackage, Map configOptions = [:], Map referenceGenomeConfig = [:]) {
        super.createProjectConfigRna(workPackage, configOptions, referenceGenomeConfig)

        //multithreading of FEATURE_COUNT_CORES does not work for the test data (anymore), therefore it is set to one thread
        //since in production it does not make problems, it is only changed for the test
        MergingWorkPackageAlignmentProperty mergingWorkPackageAlignmentProperty = new MergingWorkPackageAlignmentProperty(name: 'FEATURE_COUNT_CORES', value: '1', mergingWorkPackage: workPackage)
        workPackage.alignmentProperties.add(mergingWorkPackageAlignmentProperty)
        workPackage.save(flush: true)
    }
}
