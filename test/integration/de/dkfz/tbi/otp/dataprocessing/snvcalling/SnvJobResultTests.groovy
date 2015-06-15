package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.junit.Test

class SnvJobResultTests {

    @Test
    void testMakeWithdrawn_useFourResults_ShouldSetTheLastThreeToWithdrawn() {
        SnvJobResult snvJobResultCalling = DomainFactory.createSnvJobResultWithRoddyBamFiles(step: SnvCallingStep.CALLING)
        SnvJobResult snvJobResultAnnotation = DomainFactory.createSnvJobResultWithRoddyBamFiles(inputResult: snvJobResultCalling, step: SnvCallingStep.SNV_ANNOTATION)
        SnvJobResult snvJobResultDeepAnnotation = DomainFactory.createSnvJobResultWithRoddyBamFiles(inputResult: snvJobResultAnnotation, step: SnvCallingStep.SNV_DEEPANNOTATION)
        SnvJobResult snvJobResultFilter = DomainFactory.createSnvJobResultWithRoddyBamFiles(inputResult: snvJobResultDeepAnnotation, step: SnvCallingStep.FILTER_VCF)

        assert !snvJobResultCalling.withdrawn
        assert !snvJobResultAnnotation.withdrawn
        assert !snvJobResultDeepAnnotation.withdrawn
        assert !snvJobResultFilter.withdrawn

        LogThreadLocal.withThreadLog(System.out) {
            snvJobResultAnnotation.makeWithdrawn()
        }
        assert !snvJobResultCalling.withdrawn
        assert snvJobResultAnnotation.withdrawn
        assert snvJobResultDeepAnnotation.withdrawn
        assert snvJobResultFilter.withdrawn
    }

}
