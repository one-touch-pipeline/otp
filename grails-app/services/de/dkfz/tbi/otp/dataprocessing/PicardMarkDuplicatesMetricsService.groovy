package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.MetaDataEntry
import de.dkfz.tbi.otp.ngsdata.SavingException

/**
 * Service for parsing the metrics file and saving the MarkDuplicates in the database.
 *
 */
class PicardMarkDuplicatesMetricsService {

    /**
     * Defines the headers of the metrics values and how the property is named in {@link PicardMarkDuplicatesMetrics}.
     * The definition is used for filling the Dom class.
     *
     */
    private static enum Header {
        LIBRARY("library"),
        UNPAIRED_READS_EXAMINED("unpaired_reads_examined"),
        READ_PAIRS_EXAMINED("read_pairs_examined"),
        UNMAPPED_READS("unmapped_reads"),
        UNPAIRED_READ_DUPLICATES("unpaired_read_duplicates"),
        READ_PAIR_DUPLICATES("read_pair_duplicates"),
        READ_PAIR_OPTICAL_DUPLICATES("read_pair_optical_duplicates"),
        PERCENT_DUPLICATION("percent_duplication"),
        ESTIMATED_LIBRARY_SIZE("estimated_library_size")

        /**
         * The name of the property in {@link PicardMarkDuplicatesMetrics}.
         */
        String property

        private Header(String property) {
            this.property = property
        }
    }

    /**
     * Header prefix used to find the metrics entry in the file
     */
    private static final String METRICS_CLASS = "## METRICS CLASS"

    /**
     * The expected header line.
     * This line is expected after the {@link #METRICS_CLASS} line.
     */
    private static final String HEADER = "LIBRARY\tUNPAIRED_READS_EXAMINED\tREAD_PAIRS_EXAMINED\tUNMAPPED_READS\tUNPAIRED_READ_DUPLICATES\tREAD_PAIR_DUPLICATES\tREAD_PAIR_OPTICAL_DUPLICATES\tPERCENT_DUPLICATION\tESTIMATED_LIBRARY_SIZE"

    ProcessedMergedBamFileService processedMergedBamFileService

    /**
     * Loads the metrics of the file into the database.
     * The metrics file of the given {@link ProcessedMergedBamFile} is searched for the metrics statistics,
     * which are parsed and saved into the database.
     * If the file does not exist or no metrics can be found or can not be parsed, an {@link RuntimeException}
     * is thrown.
     *
     * @param processedMergedBamFile the {@link ProcessedMergedBamFile} the metrics file is parsed and loaded for.
     * @return true, if the method is successful.
     */
    public boolean parseAndLoadMetricsForProcessedMergedBamFiles(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile are not allowed to be null")
        String fileName = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        File file = new File(fileName)
        validateFile(file)
        Iterator<String> lines = file.readLines().iterator()
        while (lines.hasNext()) {
            String line = lines.next()
            if (line.startsWith(METRICS_CLASS)) {
                String[] metricsClass = line.split("\t")
                line = lines.next()
                isTrue(HEADER.equals(line), "The found header does not match: found: '${line}', expected '${HEADER}'")
                line = lines.next()
                String[] values = line.split("\t")
                isTrue(values.length == Header.values().length, "Wrong count of parameters, expected '${Header.values().length}', but found '${values.length}'")
                PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = new PicardMarkDuplicatesMetrics()
                for (Header header: Header.values()) {
                    MetaProperty metaProperty = picardMarkDuplicatesMetrics.getMetaClass().getMetaProperty("${header.property}")
                    Object value
                    try {
                        switch (metaProperty.getType()) {
                            case String.class:
                                value = values[header.ordinal()]
                                break
                            case long.class:
                                value = values[header.ordinal()] as Long
                                break
                            case double.class:
                                value = values[header.ordinal()] as Double
                                break
                            default :
                                throw new RuntimeException("unknown: " + metaProperty.getType())
                        }
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Error converting value '${value}' to ${metaProperty.getType()} of ${header}", e)
                    }
                    picardMarkDuplicatesMetrics."${header.property}" = value
                }
                picardMarkDuplicatesMetrics.metricsClass = metricsClass[1]
                picardMarkDuplicatesMetrics.abstractBamFile = processedMergedBamFile
                assertSave(picardMarkDuplicatesMetrics)
                return true
            }
        }
        throw new RuntimeException("No metrics info could be found in file ${fileName}")
    }

    /**
     * checks, if file exist, is readable and is not empty
     */
    private void validateFile(File file) {
        if (!file.exists()) {
            throw new RuntimeException("The metrics file ${file} does not exist")
        }
        if (!file.canRead()) {
            throw new RuntimeException("Can not read the metrics file ${file}")
        }
        if (!file.size()) {
            throw new RuntimeException("The metrics file ${file} is empty")
        }
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
