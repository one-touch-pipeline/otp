package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Environment
import org.junit.*
import static org.junit.Assert.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

class LoadBedFileScriptTests extends GroovyScriptAwareIntegrationTest{

    Realm realm
    List<File> directories = []
    List<File> bedFiles = []

    private final static String script_path = "scripts/ExomeEnrichmentKit/LoadBedFiles.groovy"
    private final static String reference_genome_path = "/tmp/reference_genomes/"
    private final static String target_regions_dir = "targetRegions"
    // the db must contain all the kits mentioned in the script
    // the db must contain all the reference genomes mentioned in the script
    // all the bed files must be on the file system

    List<Input> bedFilesToLoad = [
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

        realm = new Realm()
        realm.name = "DKFZ"
        realm.env = Environment.getCurrent().getName()
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = ""
        realm.processingRootPath = "tmp"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000000
        realm.pbsOptions = ""
        realm.save(flush : true)

        bedFilesToLoad.each { Input input ->

            File directory = new File("${reference_genome_path}/${input.refGenName}/${target_regions_dir}")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directories << directory
            File bedFile = new File("${directory.getAbsolutePath()}/${input.bedName}")
            bedFile.delete()
            assertTrue bedFile.createNewFile()
            bedFile << "chr1\t1\t2\n"
            bedFile << "chr2\t3\t4"
            bedFiles << bedFile

            ReferenceGenome existingRefGen = ReferenceGenome.findByName(input.refGenName)
            if (!existingRefGen) {
                ReferenceGenome referenceGenome = new ReferenceGenome()
                referenceGenome.name = input.refGenName
                referenceGenome.path = "${input.refGenName}"
                referenceGenome.fileNamePrefix = "prefixName"
                referenceGenome.length = 5
                referenceGenome.save(flush: true)

                ReferenceGenomeEntry entry = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "chr1Alias",
                        length: 5 as long,
                        referenceGenome: referenceGenome
                        )
                entry.save(flush: true)

                entry = new ReferenceGenomeEntry(
                        name: "chr2",
                        alias: "chr2Alias",
                        length: 5 as long,
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
        run(script_path)
        BedFile bedFile = BedFile.findByFileName(bedFilesToLoad.first().bedName)
        assertNotNull bedFile
        assertEquals (2, bedFile.targetSize)
    }

    @Test(expected = IllegalArgumentException)
    void testNoRealmExists() {
        realm.name = "DOES_NOT_EXISTS"
        assertNotNull realm.save(flush: true)
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void testNoRefGenExists() {
        renameOneRefGen()
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void testNoKitExists() {
        ExomeEnrichmentKit kit = ExomeEnrichmentKit.list().first()
        kit.name = "DOES_NOT_EXISTS"
        kit.save(flush: true)
        run(script_path)
    }

    @Test(expected = RuntimeException)
    void testCanNotReadBedFile() {
        assertTrue bedFiles.first().delete()
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void testStartNotLessThenEndInBedFile() {
        assertTrue bedFiles.first().delete()
        assertTrue bedFiles.first().createNewFile()
        bedFiles.first() << "chr1\t1\t2\n"
        bedFiles.first() << "chr2\t4\t4"
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void testEntryDoesNotExists() {
        bedFiles.first() << "chr1\t1\t2\n"
        bedFiles.first() << "chr2\t3\t4"
        List<ReferenceGenomeEntry> entries = ReferenceGenomeEntry.findAllByName("chr1")
        entries.each { ReferenceGenomeEntry entry ->
            entry.name = "DOES_NOT EXISTS"
            assertNotNull entry.save(flush: true)
        }
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void sumLessThenZero() {
        assertTrue bedFiles.first().delete()
        bedFiles.first().createNewFile()
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void testSumLessThenRefGenLength() {
        assertTrue bedFiles.first().delete()
        assertTrue bedFiles.first().createNewFile()
        bedFiles.first() << "chr1\t1\t4\n"
        bedFiles.first() << "chr2\t1\t4"
        run(script_path)
    }

    @Test
    void testBedExists() {
        run(script_path)
        renameOneRefGen()
        // there must be no exception because the same bedFile
        // is not going to be loaded 2 times into the db
        run(script_path)
    }

    @Test(expected = IllegalArgumentException)
    void testEndMoreThenLengh() {
        assertTrue bedFiles.first().delete()
        assertTrue bedFiles.first().createNewFile()
        bedFiles.first() << "chr1\t1\t2\n"
        bedFiles.first() << "chr2\t1\t55"
        run(script_path)
    }

    private void renameOneRefGen() {
        ReferenceGenome refGen = ReferenceGenome.list().first()
        refGen.name = "DOES_NOT_EXISTS"
        refGen.save(flush: true)
    }
}
