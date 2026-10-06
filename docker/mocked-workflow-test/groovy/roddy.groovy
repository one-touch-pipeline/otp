/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import groovy.json.JsonOutput

import java.nio.file.*
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater

class Roddy {

    /**
     * The analysis configurations OTP may call, mapped to the plugin version OTP is expected to use.
     *
     * The key is the value of {@code AbstractExecuteRoddyPipelineJob.getAnalysisConfiguration()} and at the same time the name of the method
     * creating the mocked result files.
     * The value is the newest workflow version of the newest workflow api version, since that one is selected by the workflow tests.
     */
    static final Map<String, String> PIPELINES = [
            // alignment
            qcAnalysis                  : "AlignmentAndQCWorkflows:1.2.73-204",
            exomeAnalysis               : "AlignmentAndQCWorkflows:1.2.73-204",
            bisulfiteCoreAnalysis       : "AlignmentAndQCWorkflows:1.2.73-204",
            RNAseqAnalysis              : "RNAseqWorkflow:1.3.0-1",

            // analysis
            snvCallingAnalysis          : "SNVCallingWorkflow:1.2.166-6",
            indelCallingAnalysis        : "IndelCallingWorkflow:1.2.177-603",
            sophiaAnalysis              : "SophiaWorkflow:2.2.3",
            copyNumberEstimationAnalysis: "ACEseqWorkflow:1.2.8-4",
    ]

    static final String MOCKED_FILE_CONTENT = "This is a mocked result file created by the mocked roddy script for testing purposes."

    /**
     * The tumor cell content and the ploidy factor of the mocked aceseq qc file.
     *
     * They are part of the file names of the aceseq 'extra' plots, therefore the qc file and the plot names have to use the same values.
     */
    static final String ACESEQ_TUMOR_CELL_CONTENT = "0.5"
    static final String ACESEQ_PLOIDY_FACTOR = "2.27"

    /**
     * The chromosomes of the reference genomes used by the alignment workflow tests.
     *
     * The quality control files need an entry for each of them, since OTP checks that all chromosomes of the reference genome are covered.
     *
     * @see de.dkfz.tbi.otp.workflowTest.alignment.AbstractAlignmentWorkflowSpec, which creates the corresponding ReferenceGenomeEntry objects
     */
    static final List<String> ALIGNMENT_CHROMOSOMES = ["21", "22"].asImmutable()

    static final String QA_ALL = 'all'
    static final String QA_DIRECTORY = 'qualitycontrol'
    static final String QA_JSON_FILE = 'qualitycontrol.json'
    static final String QA_TARGET_EXTRACT_JSON_FILE = 'qualitycontrol_targetExtract.json'
    static final String MERGED_DIRECTORY = 'merged'
    static final String METHYLATION_DIRECTORY = 'methylation'
    static final String ARRIBA_DIRECTORY = 'fusions_arriba'
    static final String RNA_SEQC_DIRECTORY = 'RNAseQC'

    /**
     * The values of the rna quality control file, which are only allowed for paired end data.
     *
     * @see de.dkfz.tbi.otp.dataprocessing.RnaQualityAssessment#nullIfAndOnlyIfLayoutIsSingle
     */
    static final List<String> RNA_QA_PAIRED_END_VALUES = [
            'end1MismatchRate',
            'end1PercentageSense',
            'end2MismatchRate',
            'end2PercentageSense',
            'properlyPaired',
            'properlyPairedPercentage',
            'singletons',
            'singletonsPercentage',
    ].asImmutable()

    /** the length used for all chromosomes of the mocked bam file header */
    static final int CHROMOSOME_LENGTH = 50000000

    /** how often the mocked file content is repeated in the comment line of the bam header to exceed the minimal bam file size */
    static final int BAM_HEADER_PADDING_COUNT = 20

    /** the block gzip end of file marker, an empty bgzf block */
    static final byte[] BGZF_EOF_BLOCK = [
            0x1f, 0x8b, 0x08, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0x06, 0x00, 0x42, 0x43, 0x02, 0x00,
            0x1b, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    ].collect { it as byte } as byte[]

    static String EXECUTION_DIRECTORY = "exec_260318_003237600_otp_analysis"

    static Path jobStateLogfile

    /** the configuration values of the config.xml, mapped by their name */
    static Map<String, String> configurationValues

    /** the metadata table of the roddy call, only given by the wgbs alignment workflow */
    static Path metadataTable

