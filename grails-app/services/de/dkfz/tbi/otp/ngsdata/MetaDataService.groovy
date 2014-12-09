package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PreFilter
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.utils.ReferencedClass

class MetaDataService {


    // locks for operation that are not tread safe
    private final Lock loadMetaDataLock = new ReentrantLock()

    /**
     * Dependency injection of file type service
     */
    def fileTypeService
    def metaDataFileService
    ExomeEnrichmentKitService exomeEnrichmentKitService

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

    /**
     * Retries a MetaDataEntry in an ACL aware manner.
     * @param id The id of the MetaDataEntry to retrieve
     * @return The MetaDataEntry if present, otherwise null
     */
    @PostAuthorize("(returnObject == null) or ((returnObject.dataFile.project != null) and hasPermission(returnObject.dataFile.project?.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)) or hasRole('ROLE_OPERATOR')")
    MetaDataEntry getMetaDataEntryById(Long id) {
        return MetaDataEntry.get(id)
    }

    /**
     * Updates the given MetaDataEntry's value to the new given value.
     * Creates a ChangeLog entry for this update.
     * @param entry The MetaDataEntry to update
     * @param value The new value to set
     * @throws ChangelogException In case the Changelog Entry could not be created
     * @throws MetaDataEntryUpdateException In case the MetaDataEntry could not be updated
     */
    @PreAuthorize("((#entry.dataFile.project != null) and hasPermission(#entry.dataFile.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', write)) or hasRole('ROLE_OPERATOR')")
    boolean updateMetaDataEntry(MetaDataEntry entry, String value) throws ChangelogException, MetaDataEntryUpdateException {
        ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(MetaDataEntry.class.getName())
        ChangeLog changelog = new ChangeLog(rowId: entry.id, referencedClass: clazz, columnName: "value", fromValue: entry.value, toValue: value, comment: "-", source: ChangeLog.Source.MANUAL)
        if (!changelog.save()) {
            throw new ChangelogException("Creation of changelog failed, errors: " + changelog.errors.toString())
        }
        entry.value = value
        if (!entry.save(flush: true)) {
            throw new MetaDataEntryUpdateException(entry)
        }
        return true
    }

    /**
     * Checks for the list of given Meta Data Entries whether there exists at least one ChangeLog element.
     * A map is created with the MetaDataEntry as key and a boolean as value. True means there is at least
     * one ChangeLog entry, false means there is none.
     *
     * @param entries The MetaDataEntries for which it should be checked whether there is a ChangeLog
     * @return Map of MetaDataEntries with boolean information as value whether there is a ChangeLog
     */
    @PreFilter("((filterObject.dataFile.project != null) and hasPermission(filterObject.dataFile.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', 'read')) or ((filterObject.dataFile.run != null) and hasPermission(filterObject.dataFile.run.seqCenter.id, 'de.dkfz.tbi.otp.ngsdata.SeqCenter', 'read')) or hasRole('ROLE_OPERATOR')")
    Map<MetaDataEntry, Boolean> checkForChangelog(List<MetaDataEntry> entries) {
        ReferencedClass clazz = ReferencedClass.findByClassName(MetaDataEntry.class.getName())
        if (!clazz) {
            Map<MetaDataEntry, Boolean> results = [:]
            entries.each { MetaDataEntry entry ->
                results.put(entry, false)
            }
            return results
        }
        List<ChangeLog> changelogs = ChangeLog.findAllByRowIdInListAndReferencedClass(entries.collect { it.id }, clazz)
        Map<MetaDataEntry, Boolean> results = [:]
        entries.each { MetaDataEntry entry ->
            results.put(entry, changelogs.find { it.rowId == entry.id } ? true : false)
        }
        return results
    }

