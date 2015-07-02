package workflows

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.Chromosomes
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

abstract class QualityAssessmentAbstractWorkflowTests extends WorkflowTestCase {

    /*
     * preparation:
     *  - see src/docs/guide/devel/testing/workflowTesting.gdoc
     *  - make sure test-qa.sh, which calls the required version of qa.jar, is executable:
     *    chmod g+x test-qa.sh
     */

    /*
     * NOTE:
     * with the current test set up the executed workflow calls production version of all
     * scripts - it calls scripts installed on the cluster.
     * Exception to it is qa.jar which is called from test-qa.sh.
     * Changing of test-qa.sh makes it possible to test production or a test version of qa.jar
     */


    TestData testData = new TestData()

    // files to be processed by the tests
    // files are the same for both merged and not-merged workflows tests
    // (for qa.jar it is not important if the given bam is merged or not,
    // the OTP meta data is important)
    String orgFileName = "seq.bam.bam"
    long totalSequences = 600000l // number of reads in $orgFileName which is taken from qa results for the this bam file
    String baiOrgFileName = "${orgFileName}.bai"
    String bedFileName = "Agilent5withUTRs_chr.bed"

    // take the needed versions of qa.jar
    // by default it is assumed that build directory contains version to be used for tests
    String qaJarShellScript

    @Before
    void setUp() {
        qaJarShellScript = "${getWorkflowDirectory().absolutePath}/test-qa.sh"

        createData()
    }


    @Ignore
    @Test
    void testExecutionWithProcessingOptionsWgs() {
        createAdditionalWholeGenomeData()
        createDirectoryStructure(inputFilesPath())
        updateOptions()
        execute()
    }

    @Test
    @Ignore
    void testExecutionWithProcessingOptionsExome() {
        createAdditionalExomData()
        createDirectoryStructure(inputFilesPath())
        createDirectoryForReferenceGenome()
        updateOptions()
        execute()
    }

    // in the case of not-merged qa worfkflow test only one seqTrack is required, but
    // in the case of merged qa-workflow test it is better to have 2 seqTracks
    // therefore this method expects list of seqTracks as parameters
    abstract protected void createAdditionalTestData(List<SeqTrack> seqTracks)

    /**
     * @return a map containing the following key and values
     * path: path to the directory where the links to the qa.jar input files must be created
     * filePath: path to the link for bam file
     * baiFilePath: path to the link for bai file
     */
    abstract protected Map inputFilesPath()

    // options must be created by the script used to created options in production
    // options created by the script must be changed to be used for testing
    private void updateOptions() {
        ProcessingOption.list().each { ProcessingOption option ->
            option.value = option.value.replaceAll('qualityAssessment.sh', "${qaJarShellScript}")
            option.save(flush: true)
        }
    }

    private void createDirectoryStructure(Map input) {
        String softLinkFilepath = "${testDataDir}/merged-quality-assessment/${orgFileName}"
        String softLinkBaiFilepath = "${testDataDir}/merged-quality-assessment/${baiOrgFileName}"
        // Just to be sure the rootPath and the processingRootPath are clean for new test
        createDirectories([new File(input.path)])
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${softLinkFilepath} ${input.filePath}"
        String cmdBuildSoftLinkToBaiFileToBeProcessed = "ln -s ${softLinkBaiFilepath} ${input.baiFilePath}"
        executionService.executeCommand(realm, "${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdBuildSoftLinkToBaiFileToBeProcessed}")
        File file = new File(input.filePath)
        assertTrue(file.canRead())
        file = new File(input.baiFilePath)
        assertTrue(file.canRead())
    }

    /*
     * Function to map the classification strings to enums to match
     * the ReferenceGenomeEntry object.
     * Default classification is UNDEFINED
     */
    Classification mapClassification(String classificationInput) {
        Classification classification = Classification.UNDEFINED
        if ( classificationInput.equals("CHROMOSOME")) {
            classification = Classification.CHROMOSOME
        }
        if ( classificationInput.equals("MITOCHONDRIAL")) {
            classification = Classification.MITOCHONDRIAL
        }
        if ( classificationInput.equals("CONTIG")) {
            classification = Classification.CONTIG
        }
        return classification
    }

