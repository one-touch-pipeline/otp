
The goal of the application is to output statistics and other relevant information about quality of the alignment file (BAM).


Application parameters:

    - Path to the BAM file (input file)
    - Path to the index (bai) file (input file)
    - Path to JSON file that contains statistics related to the reads alignment to a reference genome and grouped by chromosome (Output file).
    - Path to coverage data TSV file that contains the count of reads by bin size (Output file).
    - Path to insert sizes TSV file that contains the count of reads by the absolute value of the insert size for all reads (Output file).
    - Flag to remove output: if "true" overwrite the existing results, if "false" the execution will be stopped.
    - Identifier to represent the set of statistics collected for all chromosomes. (String with a length between 1 and 50 characters, without empty spaces. common value: "ALL").
    - Minimum length a read should be aligned to the reference sequence (Integer between 0 and 10000. common value: 36).
    - Minimum Mean Base Quality that is used to decide if a read is mapped with low or high quality (Integer between 0 and 100. common value: 0).
    - Quality threshold. Cut-off value to decide if read is mapped at all and included into mapping calculation (Integer between 0 and 100. common value: 0).
    - Coverage Mapping Quality Threshold. Cut-off value to decide if a read is added for the coverage plot. (Integer between 0 and 100. common value: 1).
    - Bin size for the coverage plot in bp (Integer between 100 and 10000. common value: 1000).
    - Bin size for the insert size histogram (Integer between 1 and 1000000. common value: 10).
    - Flag for filtering special chromosomes: if "true" special chromosomes (*, m, chrM) are excluded from the "ALL" chromosome statistics. If "false" no filtering is used.

Example execution:

java -jar application.jar file.bam file.bai ./stats.json ./coverage.tsv ./insertSize.tsv false ALL 36 0 0 1 1000 10 false



Gradle Tasks:

This is a default gradle project developed in groovy, so a build.gradle file at the root of project is expected. Beyond the default set of tasks to allow jar generation, other tasks to run the application locally (with a small sample bam file) and tests were created.
For an extensive list of available tasks just run "gradle tasks" at the console at project root.

If using the build script to run the jar and data comparison tests, some options are hardcoded at the build.gradle and some (MinAlignedRecordLength, MinMeanBaseQuality and WindowSize) are obtained through parsing of the CO group set of scripts (to avoid duplication and hard to detect inconsistencies if they change in future versions CO scripts).

Consider that for development all results previously generated will be overwritten. (the "overrideOutput" flag is set to "true" at the build.gradle)
The test mode option was created for the generation of results that matched the ones from the first version of the CO group (given that the co group were not filtering some reads in flagstats)


Data repository structure:

To allow comparison of results from the application, against the set of scripts from CO Group, some representative Bam (and Bai) files have to be stored in a location reachable by the cluster

File structure for the repository of data:

.
│  
├── generateData.sh
│  
├── bam-files
│  
├── co-scripts
│   └── v1
│   └── v2
│  
└── latest-co-output


Description:
    - generateData.sh is used to generate the required gold standard data through the submission of the scripts from the CO group to the cluster. (It is used by calling the "generateGoldStandard" task in gradle but could be used independently (if the right parameters are passed))
    - bam-Files is the repository for the representative bam (and bai) files used as input for both the application and the CO scripts
    - co-scripts is the repository of different versions of the scripts from the co group. (only results from the last version (highest index) are expected to be found at the latest-co-output)
    - latest-co-output is the repository of resulting data from the execution of the CO group scripts


The required filepaths have to be declared at the top of the build.gradle and should be set to the test environment, which is needed to allow tests through development.
The "resultsDir" path is the output directory. For execution of the application and for development purposes it could be defined locally (if cluster access for the execution of jobs is not required)



Testing:

The testing of the application with real data is resources consuming (average BAM files with 100Gb), so submission of jobs to the cluster is required. For development tests, a small BAM file could be used. The build.gradle constains a hardcoded reference to a small bam that is used by the task "runSmallBam"

Since the application was develop based in previously developed CO group set of scripts and we have adopted other formats besides some different values as output, we have created some tasks to allow the automatic generation and comparison of data.

The tasks could be run independently of any IDE as simple as "gradle taskName"

Tasks for development tests

    - runSmallBam
        runs the application locally on the previously declared BAM file and set of parameters hardcode in the task


Tasks for generation of data  (submitting the jobs to the cluster)

    This tasks at the moment have to be executed on the cluster head (since gradle is not installed in the cluster, will be necessary to have gradle installed in the home folder)

    - generateTestData
        Generates all data using the created jar and outputting resulting files to the predefined "resultsDir" (has to be defined previously at the build.gradle)

    - generateGoldStandardData
        Generates all data using the CO set of scripts and outputting resulting files to the predefined "dataRepositoryDir"


Tasks for comparison of generated data

    - comparisonTest
        It compare results from the test data against the gold Standard data (generated by CO set of scripts) and outputs a summary report of the comparison to the console (should be run after the entire generation of data is completed sucessfully.. otherwise will throw exceptions for not finding the resulting files..)
        The statistics from set of files generated by the application is compared against the collected values outputted in the set of files generated by the CO group scripts

        Compared files
        The generated coverage data file format (tab separated values) is exactly the same as the CO group, so comparison is straightforward.
        The generated statistics (json format) are compared against results gathered from several different files from the CO group.
        The generated insert size file format (tab separated values) and values are also not the same as the CO group, and no direct comparison of values is performed. Only the mean and standard Deviation percentage comparison of the values is performed.
