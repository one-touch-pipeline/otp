package de.dkfz.tbi.otp.ngsdata

class MetaDataService {

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

        println "registering run ${run.name} from ${run.seqCenter}"

        String runDir = run.mdPath + "/run" + run.name
        File dir = new File(runDir)

        if (!dir.canRead() || !dir.isDirectory()) {
            println "not readable directory ${dir}"
            return
        }

        def fileNames = dir.list() 

        FileType fileType = FileType.findByType(FileType.Type.METADATA)
        DataFile dataFile
        fileNames.each {

            if (it.count("wrong")) return
            if (it.count("fastq") > 0 || it.count("align") > 0) {

                dataFile = new DataFile(
                    pathName: runDir, 
                    fileName: it		
                )
                run.addToDataFiles(dataFile)
                fileType.addToDataFiles(dataFile)

                safeSave(run)
                //safeSave(dataFile)
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

        println "loading metadata for run ${run.name}"

        def listOfMDFiles = []
        run.dataFiles.each {listOfMDFiles << it}


        DataFile dataFile
        listOfMDFiles.each {

            if (it.fileType.type != FileType.Type.METADATA) return
            if (it.used) return

            println "\tfound md souce file ${it.fileName}"

            // hint to determine file type

            FileType.Type type = FileType.Type.UNKNOWN
            if (it.fileName.contains("fastq"))
                type = FileType.Type.SEQUENCE

            if (it.fileName.contains("align"))
                type = FileType.Type.ALIGNMENT

            File mdFile = new File(it.pathName + "/" + it.fileName)
            if (!mdFile.canRead()) {
                println "\tcan not read ${it.fileName}"
                return
            }

            def tokens
            def values
            def keys

            mdFile.eachLine { line, no ->

                if (no == 1) {

                    long start = new Date().getTime()
                    // parse the header
                    tokens = tokenize(line, '\t');
                    keys = getKeysFromTokens(tokens)
                    long stop = new Date().getTime()

                    //println "\theader ${stop - start}"

                } else {

                    // match values with the header
                    // new entry in MetaData

                    long start = new Date().getTime()

                    dataFile = new DataFile() // set-up later
                    run.addToDataFiles(dataFile)

                    safeSave(dataFile)

                    values = tokenize(line, '\t')
                    for(int i=0; i<keys.size(); i++) {

                        MetaDataKey key = keys.getAt(i)
                        MetaDataEntry entry = new MetaDataEntry (
                            value: values.getAt(i) ?: "",
                            source : MetaDataEntry.Source.MDFILE,
                            key : key,
                            //dataFile : dataFile
                        )

                        //safeSave(entry)
                        //key.addToMetaDataEntries(entry)
                        dataFile.addToMetaDataEntries(entry);
                        safeSave(entry)
                    }

                    long middle1 = new Date().getTime()

                    // fill-up important fields
                    assignFileName(dataFile)
                    assignFileType(dataFile, type)
                    addKnownMissingMetaData(run, dataFile)

                    long middle2 = new Date().getTime()

                    //dbGateService.safeSave(dataFile)
                    //dbGateService.safeSave(run)

                    long stop = new Date().getTime()

                    /*
                    println "\tline1 ${middle1 - start}"
                    println "\tline2 ${middle2 - middle1}"
                    println "\tline3 ${stop - middle2}"
                    println "\tline ${stop-start}\n"
                    */
                }
            }

            it.used = true;
            safeSave(it)

            // save the keys
            //keys.each { key ->
            //	safeSave(key)
            //}

            safeSave(run)
        }
    }



    /**
     * 
     * This method tokenizes a string
     * Standard java method can not be used because two subsequent tabs
     * shall return empty token. The tokens are stripped from quotation 
     * marks
     * 
     * @param line - input string
     * @param tab - separator (typically '\t')
     * @return - array of strings
     */

    private def tokenize(String line, String tab) {

        def tokens = []
        def chars = line.getChars()

        int idx = 0
        for(int i=0; i<line.length(); i++) {

            if (chars[i] == tab || i == line.length()-1) {

                int end = (i==line.length()-1)? i+1 : i
                String token = line.substring(idx, end);
                token = token.replaceAll('\"', '');
                token = token.replaceAll(tab, '');
                tokens << token
                //println "${idx} ${i} ${token}"
                idx = i+1;
            }
        }

        tokens
    }


    /**
     * 
     * I have to think
     * 
     * @param tokens
     * @return
     */

    private def getKeysFromTokens(List tokens) {

        def keys = []
        MetaDataKey key

        tokens.each {

            String token = correctedKey(it)

            key = MetaDataKey.findByName(token)
            if (key == null) {
                key = new MetaDataKey(name: token)
                safeSave(key)
            }

            keys << key
        }

        keys
    }



    /**
     * 
     * assign file name for a specific DataFile object
     * the file name is in either FASTQ_FILE or ALIGN_FILE 
     * column of meta-data.
     * this function also fills MD5 check sum field from meta-data
     * 
     * @param dataFile
     */

    private void assignFileName(DataFile dataFile) {

        long start = new Date().getTime()

        def keyNames = ["FASTQ_FILE", "ALIGN_FILE"]

        keyNames.each { 


            long startDF = new Date().getTime()

            /*
            MetaDataKey key = MetaDataKey.findByName(it)
            MetaDataEntry entry =
                MetaDataEntry.findByDataFileAndKey(dataFile, key)

            def criteria = MetaDataEntry.createCriteria()
            def entries = criteria.list {
                and {
                    eq("dataFile", dataFile)
                    eq("key", key)
                }
            }

            MetaDataEntry entry = entries[0]
            */

            MetaDataEntry entry	= getMetaDataEntry(dataFile, it)


             //dataFile.metaDataEntries.each { iEntry ->
            //	if (iEntry.key.name == it)
            //		entry = iEntry
            //}	

            long stopDF = new Date().getTime()
            //println "\tdynamic finder time ${stopDF - startDF}"

            if (!entry) return

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

            int idx = value.lastIndexOf("/");

            dataFile.pathName = (idx == -1)? "" : value.substring(0, idx)
            dataFile.fileName = value.substring(idx+1) ?: "error"
        }

        // md5 check sum
        //MetaDataKey key = MetaDataKey.findByName("MD5")
        //MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
        MetaDataEntry entry = getMetaDataEntry(dataFile, "MD5")
        dataFile.md5sum = entry?.value


        if (dataFile.fileName == null) {

            dataFile.fileName = "errorNoHeader"
            dataFile.pathName = "errorNoHeader"

            dataFile.metaDataEntries.each {
                println "${it.key} ${it.value}"
            }
        }

        long stop = new Date().getTime()	
        //println "\tfunction time ${stop-start}"
    }

    /**
     * 
     * Returns a metat data entry belonging the a given data file 
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
     * 
     * Returns a metat data entry belonging the a given data file 
     * with a key specified by the input parameter
     * 
     * @param file
     * @param key
     * @return
     */

    private MetaDataEntry getMetaDataEntry(DataFile file, String key) {

        MetaDataEntry entry = null

        file.metaDataEntries.each { MetaDataEntry iEntry ->
            if (iEntry.key.name == key) {
                entry = iEntry
                //println entry.value
            }
        }

        return entry
    }


    /**
     *
     * assign a file type object to e given DataFile
     * the assignment is based on two sources of information:
     * file name and if the file comes from "fastq" or "align"
     * meta-data file
     * 
     * @param dataFile
     * @param type
     */

    private void assignFileType(DataFile dataFile, FileType.Type type) {

        //println dataFile.fileName
        FileType tt = fileTypeService.getFileType(dataFile.fileName, type)
        tt.addToDataFiles(dataFile)
        safeSave(tt)
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
            dbGateService.safeSave(key)
        }


        MetaDataEntry entry = getMetaDataEntry(dataFile, keyName)
        //		MetaDataEntry.findByDataFileAndKey(dataFile, key)

        if (entry) return

        def types = SeqType.findAll()

        for(int iType = 0; iType < types.size(); iType++) {

            if (run.mdPath.contains(types[iType].dirName)) {

                String value = types[iType].name
                println "\tassiginig to ${value}"

                entry = new MetaDataEntry (
                    value: value,
                    source: MetaDataEntry.Source.SYSTEM,
                    key: key
                )
                //key.addToMetaDataEntries(entry)
                dataFile.addToMetaDataEntries(entry);
                return
            }
        }
    }


