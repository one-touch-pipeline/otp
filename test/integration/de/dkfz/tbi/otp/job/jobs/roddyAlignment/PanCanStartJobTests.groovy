package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

class PanCanStartJobTests {

    @Autowired
    PanCanStartJob panCanStartJob

    @Test
    void "test that getSeqType contains expected SeqTypes"() {
        List<SeqType> expected = [
                DomainFactory.createWholeGenomeSeqType(),
                DomainFactory.createExomeSeqType(),
                DomainFactory.createChipSeqType(),
        ]

        TestCase.assertContainSame(expected, panCanStartJob.seqTypes)
    }
}
