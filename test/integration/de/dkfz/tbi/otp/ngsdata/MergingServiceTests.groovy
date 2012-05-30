package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class MergingServiceTests extends AbstractIntegrationTest {

    def mergingService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Ignore
    @Test
    void testPrintAllMergedBamForIndividual() {
        // Individual may not be null
        shouldFail(IllegalArgumentException) {
            mergingService.printAllMergedBamForIndividual(null, null)
        }
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        List<SeqType> types = []
        SeqType seqType1 = new SeqType(name: "testSeqType1", libraryLayout: "testLibraryLayout1", dirName: "testDirName1")
        assert(seqType1.save())
        types.add(seqType1)
        SeqType seqType2 = new SeqType(name: "testSeqType2", libraryLayout: "testLibraryLayout2", dirName: "testDirName2")
        assert(seqType2.save())
        types.add(seqType2)
        SeqType seqType3 = new SeqType(name: "testSeqType3", libraryLayout: "testLibraryLayout3", dirName: "testDirName3")
        assert(seqType3.save())
        types.add(seqType3)
        SeqType seqType4 = new SeqType(name: "testSeqType4", libraryLayout: "testLibraryLayout4", dirName: "testDirName4")
        assert(seqType4.save())
        types.add(seqType4)
        // Individual may not be null
        shouldFail(IllegalArgumentException) {
            mergingService.printAllMergedBamForIndividual(null, types)
        }
        mergingService.printAllMergedBamForIndividual(individual, types)
        final String basePath = "/tmp/lsdf/project/"
        String seq = "testDir/sequencing/"
        new File(basePath).mkdirs()
        String viewByPid = "/view-by-pid/testPid/"
        String tumor = "tumor/"
        String control = "control/"
        String unknown = "unknown/"
        String mergedAlignment = "/merged-alignment/"
        List<File> testFiles = []
        new File("/tmp/otp/").mkdirs()
        File tmp = new File("/tmp/otp/")
        println("is dir?: ${tmp.isDirectory()}")
        File file = new File(tmp, "test")
        println(tmp.list())
            println("${file.absoluteFile}, ${file.isFile()}")
        for(int i = 1; i <= 4; i++) {
            String bla = basePath + seq + "testDirName${i}" + viewByPid + tumor + "testlibrarylayout${i}" + mergedAlignment
            new File(bla).mkdirs()
            File dir = new File(bla)
            testFiles.add(file)
            bla = basePath + seq + "testDirName${i}" + viewByPid + control + "testlibrarylayout${i}" + mergedAlignment
            new File(bla).mkdirs()
            testFiles.add(new File(bla, "testFile${i}.bam"))
            bla = basePath + seq + "testDirName${i}" + viewByPid + unknown + "testlibrarylayout${i}" + mergedAlignment
            new File(bla).mkdirs()
            testFiles.add(new File(bla, "testFile${i}.bam"))
        }
        List<String> mergedBams = mergingService.printAllMergedBamForIndividual(individual, types)
        // Individual has no Sample(s) associated
        assertTrue(mergedBams.empty)
        Sample sample1 = new Sample(type: Sample.Type.TUMOR, individual: individual)
        assert(sample1.save())
        Sample sample2 = new Sample(type: Sample.Type.CONTROL, individual: individual)
        assert(sample2.save())
        Sample sample3 = new Sample(type: Sample.Type.UNKNOWN, subType: "tumor", individual: individual)
        assert(sample3.save())
        List<String> mergedBams2 = mergingService.printAllMergedBamForIndividual(individual, types)
        // mergedDir is no directory
        assertTrue(mergedBams2.empty)
        mergedBams2.each { String mergedBam ->
            println("mergedBam: ${mergedBam}")
        }

    }
}