    /**
     * 
     * Solve known bugs
     * For example meta-data column which shall be "LANE_NO"
     * for some runs is "lane". The solid standard "SLIDE_NO"
     * is changed to "LANE_NO" to unify key for later processing
     * 
     * @param token
     * @return
     */

    private String correctedKey(String token) {

        if (token == "lane") return "LANE_NO"
        if (token == "SLIDE_NO") return "LANE_NO"

        return token
    }



    /**
     * 
     * Checks if values of MetaDataEntry table are correct 
     * the following keys are checked
     * RunID, sample identifier, sequencing center
     * sequencing type and library layout
     * 
     * @param runId
     */

    void validateMetadata(long runId) {

        Run run = Run.get(runId)

        run.dataFiles.each { dataFile ->

            dataFile.metaDataValid = true

            dataFile.metaDataEntries.each { entry ->

                MetaDataEntry.Status valid = MetaDataEntry.Status.VALID
                MetaDataEntry.Status invalid = MetaDataEntry.Status.INVALID


                if (entry.key.name == "RUN_ID") {
                    entry.status = (run.name == entry.value) ? valid : invalid
                }

                if (entry.key.name == "SAMPLE_ID") {
                    SampleIdentifier sample = SampleIdentifier.findByName(entry.value);
                    entry.status = (sample != null) ? valid : invalid
                }

                if (entry.key.name == "CENTER_NAME") {

                    entry.status = invalid
                    SeqCenter center = run.seqCenter

                    // normal case
                    if (center.name == entry.value)
                    entry.status = valid

                    // DKFZ Illumina case (as CORE)
                    if (center.dirName == entry.value.toLowerCase())
                    entry.status = valid
                }


                if (entry.key.name == "SEQUENCING_TYPE") {
                    SeqType seqType = SeqType.findByName(entry.value)
                    entry.status = (seqType != null) ? valid : invalid
                }

                if (entry.key.name == "LIBRARY_LAYOUT") {
                    SeqType seqType = SeqType.findByLibraryLayout(entry.value)
                    entry.status = (seqType != null) ? valid : invalid
                }

                if (entry.status == invalid) {
                    println "${entry.key}\t${entry.value}"
                    dataFile.metaDataValid = false
                }
            }
        }

        safeSave(run)
    }



