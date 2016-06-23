package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

class WgbsAlignmentStartJobTests {

    @Autowired
    WgbsAlignmentStartJob wgbsAlignmentStartJob


    @Test
    void "test findUsableBaseBamFile when bamFileInProjectFolder is usable should return null"() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                seqType        : DomainFactory.createWholeGenomeBisulfiteSeqType(),
                needsProcessing: true,
                pipeline       : DomainFactory.createPanCanPipeline(),
        ])
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mergingWorkPackage,
        ])
        mergingWorkPackage.bamFileInProjectFolder = bamFile
        assert mergingWorkPackage.save(flush: true)

        assert null == wgbsAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void "test that getSeqType contains expected SeqTypes"() {
        List<SeqType> expected = [
                DomainFactory.createWholeGenomeBisulfiteSeqType(),
                DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType(),
        ]

        TestCase.assertContainSame(expected, wgbsAlignmentStartJob.seqTypes)
    }
}