    static void main(String[] args) {
        println "Command: ${args.join(' ')}"
        if (!args) {
            System.err.println("No args")
            System.exit(1)
        }

        // the wgbs alignment workflow adds the metadata table as additional parameter
        assert args.length in [6, 7]
        assert args[0] == 'rerun'
        assert args[1] == 'config@analysis'
        assert args[2]
        String pid = args[2]
        assert args[3] == '--useconfig=/workflows/roddy/applicationProperties-test.ini'
        assert args[4] == '--usefeaturetoggleconfig=/workflows/roddy/configs/featureToggles.ini'
        String configurationDirectories = parameterValue(args[5], 'configurationDirectories')
        if (args.length == 7) {
            metadataTable = Paths.get(parameterValue(args[6], 'usemetadatatable'))
            assert Files.exists(metadataTable)
        }

        handleRoddy(pid, configurationDirectories)
    }

    /**
     * Extract the value of a roddy command line parameter of the form {@code --name=value}.
     */
    static String parameterValue(String argument, String parameterName) {
        String prefix = "--${parameterName}="
        assert argument.startsWith(prefix)
        String value = argument.substring(prefix.length())
        assert value
        return value
    }

    static void handleRoddy(String pid, String configurationDirectories) {
        Path config = Paths.get(configurationDirectories, "config.xml")
        println "config: ${config}"
        assert Files.exists(config)

        def xml = new XmlParser().parse(config.toFile())
        def analysis = xml.availableAnalyses.analysis[0]
        println "analysis: ${analysis}"
        String configuration = analysis.@configuration
        println "configuration: ${configuration}"
        String usedPlugin = analysis.@useplugin
        println "usedPlugin: ${usedPlugin}"

        assert configuration in PIPELINES.keySet()
        assert usedPlugin == PIPELINES[configuration]: "OTP used '${usedPlugin}', but the mock expects '${PIPELINES[configuration]}'. " +
                "Update PIPELINES in roddy.groovy, rebuild the otp-mocked-cluster image and bump its tag in .gitlab-ci-core.yml."

        configurationValues = xml.configurationvalues.cvalue.collectEntries { [(it.@name): it.@value] }

        Path outputBaseDirectory = Paths.get(configurationValues['outputBaseDirectory'])

        Path executionStore = Files.createDirectories(outputBaseDirectory.resolve("roddyExecutionStore").resolve(EXECUTION_DIRECTORY))
        createRoddyFiles(executionStore)

        System.err.println("Creating the following execution directory to store information about this process:\n${executionStore}\n")

        "${configuration}"(outputBaseDirectory, pid)
    }

    static void createRoddyFiles(Path executionStore) {
        [
                'applicationProperties.ini',
                'executedJobs.txt',
                'realJobCalls.txt',
                'repeatableJobCalls.sh',
                'roddyCall.sh',
                'versionsInfo.txt',
        ].each {
            Files.writeString(executionStore.resolve(it), "This is a dummy file created by the mocked roddy script for testing purposes.")
        }

        jobStateLogfile = executionStore.resolve("jobStateLogfile.txt")
        Files.writeString(jobStateLogfile, "")
    }

    /**
     * Create the given files with dummy content, creating missing parent directories on the way.
     */
    static void createResultFiles(Path outputBaseDirectory, List fileNames) {
        fileNames.each {
            Path file = outputBaseDirectory.resolve(it.toString())
            Files.createDirectories(file.parent)
            Files.writeString(file, MOCKED_FILE_CONTENT)
        }
    }

    static final qcAnalysis(Path outputBaseDirectory, String pid) {
        List<String> readGroupNames = createAlignmentAndQcResults(outputBaseDirectory, pid, false)

        createAlignmentJobs(pid, readGroupNames, [
                "mergeAndRemoveDuplicatesSlimSambamba",
                "coveragePlotSingle",
        ])
    }

    static final exomeAnalysis(Path outputBaseDirectory, String pid) {
        List<String> readGroupNames = createAlignmentAndQcResults(outputBaseDirectory, pid, true)

        createAlignmentJobs(pid, readGroupNames, [
                "mergeAndRemoveDuplicatesSlimSambamba",
                "coveragePlotSingle",
                "targetExtractCoverageSlim",
        ])
    }