    /**
     * 
     * This method tries to assign execution data for a run
     * this method knows different standards of encoding data 
     * in meta-data. If there is no MetaDataEntry with "RUN_DATE" 
     * key, the run date is build from run name. The method 
     * knows Solid and Illumina run naming standards. 
     * 
     * @param runId - database ID of Run object
     */

    void buildExecutionDate(long runId) {
        //
        // create Date object out of text field in metadata
        // in case this fail, build date from run name
        //
        // this function has hard-coded statnards in data encoding 
        // in different platforms
        //

        Run run = Run.get(runId)

        Date exDate = null
        boolean consistant = true

        run.dataFiles.each { dataFile ->

            MetaDataEntry entry = dataFile.metaDataEntries.find {it.key =="RUN_DATE"}
            if (entry == null) return

            Date date

            try {
                //
                // best effort to interpret date
                //

                if (entry.value.size() == 6)
                    date = Date.parse("yyMMdd", entry.value)

                if (entry.value.size() == 10)
                    date = Date.parse("yyyy-MM-dd", entry.value)

            } catch(Exception e) {}

            if (exDate == null) exDate = date
            if (exDate != null && !exDate.equals(date)) consistant = false

            dataFile.dateExecuted = date
        }

        // fill if all files have the same executions date
        if (exDate != null && consistant)
            run.dateExecuted = exDate

        // date from the runName
        if (run.dateExecuted == null) {
            // date from run name

            if (run.seqTech.name == "illumina") {
                String subname = run.name.substring(0, 6)
                Date d = Date.parse("yyMMdd", subname)
                run.dateExecuted = d
            }

            if (run.seqTech.name == "solid") {
                String subname = run.name.substring(10, 18)
                Date d = Date.parse("yyyyMMdd", subname)
                run.dateExecuted = d
            }
        }

        safeSave(run)
    }



    /**
     * 
     * A sequence track corresponds to one lane in Illumina
     * and one slide in Solid. 
     * 
     * This method build sequenc tracks for a given run. 
     * To each sequence track there are raw data and alignment
     * data attached
     * 
     * @param runId
     */

