package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

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