    static final bisulfiteCoreAnalysis(Path outputBaseDirectory, String pid) {
        List<String> readGroupNames = createAlignmentAndQcResults(outputBaseDirectory, pid, false)

        Files.createDirectories(outputBaseDirectory.resolve(METHYLATION_DIRECTORY).resolve(MERGED_DIRECTORY))

        // the library specific results are only created, if the bam file consists of multiple libraries
        List<String> libraryDirectoryNames = alignmentLibraryDirectoryNames()
        if (libraryDirectoryNames.size() > 1) {
            libraryDirectoryNames.each { String library ->
                Files.createDirectories(outputBaseDirectory.resolve(METHYLATION_DIRECTORY).resolve(library))
                writeQaJsonFile(outputBaseDirectory.resolve(QA_DIRECTORY).resolve(library).resolve(QA_JSON_FILE))
            }
        }

        createAlignmentJobs(pid, readGroupNames, [
                "mergeAndRemoveDuplicatesSlimSambamba",
                "methylationCallingMeta",
        ])
    }

    static final RNAseqAnalysis(Path outputBaseDirectory, String pid) {
        String filePrefix = alignmentFilePrefix(pid)
        List<String> readGroupNames = alignmentReadGroupNames()

        createBamFiles(outputBaseDirectory, "${filePrefix}_merged.mdup.bam", readGroupNames)
        createResultFiles(outputBaseDirectory, ["${filePrefix}_chimeric_merged.mdup.bam"])

        // for rna the merged quality control file is stored directly in the quality control directory
        writeRnaQaJsonFile(outputBaseDirectory.resolve(QA_DIRECTORY).resolve(QA_JSON_FILE))

        // the RNAseQC results, they are the second entry of the quality control directory the workflow tests expect
        createResultFiles(outputBaseDirectory.resolve(QA_DIRECTORY).resolve(RNA_SEQC_DIRECTORY).resolve(filePrefix), [
                "${filePrefix}_RNAseQC.tgz",
                "${filePrefix}_metrics.tsv",
                "countMetrics.html",
                "genes.rpkm.gct",
                "refGene.txt.idx",
        ])

        // the arriba plot is only created, if arriba is enabled, see RoddyConfigValueService.getRunArriba
        if (configurationValues['RUN_ARRIBA'] != 'false' && !singleEndProcessing()) {
            createResultFiles(outputBaseDirectory, ["${ARRIBA_DIRECTORY}/${filePrefix}.fusions.pdf"])
        }

        createAlignmentJobs(pid, readGroupNames, ["cleanupScript"], "starAlignment")
    }

    /**
     * Create the result files shared by all workflows of the roddy AlignmentAndQCWorkflows plugin, which are the bam file with its bai and md5sum
     * file and the quality control files for the merged bam file and for each merged lane.
     *
     * @return the read group names of the merged lanes
     */
    static List<String> createAlignmentAndQcResults(Path workDirectory, String pid, boolean needsBedFile) {
        List<String> readGroupNames = alignmentReadGroupNames()

        createBamFiles(workDirectory, "${alignmentFilePrefix(pid)}_merged.mdup.bam", readGroupNames)

        Path mergedQaDirectory = workDirectory.resolve(QA_DIRECTORY).resolve(MERGED_DIRECTORY)
        writeQaJsonFile(mergedQaDirectory.resolve(QA_JSON_FILE))
        if (needsBedFile) {
            writeTargetExtractQaJsonFile(mergedQaDirectory.resolve(QA_TARGET_EXTRACT_JSON_FILE))
        }

        readGroupNames.each { String readGroupName ->
            writeQaJsonFile(workDirectory.resolve(QA_DIRECTORY).resolve(readGroupName).resolve(QA_JSON_FILE))
        }

        return readGroupNames
    }

    static void createAlignmentJobs(String pid, List<String> readGroupNames, List<String> jobNames, String alignmentJobName = "alignAndPairSlim") {
        String roddyExecutionId = "r260519_012303995_${pid}"
        readGroupNames.each {
            createJob(roddyExecutionId, alignmentJobName)
        }
        jobNames.each {
            createJob(roddyExecutionId, it)
        }
    }

    /**
     * The fastq files the bam file is created of.
     *
     * They are provided by OTP either as cvalue 'fastq_list' or, for the wgbs alignment, via the metadata table.
     */
    static List<Path> alignmentFastqFiles() {
        if (metadataTable) {
            return metadataTableColumn('SequenceFile').collect { Paths.get(it) }
        }
        String fastqList = configurationValues['fastq_list']
        assert fastqList
        return fastqList.split(';').collect { Paths.get(it) }
    }

    /**
     * The prefix of the result files, consisting of the sample type directory name and the pid.
     *
     * The sample type directory name is taken from the view by pid path of the fastq files, since it may contain the antibody target.
     */
    static String alignmentFilePrefix(String pid) {
        // the view by pid path is: .../<sampleType[-antibodyTarget]>/<libraryLayout>/<run>/sequence/<fastqFile>
        String sampleTypeDirectory = alignmentFastqFiles().first().parent.parent.parent.parent.fileName as String
        assert sampleTypeDirectory
        return "${sampleTypeDirectory}_${pid}"
    }

