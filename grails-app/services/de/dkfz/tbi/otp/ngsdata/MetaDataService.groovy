package de.dkfz.tbi.otp.ngsdata

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class MetaDataService {

    // locks for operation that are not tread safe
    private final Lock loadMetaDataLock = new ReentrantLock()

    /**
     * Dependency injection of file type service
     */
    def fileTypeService
    def metaDataFileService
    static transactional = true

    /**
     *
     * this method loads registered input meta-data files
     * into database table MetaDataEntry
     *
     * @param runId - database id or the Run object
     */
    void loadMetaData(long runId) {
        Run run = Run.get(runId)
        log.debug("loading metadata for run ${run.name}")
        // loading metadata is not thread save - use a lock
        loadMetaDataLock.lock()
        try {
            processMetaDataFiles(run)
        } finally {
            loadMetaDataLock.unlock()
        }
    }

    private void processMetaDataFiles(Run run) {
        List<RunSegment> paths = RunSegment.findAllByRunAndMetaDataStatus(run, RunSegment.Status.PROCESSING)
        List<MetaDataFile> mdFiles = MetaDataFile.findAllByRunSegmentInList(paths)
        mdFiles.each { MetaDataFile file ->
            if (file.used) {
                return
            }
            log.debug("\tfound md souce file ${file.fileName}")
            processMetaDataFile(file)
            run.save(flush: true)
        }
    }

    private void processMetaDataFile(MetaDataFile file) {
        FileType.Type type = getTypeInMetaDataFile(file.fileName)
        File mdFile = openTextFile(file)
        List<String> tokens
        List<String> values
        List<MetaDataKey> keys
        DataFile dataFile
        mdFile.eachLine { line, no ->
            // line numbering starts at 1 and not at 0
            if (no == 1) {
                // parse the header
                tokens = tokenize(line, '\t')
                keys = getKeysFromTokens(tokens)
            } else {
                // match values with the header
                // new entry in MetaData
                dataFile = new DataFile(
                    run : file.runSegment.run,
                    runSegment : file.runSegment
                )
                dataFile.validate()
                dataFile.save(flush: true)
                values = tokenize(line, '\t')
                addMetaDataEntries(dataFile, keys, values)
                // fill-up important fields
                assignFileName(dataFile)
                fillVbpFileName(dataFile)
                fillMD5Sum(dataFile)
                assignFileType(dataFile, type)
                addKnownMissingMetaData(dataFile)
                checkIfWithdrawn(dataFile)
            }
        }
        file.used = true
        file.save(flush: true)
    }

    private FileType.Type getTypeInMetaDataFile(String fileName) { 
        FileType.Type type = FileType.Type.UNKNOWN
        if (fileName.contains("fastq")) {
            type = FileType.Type.SEQUENCE
        } else if (fileName.contains("align")) {
            type = FileType.Type.ALIGNMENT
        }
        return type
    }

    private File openTextFile(MetaDataFile file) {
        String path = metaDataFileService.initialLocation(file)
        File mdFile = new File(path)
        if (!mdFile.canRead()) {
            throw new FileNotReadableException(mdFile.path)
        }
        return mdFile
    }

    /**
     * This method tokenizes a string
     *
     * Standard java method can not be used because two subsequent tabs
     * shall return empty token. The tokens are stripped from quotation
     * marks
     *
     * @param line - input string
     * @param tab - separator (typically '\t')
     * @return - array of strings
     */
    private List<String> tokenize(String line, String tab) {
        List<String> tokens = []
        int idx = 0
        for (int i=0; i < line.length(); i++) {
            if ((line.charAt(i) == tab) || (i == line.length() - 1)) {
                int end = ((i == line.length() - 1) ? i + 1 : i)
                String token = line.substring(idx, end)
                token = token.replaceAll('\"', '')
                token = token.replaceAll(tab, '')
                tokens << token
                idx = i + 1
            }
        }
        return tokens
    }

    // TODO: Add comment
    private List<MetaDataKey> getKeysFromTokens(List tokens) {
        List<MetaDataKey> keys = []
        tokens.each { mdKey ->
            String token = correctedKey(mdKey)
            MetaDataKey key = MetaDataKey.findByName(token)
            if (!key) {
                key = new MetaDataKey(name: token)
                key.save(flush: true)
            }
            keys << key
        }
        keys
    }

    /**
    * This function loops oven keys and values and attache them
    * as MetaDataEntries to DataFile object
    * @param DataFile
    * @param keys
    * @param values
    * @return
    */
    private addMetaDataEntries(DataFile dataFile, List<MetaDataKey> keys, List<String> values) {
       for (int i=0; i<keys.size(); i++) {
           MetaDataKey key = keys.getAt(i)
           MetaDataEntry entry = new MetaDataEntry (
               value: values.getAt(i) ? values.getAt(i).trim() : "",
               source: MetaDataEntry.Source.MDFILE,
               key: key
           )
           entry.dataFile = dataFile
           entry.save(flush: true)
       }
    }

    /**
     * Assign file name for a specific DataFile object
     *
     * The file name is in either FASTQ_FILE or ALIGN_FILE
     * column of meta-data.
     * this function also fills MD5 check sum field from meta-data
     *
     * @param dataFile
     */
    private void assignFileName(DataFile dataFile) {
        def keyNames = ["FASTQ_FILE", "ALIGN_FILE"]
        keyNames.each {
            MetaDataEntry entry = getMetaDataEntry(dataFile, it)
            if (!entry) {
                return
            }
            // remove heading sequence center name and run name
            String value = entry.value
            // sequence center name
            String dirName = dataFile.run.seqCenter.dirName
            if (value.startsWith(dirName)) {
                int idx = value.indexOf("/")
                value = value.substring(idx+1)
            }
            // run name
            if (value.startsWith(dataFile.run.name) ||
                    value.startsWith("run" + dataFile.run.name)) {
                int idx = value.indexOf("/")
                value = value.substring(idx+1)
            }
            // split into file name nand path (important for solid)
            int idx = value.lastIndexOf("/")
            dataFile.pathName = (idx == -1) ? "" : value.substring(0, idx)
            dataFile.fileName = value.substring(idx+1) ? value.substring(idx+1) : "error"
        }
        if (!dataFile.fileName) {
            throw new FileNameNotDefinedException(dataFile.run.name)
        }
    }

    /**
     *
     * @param dataFile
     * @return
     */
    private void fillVbpFileName(DataFile dataFile) {
        if (needsVBPNameChange(dataFile)) {
            String lane = metaDataValue(dataFile, "LANE_NO")
            String readId = readStringFormFileName(dataFile.fileName)
            String name =  "s_" + lane + "_" + readId + "_sequence.txt.gz"
            dataFile.vbpFileName = name
            log.debug("${dataFile.fileName} ${dataFile.vbpFileName}")
        } else {
            dataFile.vbpFileName = dataFile.fileName
        }
    }

    private boolean needsVBPNameChange(DataFile dataFile) {
        if (!dataFile.run.seqPlatform.name.contains("Illumina")) {
            return false
        }
        if (!dataFile.fileName.contains("fastq.gz")) {
            return false
        }
        if (!dataFile.fileName.contains("read")) {
            return false
        }
        return true
    }
 
    private String readStringFormFileName(String fileName) {
        String readId = "0"
        if (fileName.contains("read1")) {
            readId = "1"
        } else if (fileName.contains("read2")) {
            readId = "2"
        }
        return readId
    }

    /**
     *
     * @param dataFile
     */
    private void fillMD5Sum(DataFile dataFile) {
        MetaDataEntry entry = getMetaDataEntry(dataFile, "MD5")
        dataFile.md5sum = entry?.value
    }

    /**
     *
     * Mark file, if the file is withdrawn
     *
     * @param dataFile
     */
    private void checkIfWithdrawn(DataFile dataFile) {
        MetaDataEntry entry = getMetaDataEntry(dataFile, "WITHDRAWN")
        dataFile.fileWithdrawn = (entry?.value == "1")
    }

    /**
     * Returns a meta data entry belonging the a given data file
     * with a key specified by the input parameter
     *
     * @param file
     * @param key
     * @return
     */
    private MetaDataEntry getMetaDataEntry(DataFile file, MetaDataKey key) {
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }

    /**
     * Returns a meta data entry belonging the a given data file
     * with a key specified by the input parameter
     *
     * @param file
     * @param key
     * @return
     */
    private MetaDataEntry getMetaDataEntry(DataFile file, String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }

    private String metaDataValue(DataFile file, String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(file, key)
        return entry.value
    }

    /**
     * Assign a file type object to e given DataFile
     * the assignment is based on two sources of information:
     * file name and if the file comes from "fastq" or "align"
     * meta-data file
     *
     * @param dataFile
     * @param type
     */
    private void assignFileType(DataFile dataFile, FileType.Type type) {
        FileType fileType = fileTypeService.getFileType(dataFile.fileName, type)
        dataFile.fileType = fileType
        dataFile.save(flush: true)
    }

    /**
     *
     * In old meta-data, sequencing type was not included in meta-data
     * in this case the sequencing type is created by the system
     * from directory name
     *
     * @param dataFile
     */
    private void addKnownMissingMetaData(DataFile dataFile) {
        final String keyName = "SEQUENCING_TYPE"
        MetaDataKey key = MetaDataKey.findByName(keyName)
        if (!key) {
            key = new MetaDataKey(name: keyName)
            key.save(flush: true)
        }
        MetaDataEntry entry = getMetaDataEntry(dataFile, keyName)
        if (entry) {
            return
        }
        RunSegment initialPath = dataFile.runSegment
        List<SeqType> types = SeqType.list()
        for (SeqType type in types) {
            if (initialPath.mdPath.contains(type.dirName)) {
                String value = type.name
                log.debug("\tassiginig to ${value}")
                entry = new MetaDataEntry (
                    value: value,
                    key: key,
                    source: MetaDataEntry.Source.SYSTEM,
                    dataFile: dataFile
                )
                entry.save(flush: true)
                return
            }
        }
    }

    /**
     * Solve known bugs
     *
     * For example meta-data column which shall be "LANE_NO"
     * for some runs is "lane". The solid standard "SLIDE_NO"
     * is changed to "LANE_NO" to unify key for later processing
     *
     * @param token
     * @return
     */
    private String correctedKey(String token) {
        if (token == "lane" || token == "SLIDE_NO") {
            return "LANE_NO"
        }
        return token
    }

    /**
     * return project for a given dataFile or null if a dataFile 
     * does not belong to a specific project (eg. metadata file)
     * @param dataFile
     * @return
     */
    private Project getProjectForDataFile(DataFile dataFile) {
        if (!dataFile.used) {
            return null
        }
        if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
            return dataFile.seqTrack.sample.individual.project
        }
        if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
            return dataFile.alignmentLog.seqTrack.sample.individual.project
        }
        return null
    }

    /**
     * Checks if given run is already assigned to a given project
     * if not a new object is created
     * 
     * @param run
     * @param project
     */
    private void assignRunToProject(Run run, Project project) {
        RunByProject runByProject = RunByProject.findByRunAndProject(run, project)
        if (!runByProject) {
            runByProject = new RunByProject(project: project, run: run)
            runByProject.save(flush: true)
        }
    }
}
