package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.MergingSet
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory

import org.junit.*

class MergingSetTests {

    @Test
    void testGetBamFiles_oneProcessedBamFiles() {
        MergingSet mergingSet = MergingSet.build()
        ProcessedBamFile processedBamFile = DomainFactory.assignNewProcessedBamFile(mergingSet)
        assert [processedBamFile] == mergingSet.getBamFiles()
    }

    @Test
    void testGetBamFiles_noProcessedBamFiles() {
        MergingSet mergingSet = MergingSet.build()
        assert mergingSet.getBamFiles().empty
    }
}
