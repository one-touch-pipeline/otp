package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.util.Environment
import org.junit.*
import static org.junit.Assert.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

/**
 * Script to test basic functionality to load bed files and it's
 * meta information to the OTP database.
 *
 */
class LoadBedFileScriptTests extends GroovyScriptAwareIntegrationTest{

    Realm realm
    List<File> directories = []
    List<File> bedFiles = []

    private final static String SCRIPT_PATH = "scripts/ExomeEnrichmentKit/LoadBedFiles.groovy"
    private final static String REFERENCE_GENOME_PATH = "/tmp/reference_genomes/"
    private final static String TARGET_REGIONS_PATH = "targetRegions"
    /*
     * the db must contain all the kits mentioned in the script
     * the db must contain all the reference genomes mentioned in the script
     * all the bed files must be on the file system
     */
    List<Input> bedFilesToLoad = [
        new Input(bedName: "Agilent3withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent3withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent4withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent5withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "Agilent5withUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V5+UTRs")
    ]
    class Input {
        String bedName
        String refGenName
        String kitName
    }

    @Before
    void setUp() {
        realm = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: 'tmp',
            ]).save(flush : true)

        bedFilesToLoad.each { Input input ->
            File directory = new File("${REFERENCE_GENOME_PATH}/${input.refGenName}/${TARGET_REGIONS_PATH}")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directories << directory
            File bedFile = new File("${directory.getAbsolutePath()}/${input.bedName}")
            bedFile.delete()
            assertTrue bedFile.createNewFile()
            bedFile << "chr1\t10\t20\n"
            bedFile << "chr2\t30\t40\n"
            bedFile << "chr2\t35\t45"
            bedFiles << bedFile

            ReferenceGenome existingRefGen = ReferenceGenome.findByName(input.refGenName)
            if (!existingRefGen) {
                ReferenceGenome referenceGenome = new ReferenceGenome()
                referenceGenome.name = input.refGenName
                referenceGenome.path = "${input.refGenName}"
                referenceGenome.fileNamePrefix = "prefixName"
                referenceGenome.length = 100
                referenceGenome.save(flush: true)

                ReferenceGenomeEntry entry = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "chr1Alias",
                        length: 50 as long,
                        referenceGenome: referenceGenome
                        )
                entry.save(flush: true)

                entry = new ReferenceGenomeEntry(
                        name: "chr2",
                        alias: "chr2Alias",
                        length: 50 as long,
                        referenceGenome: referenceGenome
                        )
                entry.save(flush: true)
            }

            ExomeEnrichmentKit existingKit = ExomeEnrichmentKit.findByName(input.kitName)
            if (!existingKit) {
                ExomeEnrichmentKit kit = new ExomeEnrichmentKit(
                        name: input.kitName
                        )
                kit.save(flush: true)
            }
        }

    }

    @After
    void tearDown() {
        realm = null
        directories.each {File dir -> dir.deleteOnExit()}
        bedFiles.each {File file -> file.deleteOnExit()}
    }

    @Test
    void testCorrect() {
        run(SCRIPT_PATH)
        BedFile bedFile = BedFile.findByFileName(bedFilesToLoad.first().bedName)
        assertNotNull bedFile
        assertEquals (10 + 10 + 10, bedFile.targetSize)
        assertEquals (10 + 15, bedFile.mergedTargetSize)
    }

    @Test(expected = IllegalArgumentException)
    void testNoRealmExists() {
        realm.name = "DOES_NOT_EXIST"
        assertNotNull realm.save(flush: true)
        run(SCRIPT_PATH)
    }

    @Test(expected = IllegalArgumentException)
    void testNoRefGenExists() {
        renameOneRefGen()
        run(SCRIPT_PATH)
    }

    @Test(expected = IllegalArgumentException)
    void testNoKitExists() {
        ExomeEnrichmentKit kit = ExomeEnrichmentKit.list().first()
        kit.name = "DOES_NOT_EXIST"
        kit.save(flush: true)
        run(SCRIPT_PATH)
    }

    @Test(expected = RuntimeException)
    void testCanNotReadBedFile() {
        assertTrue bedFiles.first().delete()
        run(SCRIPT_PATH)
    }

    @Test
    void testStartNotLessThenEndInBedFile() {
        assertTrue bedFiles.first().delete()
        assertTrue bedFiles.first().createNewFile()
        bedFiles.first() << "chr1\t10\t20\n"
        bedFiles.first() << "chr2\t40\t30"
        run(SCRIPT_PATH)
        BedFile bedFile = BedFile.findByFileName(bedFilesToLoad.first().bedName)
        assertNotNull bedFile
        assertEquals (10 + 10, bedFile.targetSize)
        assertEquals (10 + 10, bedFile.mergedTargetSize)
    }

    @Test(expected = IllegalArgumentException)
    void testEntryDoesNotExists() {
        bedFiles.first() << "chr1\t10\t20\n"
        bedFiles.first() << "chr2\t30\t40"
        List<ReferenceGenomeEntry> entries = ReferenceGenomeEntry.findAllByName("chr1")
        entries.each { ReferenceGenomeEntry entry ->
            entry.name = "DOES_NOT_EXIST"
            assertNotNull entry.save(flush: true)
        }
        run(SCRIPT_PATH)
    }

    @Test(expected = IllegalArgumentException)
    void sumLessThenZero() {
        assertTrue bedFiles.first().delete()
        bedFiles.first().createNewFile()
        run(SCRIPT_PATH)
    }

    @Test
    void testBedExists() {
        run(SCRIPT_PATH)
        renameOneRefGen()
        // there must be no exception because the same bedFile
        // is not going to be loaded 2 times into the db
        run(SCRIPT_PATH)
    }

    private void renameOneRefGen() {
        ReferenceGenome refGen = ReferenceGenome.list().first()
        refGen.name = "DOES_NOT_EXIST"
        refGen.save(flush: true)
    }
}
