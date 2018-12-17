package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

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
