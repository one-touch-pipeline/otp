package de.dkfz.tbi.otp.dataprocessing

import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class MergingSetTests {

    @Test
    void testGetBamFiles_oneProcessedBamFiles() {
        MergingSet mergingSet = DomainFactory.createMergingSet()
        ProcessedBamFile processedBamFile = DomainFactory.assignNewProcessedBamFile(mergingSet)
        assert [processedBamFile] == mergingSet.getBamFiles()
    }

    @Test
    void testGetBamFiles_noProcessedBamFiles() {
        MergingSet mergingSet = DomainFactory.createMergingSet()
        assert mergingSet.getBamFiles().empty
    }
}