    protected ReferenceGenome createReferenceGenome(Project project, SeqType seqType) {
        // get list of all standard chromosomes which is: 1..22, X, Y
        List<String> standardChromosomes = Chromosomes.allLabels()
        standardChromosomes.remove("M")
        assert standardChromosomes.size() == 24

        ReferenceGenome refGen = testData.createReferenceGenome(
            name: "hg19",
            path: "bwa06_hg19_chr",
            fileNamePrefix: "hg19_1-22_X_Y_M",
        )
        refGen.save(failOnError: true)

        // list which holds information about all entries in the ref. genome fasta file
        Map<String, Integer> fastaEntriesColumn = ["name":0, "alias":1, "length":2, "lengthWithoutN":3, "classification":4]
        List<List<String, Long, Long>> fastaEntries = [
            ["chr1", "1", 249250621, 225280621, "CHROMOSOME"],
            ["chr10", "10", 135534747, 131314742, "CHROMOSOME"],
            ["chr11", "11", 135006516, 131129516, "CHROMOSOME"],
            ["chr12", "12", 133851895, 130481394, "CHROMOSOME"],
            ["chr13", "13", 115169878, 95589878, "CHROMOSOME"],
            ["chr14", "14", 107349540, 88289540, "CHROMOSOME"],
            ["chr15", "15", 102531392, 81694769, "CHROMOSOME"],
            ["chr16", "16", 90354753, 78884753, "CHROMOSOME"],
            ["chr17", "17", 81195210, 77795210, "CHROMOSOME"],
            ["chr18", "18", 78077248, 74657233, "CHROMOSOME"],
            ["chr19", "19", 59128983, 55808983, "CHROMOSOME"],
            ["chr2", "2", 243199373, 238204522, "CHROMOSOME"],
            ["chr20", "20", 63025520, 59505520, "CHROMOSOME"],
            ["chr21", "21", 48129895, 35106692, "CHROMOSOME"],
            ["chr22", "22", 51304566, 34894562, "CHROMOSOME"],
            ["chr3", "3", 198022430, 194797136, "CHROMOSOME"],
            ["chr4", "4", 191154276, 187661676, "CHROMOSOME"],
            ["chr5", "5", 180915260, 177695260, "CHROMOSOME"],
            ["chr6", "6", 171115067, 167395067, "CHROMOSOME"],
            ["chr7", "7", 159138663, 155353663, "CHROMOSOME"],
            ["chr8", "8", 146364022, 142888922, "CHROMOSOME"],
            ["chr9", "9", 141213431, 120143431, "CHROMOSOME"],
            ["chrM", "M", 16571, 16571, "MITOCHONDRIAL"],
            ["chrX", "X", 155270560, 151100560, "CHROMOSOME"],
            ["chrY", "Y", 59373566, 25653566, "CHROMOSOME"]
        ]

        // init counter for overall length values
        long length = 0
        long lengthWithoutN = 0
        long lengthRefChromosomes = 0
        long lengthRefChromosomesWithoutN = 0

        // put fastaEntry as ReferenceGenomeEntry information into database
        fastaEntries.each { entry ->
            ReferenceGenomeEntry refGenEntry = new ReferenceGenomeEntry()
            refGenEntry.name = entry[fastaEntriesColumn.name]
            refGenEntry.alias = entry[fastaEntriesColumn.alias]
            refGenEntry.length = entry[fastaEntriesColumn.length]
            refGenEntry.lengthWithoutN = entry[fastaEntriesColumn.lengthWithoutN]
            refGenEntry.classification = mapClassification(entry[fastaEntriesColumn.classification])
            refGenEntry.referenceGenome = refGen
            if (refGenEntry.validate()) {
                refGenEntry.save(flush: true)
            } else {
                refGenEntry.errors.allErrors.each {
                    log.debug it
                }
            }
            // overall counting
            length += entry[fastaEntriesColumn.length]
            lengthWithoutN += entry[fastaEntriesColumn.lengthWithoutN]
            // counting if entry is a standardChromosome
            if ( standardChromosomes.contains(refGenEntry.alias) ) {
                lengthRefChromosomes += refGenEntry.length
                lengthRefChromosomesWithoutN += refGenEntry.lengthWithoutN
            }
        }

        // put length values into database
        refGen.length = length
        refGen.lengthWithoutN = lengthWithoutN
        refGen.lengthRefChromosomes = lengthRefChromosomes
        refGen.lengthRefChromosomesWithoutN = lengthRefChromosomesWithoutN
        refGen.save(flush: true)

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType()

        referenceGenomeProjectSeqType.project = project
        referenceGenomeProjectSeqType.seqType = seqType
        referenceGenomeProjectSeqType.referenceGenome = refGen
        referenceGenomeProjectSeqType.save(flush: true)

        return refGen
    }

    private void createData() {
        Project project = TestData.createProject(
                        name: "project",
                        dirName: "project-dir",
                        realmName: realm.name
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
                        seqPlatformGroup: SeqPlatformGroup.build(),
                        name: "name",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        Run run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))
    }

    protected void setupForLoadingWorkflow() {
        SeqType seqType = new SeqType(
                        name: "WHOLE_GENOME",
                        libraryLayout: "PAIRED",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        seqType = new SeqType(
                        name: "EXON",
                        libraryLayout: "PAIRED",
                        dirName: "exome-seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))
    }

    private void createAdditionalWholeGenomeData() {

        SeqType seqType = SeqType.findByName('WHOLE_GENOME')
        List<SeqTrack> seqTracks = []
        2.times {
            SeqTrack seqTrack = new SeqTrack(seqType: seqType)
            seqTracks << seqTrack
        }
        createAdditionalTestData(seqTracks)
    }

    private void createAdditionalExomData() {

        LibraryPreparationKit kit = new LibraryPreparationKit(name: 'kit')
        assertNotNull kit.save(flush: true)

        List<SeqTrack> seqTracks = []
        2.times {
            SeqTrack seqTrack = new ExomeSeqTrack(
                            seqType: SeqType.findByName('EXON'),
                            kitInfoReliability: InformationReliability.KNOWN,
                            libraryPreparationKit: kit
                            )
            seqTracks << seqTrack
        }

        createAdditionalTestData(seqTracks)

        ReferenceGenome refGen = ReferenceGenome.findByName('hg19')
        BedFile bedFile = new BedFile(
                        fileName: bedFileName,
                        targetSize: 11111111111,
                        mergedTargetSize: 1111111,
                        referenceGenome: refGen,
                        libraryPreparationKit: kit
                        )
        assertNotNull(bedFile.save([flush: true]))
    }

    private void createDirectoryForReferenceGenome() {
        String referenceGenomesDir = "${getRootDirectory()}/files/reference_genomes"
        String softLinkToReferenceGenomesDir = "${processingRootPath}/reference_genomes"
        String cmdBuildSoftLinkToReferenceGenomes = "ln -s ${referenceGenomesDir} ${softLinkToReferenceGenomesDir}"
        executionService.executeCommand(realm, "${cmdBuildSoftLinkToReferenceGenomes}")
        assert WaitingFileUtils.waitUntilExists(new File(softLinkToReferenceGenomesDir))
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(200)
    }
}