    /**
     * The read group names of the merged lanes, they define the names of the single lane quality control directories.
     *
     * @see de.dkfz.tbi.otp.ngsdata.SeqTrack#getReadGroupName , which this method reproduces based on the view by pid paths of the fastq files
     */
    static List<String> alignmentReadGroupNames() {
        return alignmentFastqFiles().groupBy { Path fastqFile ->
            [fastqFile.parent.parent.fileName as String, fastqFileNameWithoutMate(fastqFile)]
        }.collect { List key, List<Path> fastqFiles ->
            // for single end the mate number is part of the read group name, for paired end it is not
            String name = fastqFiles.size() == 1 ? fastqFileNameWithoutExtension(fastqFiles.first()) : key[1]
            return "${key[0]}_${name}" as String
        }.unique().sort()
    }

    /**
     * The library directory names of the merged lanes, they define the names of the library specific directories of the wgbs alignment.
     */
    static List<String> alignmentLibraryDirectoryNames() {
        assert metadataTable: "The library directory names are only available via the metadata table"
        return metadataTableColumn('Library').unique().sort()
    }

    static List<String> metadataTableColumn(String columnName) {
        List<String> lines = Files.readAllLines(metadataTable).findAll()
        int column = (lines.first().split('\t') as List).findIndexOf { it == columnName }
        assert column >= 0: "No column '${columnName}' in ${metadataTable}"
        return lines.tail().collect { it.split('\t')[column] }
    }

    static String fastqFileNameWithoutExtension(Path fastqFile) {
        return (fastqFile.fileName as String).split(/\./).first()
    }

    static String fastqFileNameWithoutMate(Path fastqFile) {
        return fastqFileNameWithoutExtension(fastqFile).replaceFirst(/_\d+$/, '')
    }

    /**
     * Create the bam file with its bai and md5sum file.
     *
     * The bam file needs to be a real bam file, since OTP reads its header with htsjdk to compare the read groups with the expected ones.
     */
    static void createBamFiles(Path workDirectory, String bamFileName, List<String> readGroupNames) {
        Files.createDirectories(workDirectory)

        Path bamFile = workDirectory.resolve(bamFileName)
        Files.write(bamFile, createBamFileContent(readGroupNames))

        Files.writeString(workDirectory.resolve("${bamFileName}.bai"), MOCKED_FILE_CONTENT)
        Files.writeString(workDirectory.resolve("${bamFileName}.md5"), "${md5sum(bamFile)}\n")
    }

    static String md5sum(Path file) {
        return MessageDigest.getInstance('MD5').digest(Files.readAllBytes(file)).encodeHex().toString()
    }

    /**
     * Create the quality control file of a bam file or of a single merged lane.
     *
     * The content is the same for all sequencing types: for sequencing types needing a bed file, OTP itself renames 'qcBasesMapped'
     * to 'allBasesMapped', see RoddyQualityAssessmentService.parseRoddyQaStatistics.
     */
    static void writeQaJsonFile(Path file) {
        Map json = ALIGNMENT_CHROMOSOMES.collectEntries { String chromosome ->
            [(chromosome): [
                    chromosome                   : chromosome,
                    referenceLength              : 1591386631,
                    genomeWithoutNCoverageQcBases: 0.011,
                    qcBasesMapped                : 1866013,
            ]]
        }
        json[QA_ALL] = [
                chromosome                     : QA_ALL,
                referenceLength                : 30956774121,
                genomeWithoutNCoverageQcBases  : 0.011,
                qcBasesMapped                  : 1866013,
                totalReadCounter               : 4213091,
                qcFailedReads                  : 10,
                duplicates                     : 8051,
                totalMappedReadCounter         : 4203691,
                pairedInSequencing             : 4213091,
                pairedRead1                    : 2091461,
                pairedRead2                    : 2121631,
                properlyPaired                 : 3847661,
                withItselfAndMateMapped        : 4192891,
                withMateMappedToDifferentChr   : 336351,
                withMateMappedToDifferentChrMaq: 61611,
                singletons                     : 10801,
                insertSizeMedian               : 3991,
                insertSizeSD                   : 931,
                insertSizeCV                   : 231,
                percentageMatesOnDifferentChr  : 1.551,
        ]

        writeJsonFile(file, json)
    }

