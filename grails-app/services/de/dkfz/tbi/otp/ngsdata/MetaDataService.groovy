package de.dkfz.tbi.otp.ngsdata

import java.text.SimpleDateFormat
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import de.dkfz.tbi.otp.job.processing.ProcessingException

class MetaDataService {

    // locks for operation that are not tread safe
    private final Lock loadMetaDataLock = new ReentrantLock()


    /**
     * Dependency injection of file type service
     */
    def fileTypeService

    static transactional = true

    /**
     *
     * looks into directory pointed by mdPath of a Run object
     * and register files that could be meta-data files
     *
     * @param runId - database id or the Run object
     */
    void registerInputFiles(long runId) {

        Run run = Run.get(runId)
        log.debug("registering run ${run.name} from ${run.seqCenter}")

        String runDir = run.mdPath + "/run" + run.name
        File dir = new File(runDir)
        if (!dir.canRead() || !dir.isDirectory()) {
            throw new DirectoryNotReadableException(dir)
        }
        List<String> fileNames = dir.list()
        FileType fileType = FileType.findByType(FileType.Type.METADATA)
        DataFile dataFile
        fileNames.each { String fileName ->
            if (fileName.contains("wrong")) {
                return
            }
            if (fileName.contains("fastq") || fileName.contains("align")) {
                if (fileRegistered(run, fileName)) {
                    throw new MetaDataFileDuplicationException(run.name, fileName)
                }
                dataFile = new DataFile(
                    pathName: runDir,
                    fileName: fileName
                )
                dataFile.run = run
                dataFile.fileType = fileType
                dataFile.save(flush: true)
            }
        }
        fileType.save(flush: true)
    }

    /**
     * Check if given meta data file was already registered
     * @param run
     * @param fileName
     * @return
     */
    private boolean fileRegistered(Run run, String fileName) {
        DataFile file = DataFile.findByRunAndFileName(run, fileName)
        return (boolean)file
    }

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
            List<DataFile> listOfMDFiles = DataFile.findAllByRun(run)
            listOfMDFiles.each { DataFile file ->
                if (!isNewMetaDataFile(file)) {
                    return
                }
                log.debug("\tfound md souce file ${file.fileName}")
                processMetaDataFile(file)
                run.save(flush: true)
            }
        } finally {
            loadMetaDataLock.unlock()
        }
    }

    private boolean isNewMetaDataFile(DataFile file) {
        return (file.fileType.type == FileType.Type.METADATA && !file.used)
    }

    private void processMetaDataFile(DataFile file) {
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
                dataFile = new DataFile() // set-up later
                dataFile.run = file.run
                dataFile.save(flush: true)
                values = tokenize(line, '\t')
                addMetaDataEntries(dataFile, keys, values)
                // fill-up important fields
                assignFileName(dataFile)
                fillVbpFileName(dataFile)
                fillMD5Sum(dataFile)
                assignFileType(dataFile, type)
                addKnownMissingMetaData(file.run, dataFile)
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

    private File openTextFile(DataFile file) {
        File mdFile = new File(file.pathName + File.separatorChar + file.fileName)
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
               value: values.getAt(i) ? values.getAt(i) : "",
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
            String lane = getMetaDataEntry(dataFile, "LANE_NO")
            String readId = readStringFormFileName(dataFile.fileName)
            String name =  "s_" + lane + "_" + readId + "_sequence.txt.gz"
            dataFile.vbpFileName = name
            log.debug("${dataFile.fileName} ${dataFile.vbpFileName}")
        } else {
            dataFile.vbpFileName = dataFile.fileName
        }
    }

    private boolean needsVBPNameChange(DataFile dataFile) {
        if (!dataFile.run.seqTech.name.contains("illumina")) {
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
     * @param run
     * @param dataFile
     */
    private void addKnownMissingMetaData(Run run, DataFile dataFile) {
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
        List<SeqType> types = SeqType.list()
        for (int iType = 0; iType < types.size(); iType++) {
            if (run.mdPath.contains(types[iType].dirName)) {
                String value = types[iType].name
                log.debug("\tassiginig to ${value}")
                entry = new MetaDataEntry (
                        value: value,
                        source: MetaDataEntry.Source.SYSTEM,
                        key: key
                        )
                entry.dataFile = dataFile
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
     * Checks if values of MetaDataEntry table are correct
     *
     * the following keys are checked
     * RunID, sample identifier, sequencing center
     * sequencing type and library layout
     *
     * @param runId
     */
    boolean validateMetadata(long runId) {
        Run run = Run.get(runId)
        boolean allValid = true
        validateMetaDataLock.lock()
        try {
            DataFile.findAllByRun(run).each { DataFile dataFile ->
                dataFile.metaDataValid = true
                MetaDataEntry.findAllByDataFile(dataFile).each { MetaDataEntry entry ->
                    boolean isValid = validateMetaDataEntry(run, entry)
                    if (!isValid) {
                        dataFile.metaDataValid = false
                        allValid = false
                    }
                }
                dataFile.save()
            }
        } finally {
            validateMetaDataLock.unlock()
        }
        run.save(flush: true)
        return allValid
    }

    private boolean validateMetaDataEntry(Run run, MetaDataEntry entry) {
        MetaDataEntry.Status valid = MetaDataEntry.Status.VALID
        MetaDataEntry.Status invalid = MetaDataEntry.Status.INVALID
        switch(entry.key.name) {
            case "RUN_ID":
                entry.status = (run.name == entry.value) ? valid : invalid
                break
            case "SAMPLE_ID":
                SampleIdentifier sample = SampleIdentifier.findByName(entry.value)
                entry.status = (sample != null) ? valid : invalid
                break
            case "CENTER_NAME":
                entry.status = invalid
                SeqCenter center = run.seqCenter
                if (center.dirName == entry.value.toLowerCase()) {
                    entry.status = valid
                } else if (center.name == entry.value) {
                    entry.status = valid
                }
                break
            case "SEQUENCING_TYPE":
                SeqType seqType = SeqType.findByName(entry.value)
                entry.status = (seqType != null) ? valid : invalid
                break
            case "LIBRARY_LAYOUT":
                SeqType seqType = SeqType.findByLibraryLayout(entry.value)
                entry.status = (seqType != null) ? valid : invalid
                break
        }
        entry.save(flush: true)
        return (entry.status == invalid)? false : true
    }

    /**
     * Each sequence and alignment file belonging to a run is 
     * assigned to a project based on the Sample object.
     * @param runId
     */
    private void assignFilesToProjects(long runId) {
        Run run = Run.get(runId)
        DataFile.findAllByRun(run).each { DataFile dataFile ->
            Project projct = getProjectForDataFile(dataFile)
            if (!project) {
                return // continue
            }
            dataFile.project = project
            dataFile.save(flush: true)
            assignRunToProject(run, project)
        }
        run.save(flush: true)
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
        if (dataFile.fileType.type == FileType.Type.METADATA) {
            return null
        }
        if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
            return dataFile.seqTrack.sample.individual.project
        }
        if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
            return dataFile.alignmentLog.seqTrack.sample.individual.project
        }
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
