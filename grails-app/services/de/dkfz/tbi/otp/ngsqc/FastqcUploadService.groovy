package de.dkfz.tbi.otp.ngsqc

import java.util.regex.*

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Service providing methods to parse FastQC files and saving the parsed data to the database
 */
class FastqcUploadService {

    FastqcDataFilesService fastqcDataFilesService

    /**
     * Uploads the fastQC file contents generated from the fastq file to the database
     */
    public void uploadFileContentsToDataBase(FastqcProcessedFile fastqc) {
        String pathToFile = fastqcDataFilesService.fastqcOutputFile(fastqc.dataFile)
        final String dataFileName = "fastqc_data.txt"
        try {
            uploadFileContentsToDataBasePrivate(fastqc, fastqcDataFilesService.getInputStreamFromZip(pathToFile, dataFileName).text)
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to load data from ${dataFileName} inside ${pathToFile} into the database.", t)
        }
    }

    /**
     * Uploads the fastQC file contents generated from the fastq file to the database
     * @param fastqc String containing the contents of the fastqc file
     * @param fastqDataFile reference to the fastq dataFile the fastQC was generated from
     */
    private void uploadFileContentsToDataBasePrivate(FastqcProcessedFile fastqDataFile, String fastqc) {
        log.debug("Start parsing the data...")
        String myRegularExpression = /(?s)>>.*?>>END_MODULE/
        Matcher matcher = ( fastqc =~ myRegularExpression )
        Map<String,String> data = [:]
        Map<String,String> dataStatus = [:]
        // For each module (starting with '>>' and ending with '>>END_MODULE'),
        // will be stored two maps, one with the content and the other with status of each module
        matcher.each {
            String title = it.substring(2,it.indexOf('\t')).trim()
            String status = it.substring(it.indexOf('\t'),it.indexOf('\n')).trim()
            String content = it.substring(it.indexOf('\n'),it.indexOf('>>END_MODULE')).trim()
            data[title] = content
            dataStatus[title] = status
        }
        parseAndSaveModulesStatus(dataStatus, fastqDataFile)
        parseAndSaveBasicStatistics(data, fastqDataFile)
        parseAndSaveBaseSequenceAnalysis(data, fastqDataFile)
        parseAndSaveSeqQualityScores(data, fastqDataFile)
        parseAndSaveSeqGCContent(data, fastqDataFile)
        parseAndSaveSeqLengthDistribution(data, fastqDataFile)
        parseAndSaveSeqDupLevels(data, fastqDataFile)
        parseAndSaveOverSeq(data, fastqDataFile)
        parseAndSaveKmerContent(data, fastqDataFile)
        log.debug "Parsing and upload of modules successful"
    }

    /**
     * Returns the tabular part of the module text contents
     * @param text Module text contents
     * @return The tabular part of the module text contents
     */
    private String getModuleTableBody(String text) {
        // skips the first line with title columns
        return text.substring(text.indexOf('\n')+1,text.size())
    }