    /**
     * Create the target extract quality control file, which OTP parses for sequencing types needing a bed file.
     */
    static void writeTargetExtractQaJsonFile(Path file) {
        Map json = (ALIGNMENT_CHROMOSOMES + [QA_ALL]).collectEntries { String chromosome ->
            [(chromosome): [
                    genomeWithoutNCoverageQcBases: 0.011,
                    qcBasesMapped                : 1466013,
            ]]
        }
        writeJsonFile(file, json)
    }

    /**
     * Create the quality control file of an rna bam file.
     *
     * It may only contain the entry 'all' and must not contain 'genomeWithoutNCoverageQcBases'. For single end data the paired end
     * specific values have to be absent, see the RnaQualityAssessment constraints.
     */
    static void writeRnaQaJsonFile(Path file) {
        Map qcValues = [
                chromosome                       : QA_ALL,
                alternativeAlignments            : 0,
                baseMismatchRate                 : 0.0123456789,
                chimericPairs                    : 0,
                cumulGapLength                   : 123456,
                duplicates                       : 12345678,
                duplicatesRate                   : 0.1234567,
                end1Antisense                    : 12345678,
                end1MappingRate                  : 0.1234567,
                end1MismatchRate                 : 0.0123456789,
                end1PercentageSense              : 0.12345678,
                end1Sense                        : 123456,
                end2Antisense                    : 123456,
                end2MappingRate                  : 0.1234567,
                end2MismatchRate                 : 0.123456789,
                end2PercentageSense              : 12.34567,
                end2Sense                        : 12345678,
                estimatedLibrarySize             : 12345678,
                exonicRate                       : 0.12345678,
                expressionProfilingEfficiency    : 0.1234567,
                failedVendorQCCheck              : 0,
                fivePNorm                        : 0.12345678,
                gapPercentage                    : 0.123456789,
                genesDetected                    : 12345,
                insertSizeMean                   : 123,
                insertSizeSD                     : 123,
                intergenicRate                   : 0.123456789,
                intragenicRate                   : 0.1234567,
                intronicRate                     : 0.12345678,
                mapped                           : 12345678,
                mappedPairs                      : 12345678,
                mappedRead1                      : 12345678,
                mappedRead2                      : 12345678,
                mappedUnique                     : 12345678,
                mappedUniqueRateOfTotal          : 0.12345678,
                mappingRate                      : 0.1234567,
                meanCV                           : 0.12345678,
                meanPerBaseCov                   : 12.34567,
                noCovered5P                      : 123,
                numGaps                          : 123,
                pairedInSequencing               : 123456789,
                properlyPaired                   : 12345678,
                properlyPairedPercentage         : 12.34,
                qcFailedReads                    : 0,
                rRNARate                         : 0.000000012345,
                rRNAReads                        : 123456,
                readLength                       : 123,
                secondaryAlignments              : 0,
                singletons                       : 0,
                singletonsPercentage             : 0.12,
                splitReads                       : 12345678,
                supplementaryAlignments          : 0,
                threePNorm                       : 0.12345678,
                totalMappedReadCounter           : 12345678,
                totalMappedReadCounterPercentage : 12.34,
                totalPurityFilteredReadsSequenced: 123456789,
                totalReadCounter                 : 123456789,
                transcriptsDetected              : 123456,
                uniqueRateofMapped               : 0.1234567,
                unpairedReads                    : 0,
                withItselfAndMateMapped          : 12345678,
                withMateMappedToDifferentChr     : 0,
                withMateMappedToDifferentChrMaq  : 0,
        ]

        if (singleEndProcessing()) {
            RNA_QA_PAIRED_END_VALUES.each {
                qcValues.remove(it)
            }
        }

        writeJsonFile(file, [(QA_ALL): qcValues])
    }

    /**
     * Whether the bam file is created of single end data.
     *
     * The value is part of the default configuration values of the rna alignment workflow: 'true' for RNA SINGLE, 'false' for RNA PAIRED.
     */
    static boolean singleEndProcessing() {
        return configurationValues['useSingleEndProcessing'] == 'true'
    }

    static final snvCallingAnalysis(Path outputBaseDirectory, String pid) {
        String snvResultPrefix = 'snvs_'
        createResultFiles(outputBaseDirectory, [
                "${snvResultPrefix}${pid}_raw.vcf.gz",
                "${snvResultPrefix}${pid}.vcf.gz",
                "${snvResultPrefix}${pid}_allSNVdiagnosticsPlots.pdf",
                "${snvResultPrefix}${pid}_snvs_stds_somatic_snvs_conf_0_to_10.vcf",
        ])

        String roddyExecutionId = "r260519_012303995_${pid}"
        (1..24).each {
            createJob(roddyExecutionId, "snvCalling")
        }
        createJob(roddyExecutionId, "snvJoinVcfFiles")
        createJob(roddyExecutionId, "snvAnnotation")
        createJob(roddyExecutionId, "snvDeepAnnotation")
        createJob(roddyExecutionId, "snvFilter")
    }

