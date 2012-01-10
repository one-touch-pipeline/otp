package de.dkfz.tbi.otp.ngsdata

import java.text.SimpleDateFormat
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import de.dkfz.tbi.otp.job.processing.ProcessingException

class MetaDataService {
    /**
     * Loading Meta data is not thread save - use a lock for it
     */
    private final Lock loadMetaDataLock = new ReentrantLock()
    /**
     * Validating Meta Data is not thread save = use a lock for it
     */
    private final Lock validateMetaDataLock = new ReentrantLock()

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
        if (!run) {
            return
        }

        log.debug("registering run ${run.name} from ${run.seqCenter}")

        String runDir = run.mdPath + "/run" + run.name
        File dir = new File(runDir)
        if (!dir.canRead() || !dir.isDirectory()) {
            // TODO Ask Sylwester
            throw new ProcessingException("not readable directory ${dir}")
        }
        List<String> fileNames = dir.list()
        FileType fileType = FileType.findByType(FileType.Type.METADATA)
        DataFile dataFile
        fileNames.each { String fileName ->
            if (fileName.contains("wrong")) {
                return
            }
            if (fileName.contains("fastq") || fileName.contains("align")) {
                dataFile = new DataFile(
                        pathName: runDir,
                        fileName: fileName
                        )
                dataFile.run = run
                dataFile.fileType = fileType
                dataFile.save()
            }
        }
        fileType.save()
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
            DataFile dataFile
            listOfMDFiles.each { DataFile file ->
                if (file.fileType.type != FileType.Type.METADATA || file.used) {
                    return
                }
                log.debug("\tfound md souce file ${file.fileName}")
                // hint to determine file type
                FileType.Type type = FileType.Type.UNKNOWN
                if (file.fileName.contains("fastq")) {
                    type = FileType.Type.SEQUENCE
                } else if (file.fileName.contains("align")) {
                    type = FileType.Type.ALIGNMENT
                }
                File mdFile = new File(file.pathName + File.separatorChar + file.fileName)
                if (!mdFile.canRead()) {
                    log.debug("\tcan not read ${file.fileName}")
                    return
                }
                List<String> tokens
                List<String> values
                List<MetaDataKey> keys
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
                        dataFile.run = run
                        dataFile.save()
                        values = tokenize(line, '\t')
                        for (int i=0; i<keys.size(); i++) {
                            MetaDataKey key = keys.getAt(i)
                            MetaDataEntry entry = new MetaDataEntry (
                                    value: values.getAt(i) ? values.getAt(i) : "",
                                    source: MetaDataEntry.Source.MDFILE,
                                    key: key
                                    )
                            entry.dataFile = dataFile
                            entry.save()
                        }
                        // fill-up important fields
                        assignFileName(dataFile)
                        fillVbpFileName(dataFile)
                        fillMD5Sum(dataFile)
                        assignFileType(dataFile, type)
                        addKnownMissingMetaData(run, dataFile)
                        checkIfWithdrawn(dataFile)
                    }
                }
                file.used = true
                file.save()
                run.save()
            }
        } finally {
            loadMetaDataLock.unlock()
        }
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
                key.save()
            }
            keys << key
        }
        keys
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
            dataFile.fileName = "errorNoHeader"
            dataFile.pathName = "errorNoHeader"
            MetaDataEntry.findAllByDataFile(dataFile).each { println "${it.key} ${it.value}" }
        }
    }

    /**
     *
     * @param dataFile
     * @return
     */
    private void fillVbpFileName(DataFile dataFile) {
        if (dataFile.run.seqTech.name.contains("illumina") &&
                dataFile.fileName.contains("fastq.gz")) {
            String lane = getMetaDataEntry(dataFile, "LANE_NO")
            String readId = "0"
            if (dataFile.fileName.contains("read1")) {
                readId = "1"
            } else if (dataFile.fileName.contains("read2")) {
                readId = "2"
            }
            String name =  "s_" + lane + "_" + readId + "_sequence.txt.gz"
            dataFile.vbpFileName = name
            log.debug("${dataFile.fileName} ${dataFile.vbpFileName}")
        } else {
            dataFile.vbpFileName = dataFile.fileName
        }
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
        getMetaDataEntry(file, key.name)
    }

    /**
     * Returns a meta data entry belonging the a given data file
     * with a key specified by the input parameter
     *
     * @param file
     * @param key
     * @return
     */
    private MetaDataEntry getMetaDataEntry(DataFile file, String key) {
        // TODO: optimize
        MetaDataEntry entry = null
        MetaDataEntry.findAllByDataFile(file).each { MetaDataEntry iEntry ->
            if (iEntry.key.name == key) {
                entry = iEntry
            }
        }
        return entry
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
        fileType.save()
        dataFile.fileType = fileType
        dataFile.save()
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
            key.save()
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
                entry.save()
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
                    if (entry.status == invalid) {
                        log.debug("invalid md entry ${entry.key}\t${entry.value}")
                        dataFile.metaDataValid = false
                        allValid = false
                    }
                    entry.save()
                }
            }
        } finally {
            validateMetaDataLock.unlock()
        }
        run.save()
        return allValid
    }

    /**
     * This method tries to assign execution data for a run
     *
     * this method knows different standards of encoding data
     * in meta-data. If there is no MetaDataEntry with "RUN_DATE"
     * key, the run date is build from run name. The method
     * knows Solid and Illumina run naming standards.
     *
     * @param runId - database ID of Run object
     */
    void buildExecutionDate(long runId) {
        Run run = Run.get(runId)
        Date exDate = null
        boolean consistant = true
        DataFile.findAllByRun(run).each { DataFile dataFile ->
            // TODO: optimize
            MetaDataEntry entry = MetaDataEntry.findAllByDataFile(dataFile).find { it.key =="RUN_DATE" }
            if (!entry) {
                return
            }
            Date date
            SimpleDateFormat simpleDateFormat
            try {
                // best effort to interpret date
                // TODO: magic numbers are magic
                if (entry.value.size() == 6) {
                    simpleDateFormat = new SimpleDateFormat("yyMMdd")
                    date = simpleDateFormat.parse(entry.value)
                } else if (entry.value.size() == 10) {
                    simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
                    date = simpleDateFormat.parse(entry.value)
                }
            } catch (Exception e) {
                // no exception catching, 
                // if parsing fails, (date == null)
            }
            if (!exDate) {
                exDate = date
            }
            if (exDate && !exDate.equals(date)) {
                consistant = false
            }
            dataFile.dateExecuted = date
        }
        // fill if all files have the same executions date
        if (exDate && consistant) {
            run.dateExecuted = exDate
        }
        // date from the runName
        if (!run.dateExecuted) {
            SimpleDateFormat simpleDateFormat
            if (run.seqTech.name == "illumina") {
                try {
                    String subname = run.name.substring(0, 6)
                    simpleDateFormat = new SimpleDateFormat("yyMMdd")
                    run.dateExecuted = simpleDateFormat.parse(subname)
                } catch (Exception e) {
                    // TODO: Appropriate exception
                }
            } else if (run.seqTech.name == "solid") {
                try {
                    String subname = run.name.substring(10, 18)
                    simpleDateFormat = new SimpleDateFormat("yyyyMMdd")
                    run.dateExecuted = simpleDateFormat.parse(subname)
                } catch (Exception e) {
                    // TODO: Appropriate exception
                }
            }
        }
        run.save()
    }

    /**
     * Each sequence and alignment file belonging to a run is 
     * assigned to a project based on the Sample object.
     * @param runId
     */
    void assignFilesToProjects(long runId) {
        Run run = Run.get(runId)
        DataFile.findAllByRun(run).each { DataFile dataFile ->
            if (!dataFile.used) {
                return
            }
            if (dataFile.fileType.type == FileType.Type.METADATA) {
                return
            }
            if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
                Project project = dataFile.seqTrack.sample.individual.project
                RunByProject runProject = new RunByProject(project: project, run: run)
                runProject.save()
                dataFile.project = project
                dataFile.save()
            } else if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
                Project project = dataFile.alignmentLog.seqTrack.sample.individual.project
                RunByProject runProject = new RunByProject(project: project, run: run)
                runProject.save()
                dataFile.project = project
                dataFile.save()
            }
        }
        run.save()
    }
}