    /**
     * Parse and Saves Modules Status
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private parseAndSaveModulesStatus(Map<String,String> dataStatus, FastqcProcessedFile fastqcDataFile) {
        log.debug "Parsing and saving Modules Status..."
        FastqcModuleStatus fastqcStat
        FastqcModule fastqcModule
        dataStatus.each {
            fastqcStat = new FastqcModuleStatus()
            fastqcModule = FastqcModule.findByName(it.key)
            if (!fastqcModule) {
                log.debug "Not identified Module ${it.key}."
                throw new Exception("Not identified Module found '${it.key}'")
            }
            fastqcStat.module = fastqcModule
            fastqcStat.status = it.value.toUpperCase() as FastqcModuleStatus.Status // not sure this is best way...
            fastqcStat.fastqcProcessedFile = fastqcDataFile
            fastqcStat.save(flush: true)
        }
    }

    /**
     * Parse and fill the data on the 'FastqcPerBaseSequenceAnalysis' table. Data from different modules at the fastqc file is going to the same table
     * Per base sequence analysis                      #Base   Mean    Median  Lower Quartile  Upper Quartile  10th Percentile 90th Percentile
     * Per base sequence content                       #Base   G       A       T       C
     * Per base GC content                             #Base   %GC
     * Per base N content                              #Base   N-Count
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveBaseSequenceAnalysis(Map<String,String> data, FastqcProcessedFile fastqcDataFile) {
        log.debug "Parsing Base Sequence Analysis data..."
        List<FastqcPerBaseSequenceAnalysis> listFastqc = []
        log.debug " Parsing 'Per base sequence quality'"
        String text = data['Per base sequence quality']
        String body = getModuleTableBody(text)

        String[] fields
        body.eachLine {String line, int number ->
            listFastqc << new FastqcPerBaseSequenceAnalysis()
            fields = line.split('\t')
            listFastqc[number].cycle = fields[0] as int
            listFastqc[number].mean = fields[1] as double
            listFastqc[number].median = fields[2] as double
            listFastqc[number].q1 = fields[3] as double
            listFastqc[number].q3 = fields[4] as double
            listFastqc[number].p10th = fields[5] as double
            listFastqc[number].p90th = fields[6] as double
        }
        log.debug " Parsing 'Per base sequence content'"
        text =  data['Per base sequence content']
        body = getModuleTableBody(text)
        body.eachLine {String line, int number ->
            fields = line.split('\t')
            listFastqc[number].countOfNucleotideG = fields[1] as double
            listFastqc[number].countOfNucleotideA = fields[2] as double
            listFastqc[number].countOfNucleotideT = fields[3] as double
            listFastqc[number].countOfNucleotideC = fields[4] as double
        }
        log.debug " Parsing 'Per base GC content'"
        text =  data['Per base GC content']
        body = getModuleTableBody(text)
        body.eachLine {String line, int number ->
            fields = line.split('\t')
            listFastqc[number].percentageOfGC = fields[1] as double
        }
        log.debug " Parsing 'Per base N content'"
        text =  data['Per base N content']
        body = getModuleTableBody(text)
        body.eachLine {String line, int number ->
            fields = line.split('\t')
            listFastqc[number].countOfNucleotideN = fields[1] as double
            listFastqc[number].fastqcProcessedFile = fastqcDataFile
            listFastqc[number].save(flush: true)
        }
    }

    /**
     * Parses and saves "Kmer Content"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated filev
     */
    private void parseAndSaveKmerContent(Map<String,String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and saving 'Kmer Content'"
        String text = data['Kmer Content']
        String body = getModuleTableBody(text)
        FastqcKmerContent fastqckmer
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            fastqckmer = new FastqcKmerContent()
            fastqckmer.sequence = fields[0]
            fastqckmer.countOfKmer = fields[1] as int
            fastqckmer.overall = fields[2] as double
            fastqckmer.max = fields[3] as double
            fastqckmer.position = fields[4] as int
            fastqckmer.fastqcProcessedFile = fastqc
            fastqckmer.save(flush: true)
        }
    }

    /**
     * Parses and saves "Overepresented sequences"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveOverSeq(Map<String,String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and saving 'Overrepresented sequences'"
        String text = data['Overrepresented sequences']
        // skips the first line with title columns
        String body = getModuleTableBody(text)
        FastqcOverrepresentedSequences fastqcOverRepSeq
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            fastqcOverRepSeq = new FastqcOverrepresentedSequences()
            fastqcOverRepSeq.sequence = fields[0]
            fastqcOverRepSeq.countOverRep = fields[1] as int
            fastqcOverRepSeq.percentage = fields[2] as double
            fastqcOverRepSeq.possibleSource =fields[3]
            fastqcOverRepSeq.fastqcProcessedFile = fastqc
            fastqcOverRepSeq.save(flush: true)
        }
    }

    /**
     * Parses and saves "Sequence Duplication levels"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveSeqDupLevels(Map<String,String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and saving 'Sequence Duplication Levels'"
        String text = data['Sequence Duplication Levels']
        // skips the two first lines with the title columns
        String body = text.substring(text.indexOf('Relative count\n')+15,text.size())
        FastqcSequenceDuplicationLevels fastqcSeqDupLevels
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            fastqcSeqDupLevels = new FastqcSequenceDuplicationLevels()
            try {
                fastqcSeqDupLevels.duplicationLevel = fields[0] as int
            } catch (NumberFormatException nfe) {
                // The last of the entrances have the format "10++" meaning, "more than 10". To insert it in the database, will be removed the trailing "++"
                fastqcSeqDupLevels.duplicationLevel = fields[0].substring(0,fields[0].indexOf("+")) as int
            }
            fastqcSeqDupLevels.relativeCount = fields[1] as double
            fastqcSeqDupLevels.fastqcProcessedFile = fastqc
            fastqcSeqDupLevels.save(flush: true)
        }
    }

    /**
     * Parses and saves "Per sequence GC content"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveSeqGCContent(Map<String,String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and Saving 'Per sequence GC content'"
        String text = data['Per sequence GC content']
        String body = getModuleTableBody(text)
        FastqcPerSequenceGCContent fastqcSeqGC
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            fastqcSeqGC = new FastqcPerSequenceGCContent()
            fastqcSeqGC.percentageOfGC = fields[0] as int
            fastqcSeqGC.countGC = fields[1] as double
            fastqcSeqGC.fastqcProcessedFile = fastqc
            fastqcSeqGC.save(flush: true)
        }
    }

    /**
     * Parses and saves "Per sequence quality scores"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveSeqQualityScores(Map<String, String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and Saving 'Per sequence quality scores'"
        String text = data['Per sequence quality scores']
        String body = getModuleTableBody(text)
        FastqcPerSequenceQualityScores fastqcSeqQualScore
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            fastqcSeqQualScore = new FastqcPerSequenceQualityScores()
            fastqcSeqQualScore.quality = fields[0] as int
            fastqcSeqQualScore.countQS = fields[1] as double
            fastqcSeqQualScore.fastqcProcessedFile = fastqc
            fastqcSeqQualScore.save(flush: true)
        }
    }

    /**
     * Parses and saves "Basic Statistics"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveBasicStatistics(Map<String,String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and Saving 'Basic Statistics'"
        String text = data['Basic Statistics']
        String body = getModuleTableBody(text)
        log.debug(body)
        FastqcBasicStatistics fastqcBasicStats = new FastqcBasicStatistics()
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            switch (fields[0]) {
                case "File type":
                    fastqcBasicStats.fileType = fields[1]
                    break
                case "Encoding":
                    fastqcBasicStats.encoding = fields[1]
                    break
                case "Total Sequences":
                    fastqcBasicStats.totalSequences = fields[1] as long
                    break
                case "Filtered Sequences":
                    fastqcBasicStats.filteredSequences = fields[1] as long
                    break
                case "Sequence length":
                    fastqcBasicStats.sequenceLength = fields[1]
                    break
            }
        }
        //This section is related with the 'Sequence Duplication Levels' module that will be stored together with the other 'Basic Statistics'
        text = data['Sequence Duplication Levels']
        String labelTDP = '#Total Duplicate Percentage'
        String totalDuplicatePercentage = text.substring(text.indexOf(labelTDP)+labelTDP.size(),text.indexOf('\n')).trim()
        fastqcBasicStats.totalDuplicatePercentage = totalDuplicatePercentage as double
        fastqcBasicStats.fastqcProcessedFile = fastqc
        fastqcBasicStats.save(flush: true)
    }

    /**
     * Parsing and saving "Sequence Length Distribution"
     * @param data A Map with the identifier of the module as a key and the content text of the module as a value
     * @param fastqc A DataFile object representing an already processed Fastq file and a FastQC generated file
     */
    private void parseAndSaveSeqLengthDistribution(Map<String,String> data, FastqcProcessedFile fastqc) {
        log.debug "Parsing and Saving 'Sequence Length Distribution'"
        String text = data['Sequence Length Distribution']
        String body = getModuleTableBody(text)
        FastqcSequenceLengthDistribution fastqcSeqLengDist
        String[] fields
        body.eachLine {String line ->
            fields = line.split('\t')
            fastqcSeqLengDist = new FastqcSequenceLengthDistribution()
            fastqcSeqLengDist.length = fields[0]
            fastqcSeqLengDist.countSequences = fields[1] as double
            fastqcSeqLengDist.fastqcProcessedFile = fastqc
            fastqcSeqLengDist.save(flush: true)
        }
    }
}