    static final indelCallingAnalysis(Path outputBaseDirectory, String pid) {
        String indelResultPrefix = 'indel_'
        createResultFiles(outputBaseDirectory, [
                "${indelResultPrefix}${pid}.vcf.gz",
                "${indelResultPrefix}${pid}.vcf.raw.gz",
                "screenshots/${indelResultPrefix}somatic_functional_combined.pdf",
                "snvs_${pid}.GTfiltered_gnomAD.Germline.Rare.Rescue.png",
        ])

        createIndelQcJsonFile(outputBaseDirectory, pid)
        createSampleSwapJsonFile(outputBaseDirectory, pid)

        String roddyExecutionId = "r260519_012303995_${pid}"
        createJob(roddyExecutionId, "indelCalling")
        createJob(roddyExecutionId, "indelAnnotation")
        createJob(roddyExecutionId, "indelDeepAnnotation")
        createJob(roddyExecutionId, "indelVcfFilter")
        createJob(roddyExecutionId, "checkSampleSwap")
    }

    /**
     * Create the indel qc file, which is parsed by the IndelParseJob into an IndelQualityControl.
     *
     * The value of 'file' needs to be an absolute path, since the domain uses the shared constraint 'absolutePath'.
     */
    static void createIndelQcJsonFile(Path outputBaseDirectory, String pid) {
        Map qcValues = [
                file                 : outputBaseDirectory.resolve("indel_${pid}.vcf.gz").toString(),
                numIndels            : 23,
                numIns               : 24,
                numDels              : 25,
                numSize1_3           : 26,
                numSize4_10          : 27,
                numSize11plus        : 28,
                numInsSize1_3        : 29,
                numInsSize4_10       : 30,
                numInsSize11plus     : 31,
                numDelsSize1_3       : 32,
                numDelsSize4_10      : 33,
                numDelsSize11plus    : 34,
                percentIns           : 35.0,
                percentDels          : 36.0,
                percentSize1_3       : 37.0,
                percentSize4_10      : 38.0,
                percentSize11plus    : 39.0,
                percentInsSize1_3    : 40.0,
                percentInsSize4_10   : 41.0,
                percentInsSize11plus : 42.0,
                percentDelsSize1_3   : 43.0,
                percentDelsSize4_10  : 44.0,
                percentDelsSize11plus: 45.0,
        ]

        writeJsonFile(outputBaseDirectory.resolve("indel.json"), [all: qcValues])
    }

    /**
     * Create the sample swap file, which is parsed by the IndelParseJob into an IndelSampleSwapDetection.
     */
    static void createSampleSwapJsonFile(Path outputBaseDirectory, String pid) {
        Map sampleSwapValues = [
                pid                                             : pid,
                somaticSmallVarsInTumorCommonInGnomADPer        : 1,
                somaticSmallVarsInControlCommonInGnomad         : 2,
                tindaSomaticAfterRescue                         : 3,
                somaticSmallVarsInControlInBiasPer              : 4,
                somaticSmallVarsInTumorPass                     : 5,
                somaticSmallVarsInControlPass                   : 6,
                somaticSmallVarsInControlPassPer                : 7,
                tindaSomaticAfterRescueMedianAlleleFreqInControl: 8.0,
                somaticSmallVarsInTumorInBiasPer                : 9.0,
                somaticSmallVarsInControlCommonInGnomadPer      : 10,
                somaticSmallVarsInTumorInBias                   : 11,
                somaticSmallVarsInControlCommonInGnomasPer      : 12,
                germlineSNVsHeterozygousInBothRare              : 13,
                germlineSmallVarsHeterozygousInBothRare         : 14,
                tindaGermlineRareAfterRescue                    : 15,
                somaticSmallVarsInTumorCommonInGnomad           : 16,
                somaticSmallVarsInControlInBias                 : 17,
                somaticSmallVarsInControl                       : 18,
                somaticSmallVarsInTumor                         : 19,
                germlineSNVsHeterozygousInBoth                  : 20,
                somaticSmallVarsInTumorPassPer                  : 21.9,
                somaticSmallVarsInTumorCommonInGnomadPer        : 22,
                germlineSmallVarsInBothRare                     : 23,
        ]

        writeJsonFile(outputBaseDirectory.resolve("checkSampleSwap.json"), sampleSwapValues)
    }