    /**
     * Retrieves the ChangeLog for the given MetaDataEntry.
     *
     * @param entry The MetaDataEntry for which the ChangeLog should be retrieved
     * @return List of ChangeLog entries
     */
    @PreAuthorize("((#entry.dataFile.project != null) and hasPermission(#entry.dataFile.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)) or hasRole('ROLE_OPERATOR')")
    List<ChangeLog> retrieveChangeLog(MetaDataEntry entry) {
        ReferencedClass clazz = ReferencedClass.findByClassName(MetaDataEntry.class.getName())
        if (!clazz) {
            return []
        }
        return ChangeLog.findAllByRowIdAndReferencedClass(entry.id, clazz)
    }

    /**
     * Retrieves the DataFile identified by the given ID in an ACL aware manner.
     * @param id The Id of the DataFile.
     * @return DataFile if it exists, otherwise null
     */
    @PostAuthorize("(returnObject == null) or ((returnObject.project != null) and hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', 'read')) or ((returnObject.run != null) and hasPermission(returnObject.run.seqCenter.id, 'de.dkfz.tbi.otp.ngsdata.SeqCenter', 'read')) or hasRole('ROLE_OPERATOR')")
    DataFile getDataFile(Long id) {
        return DataFile.get(id)
    }

    private void processMetaDataFiles(Run run) {
        List<RunSegment> paths = RunSegment.findAllByRunAndMetaDataStatus(run, RunSegment.Status.PROCESSING)
        List<MetaDataFile> mdFiles = MetaDataFile.findAllByRunSegmentInList(paths)
        mdFiles.each { MetaDataFile file ->
            if (file.used) {
                return
            }
            log.debug("\tfound md source file ${file.fileName}")
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
            } else if (line.trim().isEmpty()) {
                return //skip empty lines
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
                addReadNumber(dataFile, type)
                assignFileType(dataFile, type)
                createSeqTypeMetaDataEntryFromDirNameIfNeeded(dataFile)
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
            String token = correctedKey(mdKey).trim()
            MetaDataKey key = MetaDataKey.findByName(token)
            if (!key) {
                key = new MetaDataKey(name: token)
                key.save(flush: true)
            }
            keys << key
        }
        //assertAllNecessaryKeysExist(keys)
        keys
    }

    /**
     * Checks, if all necessary columns are provided
     */
    private void assertAllNecessaryKeysExist(List<MetaDataKey> metaDataKey) {
        List<String> stringKeys = metaDataKey*.name
        List<String> missedKeys = []
        MetaDataColumn.values().each {
            if (!stringKeys.contains(it.name())) {
                missedKeys << it.name()
            }
        }
        if (missedKeys) {
            throw new ProcessingException("The following keys are missed in the metadata file: ${missedKeys}")
        }
    }

    /**
     * This function loops oven keys and values and attache them
     * as MetaDataEntries to DataFile object
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

    private void fillMD5Sum(DataFile dataFile) {
        MetaDataEntry entry = getMetaDataEntry(dataFile, "MD5")
        dataFile.md5sum = entry?.value
    }

    /**
     *
     * Mark file, if the file is withdrawn
     */
    private void checkIfWithdrawn(DataFile dataFile) {
        MetaDataEntry entry = getMetaDataEntry(dataFile, "WITHDRAWN")
        dataFile.fileWithdrawn = (entry?.value == "1")
    }

    /**
     * Returns a meta data entry belonging the a given data file
     * with a key specified by the input parameter
     */
    private MetaDataEntry getMetaDataEntry(DataFile file, MetaDataKey key) {
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }

    /**
     * Returns a meta data entry belonging the a given data file
     * with a key specified by the input parameter
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
     * Assign a file type object to a given DataFile
     * the assignment is based on two sources of information:
     * file name and if the file comes from "fastq" or "align"
     * meta-data file
     */
    private void assignFileType(DataFile dataFile, FileType.Type type) {
        FileType fileType = fileTypeService.getFileType(dataFile.fileName, type)
        dataFile.fileType = fileType
        dataFile.save(flush: true)
    }

    private String getLibraryLayoutFromMetadata(DataFile dataFile) {
        return metaDataValue(dataFile, MetaDataColumn.LIBRARY_LAYOUT.name())
    }


