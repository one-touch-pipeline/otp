package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

import org.junit.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase

/**
 * Script to test basic functionality to load bed files and it's
 * meta information to the OTP database.
 *
 */
class LoadBedFileScriptTests extends GroovyScriptAwareTestCase {

    Realm realm
    List<File> directories = []
    List<File> bedFiles = []

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    private final static String SCRIPT_PATH = "scripts/LibraryPreparationKit/LoadBedFiles.groovy"
    private final static String REFERENCE_GENOME_PATH = "reference_genomes/"
    private final static String TARGET_REGIONS_PATH = "targetRegions"
    private final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100
    /*
     * the db must contain all the kits mentioned in the script
     * the db must contain all the reference genomes mentioned in the script
     * all the bed files must be on the file system
     */
    List<Input> bedFilesToLoad = [
        new Input(bedName: "Agilent3withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent3withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent3withoutUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent4withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withoutUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent4withUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent4withUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent5withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withoutUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "Agilent5withUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "Agilent5withUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "nexterarapidcapture_exome_targetedregions.bed", refGenName: "hs37d5", kitName: "Illumina Nextera Rapid"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_chr.bed", refGenName: "hg19", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_plain.bed", refGenName: "hs37d5", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_chr.bed", refGenName: "hg19", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_plain.bed", refGenName: "hs37d5", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "TruSeqExomeTargetedRegions_chr.bed", refGenName: "hg19", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "TruSeqExomeTargetedRegions_plain.bed", refGenName: "hs37d5", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "TruSeqExomeTargetedRegions_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "Agilent_S0447132_Covered.bed", refGenName: "hs37d5", kitName: "Agilent S0447132"),
        new Input(bedName: "Agilent_S0447132_Covered_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent S0447132"),

        new Input(bedName: "SureSelect50MbV4_chr.bed", refGenName: "hg19", kitName: "SureSelect50MbV4"),
        new Input(bedName: "SureSelect50MbV4_plain.bed", refGenName: "hs37d5", kitName: "SureSelect50MbV4"),
        new Input(bedName: "SureSelect50MbV4_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "SureSelect50MbV4"),

        new Input(bedName: "SureSelect50MbV5_chr.bed", refGenName: "hg19", kitName: "SureSelect50MbV5"),
        new Input(bedName: "SureSelect50MbV5_plain.bed", refGenName: "hs37d5", kitName: "SureSelect50MbV5"),
        new Input(bedName: "SureSelect50MbV5_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "SureSelect50MbV5"),
    ]
    class Input {
        String bedName
        String refGenName
        String kitName
    }

    @Before
    void setUp() {
        temporaryFolder.create()
        File baseFolder = temporaryFolder.newFolder()

        realm = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: baseFolder.path,
        ]).save(flush : true)

        bedFilesToLoad.each { Input input ->
            File directory = new File(baseFolder, "${REFERENCE_GENOME_PATH}/${input.refGenName}/${TARGET_REGIONS_PATH}")
            directory.mkdirs()
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
                referenceGenome.length = ARBITRARY_REFERENCE_GENOME_LENGTH
                referenceGenome.lengthWithoutN = ARBITRARY_REFERENCE_GENOME_LENGTH
                referenceGenome.lengthRefChromosomes = ARBITRARY_REFERENCE_GENOME_LENGTH
                referenceGenome.lengthRefChromosomesWithoutN = ARBITRARY_REFERENCE_GENOME_LENGTH
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

            LibraryPreparationKit existingKit = LibraryPreparationKit.findByName(input.kitName)
            if (!existingKit) {
                LibraryPreparationKit kit = new LibraryPreparationKit(
                                name: input.kitName
                                )
                kit.save(flush: true)
            }
        }

    }

    @After
    void tearDown() {
        realm = null
        directories = null
        bedFiles = null
    }

    @Test
    void testCorrect() {
        runScript(SCRIPT_PATH)
        BedFile bedFile = BedFile.findByFileName(bedFilesToLoad.first().bedName)
        assertNotNull bedFile
        assertEquals (10 + 10 + 10, bedFile.targetSize)
        assertEquals (10 + 15, bedFile.mergedTargetSize)
    }

    @Test(expected = IllegalArgumentException)
    void testNoRealmExists() {
        realm.name = "DOES_NOT_EXIST"
        assertNotNull realm.save(flush: true)
        runScript(SCRIPT_PATH)
    }

    @Test(expected = IllegalArgumentException)
    void testNoRefGenExists() {
        renameOneRefGen()
        runScript(SCRIPT_PATH)
    }

    @Test(expected = IllegalArgumentException)
    void testNoKitExists() {
        LibraryPreparationKit kit = LibraryPreparationKit.list().first()
        kit.name = "DOES_NOT_EXIST"
        kit.save(flush: true)
        runScript(SCRIPT_PATH)
    }

    @Test(expected = RuntimeException)
    void testCanNotReadBedFile() {
        assertTrue bedFiles.first().delete()
        runScript(SCRIPT_PATH)
    }

    @Test
    void testStartNotLessThenEndInBedFile() {
        assertTrue bedFiles.first().delete()
        assertTrue bedFiles.first().createNewFile()
        bedFiles.first() << "chr1\t10\t20\n"
        bedFiles.first() << "chr2\t40\t30"
        runScript(SCRIPT_PATH)
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
        runScript(SCRIPT_PATH)
    }

    @Test(expected = IllegalArgumentException)
    void sumLessThenZero() {
        assertTrue bedFiles.first().delete()
        bedFiles.first().createNewFile()
        runScript(SCRIPT_PATH)
    }

    @Test
    void testBedExists() {
        runScript(SCRIPT_PATH)
        renameOneRefGen()
        // there must be no exception because the same bedFile
        // is not going to be loaded 2 times into the db
        runScript(SCRIPT_PATH)
    }

    private void renameOneRefGen() {
        ReferenceGenome refGen = ReferenceGenome.list().first()
        refGen.name = "DOES_NOT_EXIST"
        refGen.save(flush: true)
    }
}
