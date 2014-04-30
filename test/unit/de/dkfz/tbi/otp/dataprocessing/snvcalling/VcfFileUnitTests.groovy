package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(VcfFile)
@Mock([SnvCallingInstance])
class VcfFileUnitTests {

    @Test
    void testSavingOfVcfFile() {
        VcfFile vcfFile = new VcfFile(
                        fileName: "someRandomProcessedVcfFile.vcf",
                        step: SnvCallingStep.SNV_CALL,
                        snvCallingRun: new SnvCallingInstance(),
                        processingState: SnvProcessingStates.IN_PROGRESS,
                        )
        assert vcfFile.validate()
        assert vcfFile.save()
    }
}
