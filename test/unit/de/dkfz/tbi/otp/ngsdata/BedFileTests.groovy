package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import org.junit.*

@TestFor(BedFile)
@Mock([ReferenceGenome, LibraryPreparationKit])
class BedFileTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    ReferenceGenome refGen
    LibraryPreparationKit kit1
    LibraryPreparationKit kit2
    BedFile bedFile1
    BedFile bedFile2

    @Before
    void setUp() {
        refGen = new ReferenceGenome(
                name: "refGen",
                path: "filePath",
                fileNamePrefix: "prefix",
                length: ARBITRARY_REFERENCE_GENOME_LENGTH,
                lengthWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                lengthRefChromosomes: ARBITRARY_REFERENCE_GENOME_LENGTH,
                lengthRefChromosomesWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                )
        assertNotNull refGen.save(flush: true)
        kit1 = new LibraryPreparationKit(
                name: "kitName1"
                )
        assertNotNull kit1.save(flush: true)
        kit2 = new LibraryPreparationKit(
                name: "kitName2"
                )
        assertNotNull kit2.save(flush: true)
        bedFile1 = new BedFile(
                fileName: "fileName1",
                targetSize: 1,
                referenceGenome: refGen,
                libraryPreparationKit: kit1
                )
        bedFile2 = new BedFile(
                fileName: "fileName2",
                targetSize: 111,
                referenceGenome: refGen,
                libraryPreparationKit: kit2
                )
    }

    @After
    void tearDown() {
        refGen = null
        kit1 = null
        kit2 = null
        bedFile1 = null
        bedFile2 = null
    }

    @Test
    void testCreateCorrect() {
        assertTrue bedFile1.validate()
        assertNotNull bedFile1.save(flush: true)
        assertTrue bedFile2.validate()
        assertNotNull bedFile2.save(flush: true)
        assertTrue !bedFile1.toString().empty
        assertTrue !bedFile2.toString().empty
    }

    @Test
    void testFileNameNull() {
        bedFile1.fileName = null
        assertFalse bedFile1.validate()
    }

    @Test
    void testFileNameEmpty() {
        bedFile1.fileName = ""
        assertFalse bedFile1.validate()
    }

    @Test
    void testFileNameNotUnique() {
        assertNotNull bedFile1.save(flush: true)
        bedFile2.fileName = "fileName1"
        assertFalse bedFile2.validate()
    }

    @Test
    void testGenomeKitPrimaryKey() {
        assertNotNull bedFile1.save(flush: true)
        bedFile2.libraryPreparationKit = kit1
        assertFalse bedFile2.validate()
    }

    @Test
    void testMinTargetSize() {
        bedFile1.targetSize = 0
        assertFalse bedFile1.validate()
        bedFile2.targetSize = -1
        assertFalse bedFile2.validate()
    }
}