    void buildSequenceTracks(long runId) {

        Run run = Run.get(runId)

        // find out present lanes/slides
        // lines/ slides could by identifiers not only numbers

        MetaDataKey key = MetaDataKey.findByName("LANE_NO")

        /*
        def entries = MetaDataEntry.findAll (
        "from MetaDataEntry mde " +
        "where mde.dataFile.run.id = ? and mde.dataFile.fileType.type = ? " +
        "and mde.dataFile.metaDataValid = ? " +
        "and mde.key = ? order by mde.value",
        run.id, FileType.Type.SEQUENCE, true, key
        )
        */

        def entries = []

        // get the list of unique lanes identifiers
        run.dataFiles.each {DataFile dataFile -> 

            if (!dataFile.metaDataValid) return
            if (dataFile.fileType.type != FileType.Type.SEQUENCE) return

            dataFile.metaDataEntries.each {entry ->

                if (entry.key != key) return

                // check if exists
                for(int i=0; i<entries.size(); i++) {
                    if (entries[i].value == entry.value) 
                    return
                }

                entries << entry
            }
        }

        // run track creation for each lane
        for(int i=0; i<entries.size(); i++) {
            println "LANE ${entries[i].value}"
            buildOneSequenceTrack(run, entries[i].value)
        }
    }



    /**
     * 
     * Builds one sequence track identified by a lane id 
     * 
     * @param run - Run obejct
     * @param lane - lane identifier string
     */

    private void buildOneSequenceTrack(Run run, String lane) {
        //
        // this function build one sequence track for a given run
        //

        //println "Building lane ${lane}"

        // find sequence files
        def laneDataFiles =
            getRunFilesWithTypeAndLane(run, FileType.Type.SEQUENCE, lane)

        // check if metadata consistent

        def keyNames = [
            "SAMPLE_ID",
            "SEQUENCING_TYPE",
            "LIBRARY_LAYOUT",
            "PIPELINE_VERSION",
            "READ_COUNT"
        ]

        def keys = []
        keyNames.each {
            MetaDataKey key = MetaDataKey.findByName(it)
            keys << key
        }

        def values = []

        //println keys
        boolean consistent = checkIfConsistent(laneDataFiles, keys, values)

        // error handling
        if (!consistent) return

        // check if complete
        // to be implemented

        // build structure

        //println values
        SampleIdentifier sampleId = SampleIdentifier.findByName(values[0])
        Sample sample = sampleId.sample
        if (sample == null) return

        SeqType seqType = SeqType.findByNameAndLibraryLayout(values[1], values[2])
        if (seqType == null) return

        SeqTrack seqTrack = new SeqTrack(
            run : run,
            sample : sample,
            seqType : seqType,
            seqTech : run.seqTech,
            laneId : lane as int,
            hasFinalBam : false,
            hasOriginalBam : false,
            usingOriginalBam : false
        )

        laneDataFiles.each {
            seqTrack.addToDataFiles(it)
        }

        fillReadsForSeqTrack(seqTrack);
        safeSave(seqTrack)

        // alignment part

        // get files
        def alignFiles =
            getRunFilesWithTypeAndLane(run, FileType.Type.ALIGNMENT, lane)

        // no alignment files
        if (!alignFiles) return

        // find out if data complete
        def alignKeyNames = ["SAMPLE_ID", "ALIGN_TOOL"]
        def alignKeys = []
        alignKeyNames.each {
            MetaDataKey key = MetaDataKey.findByName(it)
            alignKeys << key
        }

        def alignValues = []		
        consistent = checkIfConsistent(alignFiles, alignKeys, alignValues)

        //println "${alignValues} ${consistent}"
        if (!consistent) return
        if (values[0] != alignValues[0]) return


        println "alignment data found"

        // create or find aligment params object

        String alignProgram = alignValues[1] ?: values[3]
        AlignmentParams alignParams = AlignmentParams.findByProgramName(alignProgram)

        if (!alignParams)
        alignParams = new AlignmentParams(programName: alignProgram)
        safeSave(alignParams)

        // create alignment log		
        AlignmentLog alignLog = new AlignmentLog(
            alignmentParams : alignParams,
            seqTrack : seqTrack
        )

        // attach data files

        alignFiles.each {
            alignLog.addToDataFiles(it)
        }

        seqTrack.hasOriginalBam = true

        // save
        safeSave(alignLog)
        safeSave(alignParams)
        safeSave(seqTrack)
    }