    private void addReadNumber(DataFile dataFile, FileType.Type type) {
        FileType fileType = fileTypeService.getFileType(dataFile.fileName, type)
        if (fileType.type == FileType.Type.SEQUENCE && fileType.vbpPath == "/sequence/") {
            String fileName = dataFile.fileName
            String libraryLayout = getLibraryLayoutFromMetadata(dataFile)
            boolean isSingle = libraryLayout == "SINGLE"
            dataFile.readNumber = findOutReadNumberIfSingleEndOrByFileName(fileName, isSingle)
            assert dataFile.save(flush: true)
        }
    }

    /**
     * In old meta-data, sequencing type was not included in meta-data
     * in this case the sequencing type is created by the system
     * from directory name
     */
    private void createSeqTypeMetaDataEntryFromDirNameIfNeeded(DataFile dataFile) {
        notNull(dataFile, "param dataFile in MetaDataService.addKnownMissingMetaData() is NULL")

        MetaDataKey key = getOrCreateKey(MetaDataColumn.SEQUENCING_TYPE.name())

        MetaDataEntry entry = getMetaDataEntry(dataFile, MetaDataColumn.SEQUENCING_TYPE.name())
        if (entry) {
            return
        }
        RunSegment initialPath = dataFile.runSegment
        for (SeqType seqType in SeqType.list()) {
            if (initialPath.mdPath.contains(seqType.dirName)) {
                String value = seqType.name
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
     * After this call, it is ensured that the specified keyName exists in the Database.
     */
    MetaDataKey getOrCreateKey(String keyName) {
        notNull(keyName, "input keyName in MetaDataService.getOrCreateKey() is NULL")

        MetaDataKey key = MetaDataKey.findByName(keyName)
        if (!key) {
            key = new MetaDataKey(name: keyName)
            key.save(flush: true)
        }

        return key
    }

    /**
     * Solve known bugs
     *
     * For example meta-data column which shall be "LANE_NO"
     * for some runs is "lane". The solid standard "SLIDE_NO"
     * is changed to "LANE_NO" to unify key for later processing
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
     */
    private Project getProjectForDataFile(DataFile dataFile) {
        if (!dataFile.used) {
            return null
        }
        if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
            return dataFile.seqTrack.project
        }
        if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
            return dataFile.alignmentLog.seqTrack.project
        }
        return null
    }

    /**
     * Checks if given run is already assigned to a given project
     * if not a new object is created
     */
    private void assignRunToProject(Run run, Project project) {
        RunByProject runByProject = RunByProject.findByRunAndProject(run, project)
        if (!runByProject) {
            runByProject = new RunByProject(project: project, run: run)
            runByProject.save(flush: true)
        }
    }

    /**
     * Sometimes information for old lanes can be inferred from new lanes.
     * This method is the hook point where the specific inferences can be called.
     */
    public void enrichOldDataWithNewInformationFrom(Run run) {
        notNull(run, "The input of the method enrichOldDataWithNewInformationFrom is null")

        List<RunSegment> runSegments = RunSegment.findAllByRun(run)
        runSegments.each { RunSegment runSegment ->
            exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
        }
    }

    // such method exists because of the current desing of meta data workflow
    // (dataFile.fileType is set before dataFile.seqType).
    // the method is to keep findOutReadNumber(..) clean.
    /**
     * tries to find out read number of fastq files but returns 1 if the library layout is single end.
     * @param dataFileName name of fastq file
     * @param isSingleEnd is true if the library layout is single end, otherwise - false
     * @return read number, 1 or 2
     * @throws RuntimeException if the read number cannot be found.
     */
    private static int findOutReadNumberIfSingleEndOrByFileName(String dataFileName, boolean isSingleEnd) {
	if (isSingleEnd) {
	    return 1
	} else {
	    return findOutReadNumber(dataFileName)
	}
    }

    /**
     * tries to find out read number of a fastq file.
     * @param dataFileName name of fastq file
     * @return read number, 1 or 2
     * @throws RuntimeException if the read number cannot be found.
     */
    private static int findOutReadNumber(String dataFileName) {
	assert dataFileName, "for non single-read fastq files, file name must be provided"
        def patterns = [
            //SOMEPID_L001_R2.fastq.gz
            [regExpr: /.+_L00\d{1,2}.R([12]).+/, readGroupNumber: 1],
            //s_101202_7_1.fastq.gz
            //s_101202_7_2.fastq.gz
            //s_110421_3.read2.fastq.gz
            //s_3_1_sequence.txt.gz
            //s_140304_3_001_2_sequence.txt.gz
            //s_111201_2a_1_sequence.txt.gz
            //SOMEPID_s_6_1_sequence.txt.gz
            //SOMEPID_s_3_1_sequence.txt.gz
            [regExpr: /.*s(_\d{6})?_\d{1,2}([a-z])?(_|\.read|_\d{3}_)([12]).+/, readGroupNumber: 4],
            //AB-1234_CDE_EFGH_091_lib14837_1189_7_1.fastq.tar.bz
            //AB-1234_5647_lib12345_1_sequence.fastq.bz2
            //CD-2345_6789_lib234567_7890_1.fastq.bz2
            [regExpr: /^[A-Z]{2}-\d{4}_.+_lib\d{5}(_\d{4})?(_\d{1,2})?_([12])(_sequence)?\.fastq.+/, readGroupNumber: 3],
            //NB_E_789_R.1.fastq.gz
            //NB_E_789_R.2.fastq.gz
            //NB_E_234_R5.2.fastq.gz
            //NB_E_345_T1S.2.fastq.gz
            //NB_E_456_O_lane5.2.fastq.gz
            [regExpr: /^NB_E_\d{3}_[A-Z0-9]{1,3}(_lane\d)?\.([12])\.fastq.+/, readGroupNumber: 2],
            //00_MCF10A_GHI_JKL_WGBS_I.A34002.137487.C2RT2ACXX.1.1.fastq.gz
            //00_MCF10A_GHI_JKL_H3K4me1_I.IX1239-A26685-ACTTGA.134224.D2B0LACXX.2.1.fastq.gz
            [regExpr: /^00_MCF10A.+\.\d{6}\.[A-Z0-9]{9}\.\d{1,2}\.([12])\.fastq.+/, readGroupNumber: 1],
            //
            [regExpr: /^RB\d{1,2}_(Blut|Tumor)_R([12])\.fastq.+/, readGroupNumber: 2],
            //P021_WXYZ_L1_Rep3_2.fastq.gz
            //H019_ASDF_L1_lib54321_1.fastq.gz
            //FE-0100_H021_WXYZ_L1_5_1.fastq.gz
            [regExpr: /.*[HP]\d\d[A-Z0-9]_[A-Z0-9]{4}_L\d_.+_([12])\.fastq.+/, readGroupNumber: 1],
            //lane6mp25PE2_2_sequence.txt.gz
            //lane211s003107_1_sequence.txt.gz
            //lane8wwmp44PE2_1_sequence.txt.gz
            //SOMEPID_lane511s003237_1_sequence.txt.gz
            [regExpr: /.*[Ll]ane\d.+_([12])_sequence.txt.gz$/, readGroupNumber: 1],
            //180824_I234_ABCDEFGHIJK_L5_WHAIPI000042-43_2.raw.fq.gz
            [regExpr: /^\d{6}_I\d{3}_[A-Z0-9]{11}_L\d_WHAIPI\d{6}-\d{2}(\+1)?_([12]).raw.fq.gz$/, readGroupNumber: 2]
        ]

        def readNumbers = patterns.collect { pattern ->
            def matches = dataFileName =~ pattern.regExpr
            matches ? matches[0][pattern.readGroupNumber] : null
        }.findAll { it }
        if (!readNumbers) {
	    throw new RuntimeException("cannot find readNumber for $dataFileName")
	}
        assert readNumbers.size() == 1, "$dataFileName matches to more then one pattern"
        return readNumbers.first() as int // without the "as" the conversion is wrong
    }
}