    static final sophiaAnalysis(Path outputBaseDirectory, String pid) {
        String tumorSampleType = configurationValues['possibleTumorSampleNamePrefixes']
        String controlSampleType = configurationValues['possibleControlSampleNamePrefixes']
        assert tumorSampleType
        assert controlSampleType

        createResultFiles(outputBaseDirectory, [
                "svs_${pid}_filtered_somatic_minEventScore3.tsv",
                "svs_${pid}_${tumorSampleType}-${controlSampleType}_filtered.tsv_score_3_scaled_merged.pdf",
        ])

        createSophiaQcJsonFile(outputBaseDirectory)

        String roddyExecutionId = "r260519_012303995_${pid}"
        createJob(roddyExecutionId, "sophia")
        createJob(roddyExecutionId, "sophia")
        createJob(roddyExecutionId, "sophiaAnnotator")
    }

    /**
     * Create the sophia qc file, which is parsed by the SophiaParseJob into a SophiaQc.
     */
    static void createSophiaQcJsonFile(Path outputBaseDirectory) {
        Map qcValues = [
                controlMassiveInvPrefilteringLevel   : 0,
                tumorMassiveInvFilteringLevel        : 0,
                rnaContaminatedGenesMoreThanTwoIntron: "PRKRA;ACTG2;TYRO3;COL18A1;",
                rnaContaminatedGenesCount            : 4,
                rnaDecontaminationApplied            : false,
        ]

        writeJsonFile(outputBaseDirectory.resolve("qualitycontrol.json"), [all: qcValues])
    }

    static final copyNumberEstimationAnalysis(Path outputBaseDirectory, String pid) {
        createResultFiles(outputBaseDirectory, [
                "${pid}_tcn_distances_combined_star.png",
                // the 'ALL' and 'extra' plots are searched by pattern, see AbstractAceseqFileService.getPlots
                "${pid}_plot_${ACESEQ_PLOIDY_FACTOR}_ALL.png",
                "${pid}_plot_${ACESEQ_PLOIDY_FACTOR}extra_${ACESEQ_TUMOR_CELL_CONTENT}_1.png",
                "plots/${pid}_gc_corrected.png",
                "plots/${pid}_qc_rep_corrected.png",
                "plots/control_${pid}_wholeGenome_coverage.png",
        ])

        createAceseqQcJsonFile(outputBaseDirectory, pid)

        String roddyExecutionId = "r260519_012303995_${pid}"
        (1..24).each {
            createJob(roddyExecutionId, "cnvSnpGeneration")
        }
        (1..22).each {
            createJob(roddyExecutionId, "imputeGenotypes")
        }
        [
                "annotateCnvFiles",
                "mergeAndFilterCnvFiles",
                "correctGcBias",
                "mergeAndFilterSnpFiles",
                "imputeGenotypes_X",
                "addHaplotypesToSnpFile",
                "createControlBafPlots",
                "getBreakpoints",
                "mergeBreakpointsAndSvDelly",
                "getSegmentsAndSnps",
                "markHomozygousDeletions",
                "segmentsToSnpDataHomodel",
                "clusterAndPruneSegments",
                "segmentsToSnpDataPruned",
                "estimatePeaksForPurity",
                "estimatePurityPloidy",
                "generateResultsAndPlots",
                "generateVcfFromTab",
        ].each {
            createJob(roddyExecutionId, it)
        }
    }

    /**
     * Create the aceseq qc file, which is parsed by the AceseqParseJob into AceseqQc objects.
     *
     * The AceseqParseJob parses some of the values with 'Double.parseDouble', therefore all values have to be strings.
     * Only the entry '1' is created, since the workflow tests expect exactly one AceseqQc with number 1.
     */
    static void createAceseqQcJsonFile(Path outputBaseDirectory, String pid) {
        Map qcValues = [
                gender          : "male",
                solutionPossible: "3",
                tcc             : ACESEQ_TUMOR_CELL_CONTENT,
                goodnessOfFit   : "0.904231625835189",
                ploidyFactor    : ACESEQ_PLOIDY_FACTOR,
                ploidy          : "2",
        ]

        writeJsonFile(outputBaseDirectory.resolve("cnv_${pid}_parameter.json"), ["1": qcValues])
    }