    /**
     * 
     * @param run
     * @param type
     * @param lane
     * @return
     */

    private def getRunFilesWithTypeAndLane(Run run, FileType.Type type, String lane) {
        //
        // helper function
        // return all dataFiles for a given run, type and lane
        //

        MetaDataKey key = MetaDataKey.findByName("LANE_NO")

        def c = DataFile.createCriteria()
        def dataFiles = c.list {
            and {
                eq("run", run)
                fileType{
                    eq("type", type)
                }
                metaDataEntries {
                    and{
                        eq("key", key)
                        eq("value", lane)
                    }
                }
            }
        }

        return dataFiles
    }



    /**
     * 
     * Check if meta-data values for DataFile objects belonging 
     * presumably to the same lane are consistent
     * The function returns the values associated with the 
     * keys to be checked
     * 
     * @param dataFiles - array of data files 
     * @param keys - keys for which the consistency have to be checked
     * @param values - this array will be filled with values for given keys  
     * @return consistency status
     */

    private boolean checkIfConsistent(def dataFiles, def keys, def values) {
        //
        // helper function
        // checks if metadata entries for a given dataFiles
        // and keys are consistent, fill values collection
        //

        if (dataFiles == null) return false;

        boolean consistent = true
        for(int iKey=0; iKey<keys.size; iKey++) {

            MetaDataEntry reference =
                    getMetaDataEntry(dataFiles[0], keys[iKey])
            //MetaDataEntry.findByDataFileAndKey(dataFiles[0], keys[iKey])

            values[iKey] = reference?.value

            for(int iFile = 1; iFile < dataFiles.size; iFile++) {
                MetaDataEntry entry = getMetaDataEntry(dataFiles[iFile], keys[iKey])
                //MetaDataEntry.findByDataFileAndKey(dataFiles[iFile], keys[iKey])

                if (entry?.value != reference?.value) {
                    println entry?.value
                    println reference?.value
                    consistent = false
                }
            }
        }

        consistent
    }



    /**
     * 
     * Fills the numbers in the SeqTrack object using MetaDataEntry
     * objects from the DataFile objects belonging to this SeqTrack.  
     * 
     * @param seqTrack
     */

    private void fillReadsForSeqTrack(SeqTrack seqTrack) {

        if (seqTrack.seqTech.name == "illumina") {

            def dataFiles = seqTrack.dataFiles

            def dbKeys = ["BASE_COUNT", "READ_COUNT", "INSERT_SIZE"]
            def dbFields = ["nBasePairs", "nReads", "insertSize"]
            def add = [true, false, false]

            dataFiles.each {file ->

                if (file.fileType.type != FileType.Type.SEQUENCE) return

                file.metaDataEntries.each {entry ->

                    for(int iKey=0; iKey < dbKeys.size(); iKey++) {

                        if (entry.key.name == dbKeys[iKey]) {

                            long value = 0;
                            if  (entry.value.isLong())
                                value = entry.value as long

                            if (add[iKey])
                                seqTrack."${dbFields[iKey]}" += value
                            else
                                seqTrack."${dbFields[iKey]}" = value
                        }
                    }
                }
            }
        }
    }



    /**
     * 
     * checks if all sequence (raw and aligned) data files are attached 
     * to SeqTrack objects. If yes the field DataFile.used field is set to true. 
     * if data file is a sequence or alignment type and is 
     * not used it contains errors in meta-data
     * 
     * @param runId
     */

