package de.dkfz.tbi.ngstools.bedUtils

class TargetIntervalsFactoryTest extends GroovyTestCase {

    File file

    void testCreate() {
        file = new File("/tmp/kitname.bed")
        if (file.exists()) file.delete()
        file << "chr1\t0\t100\nchr2\t32\t105\nchr3\t10000000\t249250621"
        file.deleteOnExit()
        List<String> referenceGenomeEntryNames = ["chr1", "chr2", "chr3", "chr4", "chr5"]
        TargetIntervals targetIntervals = TargetIntervalsFactory.create(file.absolutePath, referenceGenomeEntryNames)
        assertNotNull targetIntervals
    }

    void tearDown() {
        file.delete()
    }
}