    /**
     * Create the content of a bam file containing only a header with the given read groups and no alignment records.
     *
     * OTP reads the header of the bam file with htsjdk to compare the read groups with the expected ones, therefore a text file is not
     * sufficient here. Additionally the workflow tests require the bam file to be a readable gzip file of more than 1024 bytes, which is
     * achieved by a padding comment line in the header and by storing the block uncompressed.
     *
     * @see <a href="https://samtools.github.io/hts-specs/SAMv1.pdf">the sam/bam specification</a>
     */
    static byte[] createBamFileContent(List<String> readGroupNames) {
        assert readGroupNames

        StringBuilder header = new StringBuilder()
        header << "@HD\tVN:1.6\tSO:coordinate\n"
        ALIGNMENT_CHROMOSOMES.each {
            header << "@SQ\tSN:${it}\tLN:${CHROMOSOME_LENGTH}\n"
        }
        readGroupNames.each {
            header << "@RG\tID:${it}\tSM:${it}\tLB:${it}\tPL:ILLUMINA\n"
        }
        header << "@CO\t${MOCKED_FILE_CONTENT * BAM_HEADER_PADDING_COUNT}\n"

        byte[] headerBytes = header.toString().getBytes('UTF-8')

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        out.write("BAM\1".getBytes('UTF-8'))
        writeInt32(out, headerBytes.length)
        out.write(headerBytes)
        writeInt32(out, ALIGNMENT_CHROMOSOMES.size())
        ALIGNMENT_CHROMOSOMES.each { String chromosome ->
            byte[] name = chromosome.getBytes('UTF-8')
            writeInt32(out, name.length + 1)
            out.write(name)
            out.write(0)
            writeInt32(out, CHROMOSOME_LENGTH)
        }

        ByteArrayOutputStream bamFile = new ByteArrayOutputStream()
        bamFile.write(createBgzfBlock(out.toByteArray()))
        bamFile.write(BGZF_EOF_BLOCK)
        return bamFile.toByteArray()
    }

    /**
     * Wrap the given data in a single bgzf block, the block gzip format bam files are stored in.
     */
    static byte[] createBgzfBlock(byte[] data) {
        Deflater deflater = new Deflater(Deflater.NO_COMPRESSION, true)
        deflater.setInput(data)
        deflater.finish()
        byte[] deflated = new byte[data.length + 1024]
        int deflatedLength = deflater.deflate(deflated)
        assert deflater.finished()
        deflater.end()

        CRC32 crc = new CRC32()
        crc.update(data)

        // the size of the gzip header including the bgzf extra field, the deflated data and the gzip trailer
        int blockSize = 18 + deflatedLength + 8
        assert blockSize <= 65536

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        out.write(toBytes([
                0x1f, 0x8b, 0x08, 0x04, // magic, deflate, extra field present
                0x00, 0x00, 0x00, 0x00, // modification time
                0x00, 0xff,             // extra flags, unknown operating system
                0x06, 0x00,             // length of the extra field
                0x42, 0x43, 0x02, 0x00, // the bgzf sub field 'BC' of length 2
        ]))
        writeInt16(out, blockSize - 1)
        out.write(deflated, 0, deflatedLength)
        writeInt32(out, (int) crc.value)
        writeInt32(out, data.length)

        return out.toByteArray()
    }

    static void writeInt16(OutputStream out, int value) {
        out.write(value & 0xff)
        out.write((value >> 8) & 0xff)
    }

    static void writeInt32(OutputStream out, int value) {
        writeInt16(out, value & 0xffff)
        writeInt16(out, (value >> 16) & 0xffff)
    }

    static byte[] toBytes(List<Integer> values) {
        return values.collect { it as byte } as byte[]
    }

    static void writeJsonFile(Path file, Map content) {
        Files.createDirectories(file.parent)
        Files.writeString(file, JsonOutput.prettyPrint(JsonOutput.toJson(content)))
    }

    static void createJob(String prefix, String name) {
        String clusterId = System.nanoTime()

        Path path = Path.of(System.getenv("HOME"), 'jobs', clusterId)
        Files.createDirectories(path)

        Path state = path.resolve('state')
        state.text = 'PEND\n'

        Path inputFile = path.resolve("input")
        inputFile.text = "roddy call for ${name}"

        Path outputFile = path.resolve("output")
        outputFile.text = "OK"

        Path exitCode = path.resolve("exitCode")
        exitCode.text = "0\n"

        String startEntry = "${clusterId}:STARTED:123:${name}"
        String endEntry = "${clusterId}:0:456:${name}"
        String newText = "${Files.readString(jobStateLogfile)}${startEntry}\n${endEntry}\n"

        Files.writeString(jobStateLogfile, newText)

        println "Rerun job ${prefix}_${name} => ${clusterId}"
    }
}