    void checkSequenceTracks(long runId) {

        Run run = Run.get(runId)
        run.allFilesUsed = true

        run.dataFiles.each { dataFile ->

            if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
                dataFile.used = (dataFile.seqTrack != null)
                if (!dataFile.used) {
                    println dataFile
                    run.allFilesUsed = false
                }
            }

            if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
                dataFile.used = (dataFile.alignmentLog != null)
                if (!dataFile.used) {
                    println dataFile
                    run.allFilesUsed = false
                }
            }
        }

        println "All files used: ${run.allFilesUsed}\n"
    }



    /**
     * 
     * Build SeqScans
     * This functions search sequencing tracks which where not assigned
     * to any SeqScan and calls a method to build a specific SeqScan 
     *
     */

    void buildSeqScans() {

        def seqTracks = SeqTrack.findAll()
        def seqTracksNew = []

        // create a list of new seqTracks
        seqTracks.each { seqTrack ->
            if (seqTrack.seqScan == null)
                seqTracksNew << seqTrack
        }

        println "number of new tracks: ${seqTracksNew.size()}"

        seqTracksNew.each {
            buildSeqScan(it)
        }
    }



    /**
     *  
     * Build one SeqScan based on parameters in 
     * the input SeqTrack. If SeqTrack is already used in other 
     * SeqScan no new SeqScan will be created
     * 
     * @param seqTrack - new Sequencing Track
     */

    void buildSeqScan(SeqTrack seqTrack) {
        //
        // build one SeqScan
        //

        // maybe track already consumed
        if (seqTrack.seqScan != null) return

        // take parameters
        Sample sample = seqTrack.sample
        SeqTech seqTech = seqTrack.seqTech
        SeqType seqType = seqTrack.seqType

        //AlignmentParams params = AlignmentParams.get(1)


        // find all lanes
        def c = SeqTrack.createCriteria()
        def seqTracksToMerge = c.list {
            and {
                eq("sample", sample)
                eq("seqTech", seqTech)
                eq("seqType", seqType)
            }
        }

        println "found lanes: ${seqTracksToMerge}"

        /*
        // check if all have bam file
        boolean allBams = true
        seqTracksToMerge.each { SeqTrack iTrack ->
            if (!iTrack.hasFinalBam) allBams = false
        }

        if (!allBams) return
        */


        // find old seqScan and invalidate it
        def criteria = SeqScan.createCriteria()
        def oldSeqScans = criteria.list {
            and {
                eq("sample", sample)
                eq("seqTech", seqTech)
                eq("seqType", seqType)
            }
        }

        oldSeqScans.each {SeqScan old ->
            old.state = SeqScan.State.OBSOLETE
            safeSave(old)
        }


        println "invalidating ${oldSeqScans.size()} seq scans"


        // create new seqScan

        SeqScan seqScan = new SeqScan(
            alignmentParams : null,
            sample: sample,
            seqTech: seqTech,
            seqType: seqType
        )

        //sample.addToSeqScans(seqScan)
        //seqTech.addToSeqScans(seqScan)
        //seqType.addToSeqScans(seqScan)

        seqTracksToMerge.each { SeqTrack iTrack ->
            seqScan.addToSeqTracks(iTrack)
        }

        fillSeqScan(seqScan)
        fillSeqCenters(seqScan)

        safeSave(seqScan)
        safeSave(sample)
        safeSave(seqTech)
        safeSave(seqType)
    }



    /**
     * 
     * Fills SeqScan object with numbers derived from 
     * its SeqTrack objects. Coverage is calculated as
     * number of base pairs divided by genome size (3e9).
     * 
     * Coverage number will be replaced be a full calculation
     * from coverage analysis
     * 
     * @param seqScan - SeqScan object
     */

    private void fillSeqScan(SeqScan seqScan) {

        seqScan.nLanes = seqScan.seqTracks.size()

        long nbp = 0
        seqScan.seqTracks.each {SeqTrack seqTrack ->
            nbp += seqTrack.nBasePairs
        }

        seqScan.nBasePairs = nbp
        //seqScan.coverage = nbp / 3.0e9
    }



    /**
     * 
     *  Most of samples are sequenced in one sequence center
     *  if this is the case the string seqCenter for a given
     *  SeqScan is filed. 
     *  
     * @param seqScan
     */

    private void fillSeqCenters(SeqScan seqScan) {

        SeqCenter seqCenter = null
        String name = ""

        seqScan.seqTracks.each {SeqTrack seqTrack ->

            if (seqCenter == null) {
                seqCenter = seqTrack.run.seqCenter
                name = seqCenter.name
            }

            if (seqCenter != null) {
                if (seqTrack.run.seqCenter != seqCenter)
                    name += "*"
            }
        }

        seqScan.seqCenters = name
    }



    /**
     * 
     * probably will go to separate static class
     * no formal exception, information only
     * 
     * @param obj
     */

    private void safeSave(def obj) {

        obj.validate()
        if (obj.hasErrors()) {
            println obj.errors
            return
        }

        if (!obj.save())
        println "can not save ${obj}"
    }
}