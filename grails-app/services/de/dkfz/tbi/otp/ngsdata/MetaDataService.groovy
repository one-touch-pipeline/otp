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
     * 
     * 
     * 
     * @param runId
     */

    void assignFilesToProjects(long runId) {

        Run run = Run.get(runId)

        run.dataFiles.each {DataFile dataFile ->

            if (!dataFile.used) return
            if (dataFile.fileType.type == FileType.Type.METADATA) return

            if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
                Project project = dataFile.seqTrack.sample.individual.project
                project.addToRuns(run)
                dataFile.project = project;
            }

            if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
                Project project = dataFile.alignmentLog.seqTrack.sample.individual.project
                project.addToRuns(run)
                dataFile.project = project
            }
        }

        safeSave(run)
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