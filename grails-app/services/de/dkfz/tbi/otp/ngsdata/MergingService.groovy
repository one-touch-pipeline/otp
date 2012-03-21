package de.dkfz.tbi.otp.ngsdata

class MergingService {
    /**
     * Dependency injection of SeqScanService
     */
    def seqScanService

    // TODO: this constant should not be here!
    private static final String basePath = "$ROOT_PATH/project/"

    /**
     * needs revision
     * @param ind
     * @param types
     */
    // TODO check if needed
    List<String> printAllMergedBamForIndividual(Individual ind, List<SeqType> types) {
        if (!ind) {
            throw new IllegalArgumentException("Individual may not be null")
        }
        List<String> mergedBams = new ArrayList<String>()
        String projectPath = ind.project.dirName
        String baseDir = basePath + "/" + projectPath + "/sequencing/"
        types.each { SeqType type ->
            Sample.findAllByIndividual(ind).each { Sample sample ->
                String path = baseDir + type.dirName + "/view-by-pid/" +
                        ind.pid + "/" + sample.type.toString().toLowerCase() +
                        "/" + type.libraryLayout.toLowerCase() +
                        "/merged-alignment/"
                File mergedDir = new File(path)
                mergedDir.list().each { String fileName ->
                    if (fileName.endsWith(".bam")) {
                        mergedBams << fileName
                    }
                }
            }
        }
        return mergedBams
    }

    /**
     *
     * This funtions discovers all files in a directory for
     * merged bam files and with suffix ".bam".
     * For discovered files the function buildSeqScan is called
     *
     * @param ind Individual to be analyzed
     */
    void discoverMergedBams(Individual ind) {
        if (!ind) {
            throw new IllegalArgumentException("Individual may not be null")
        }
        List<SeqType> types = SeqType.findAll()
        String baseDir = getProjectFullPath(ind.project)
        types.each { SeqType type ->
            Sample.findAllByIndividual(ind).each { Sample sample ->
                String path = baseDir + getRelativePathToMergedAlignment(type, sample)
                File mergedDir = new File(path)
                mergedDir.list().each { String fileName ->
                    if (isNewBamFile(path, fileName)) {
                        buildSeqScan(sample, type, path, fileName)
                    }
                }
            }
        }
    }

    private boolean isNewBamFile(String path, String fileName) {
        if (!fileName.endsWith(".bam")) {
            return false
        }
        if (DataFile.findByPathNameAndFileName(path, fileName)) {
            return false
        }
        File bamFile = new File(path, fileName)
        if (!bamFile.canRead()) {
            return false
        }
        return true
    }

    /**
     * returns base directory of the project
     * based on base directory of the OTP system and project object
     * @param project
     * @return
     */
    private String getProjectFullPath(Project project) {
        return basePath + "/" + project.dirName + "/sequencing/"
    }

    /**
     * returns path relative to project base paths
     * to directory with merged alignment files
     * @param type
     * @param sample
     * @return
     */
    private String getRelativePathToMergedAlignment(SeqType type, Sample sample) {
        return type.dirName + "/view-by-pid/" + sample.individual.pid + "/" + 
        sample.type.toString().toLowerCase() + "/" + type.libraryLayout.toLowerCase() + 
        "/merged-alignment/"
    }

    /**
     *
     * This function build seqScan for a given bam file
     * pointed by pathToBam and fileName.
     *
     * If corresponding seqScan is already in database, the function
     * attach a bam file to it and changed status to FINISHED.
     * If there is no SeqScan it is created. This function is not
     * invalidating other seqScans.
     *
     * @param sample
     * @param seqType
     * @param pathToBam
     * @param fileName
     */
    private void buildSeqScan(Sample sample, SeqType seqType,
              String pathToBam, String fileName) {

        File bamFile = new File(pathToBam, fileName)
        String text = getBamFileHeader(bamFile)
        List<SeqTrack> tracks = getSeqTracks(text)
        if (!tracks) {
            return
        }
        if (!assertSample(sample, tracks)) {
            return
        }

        // get all possible seq scans
        List<SeqScan> seqScans = getSeqScans(sample, seqType)
        // check if the matching seq scan exists
        SeqScan matchingScan = getMatchingSeqScan(seqScans, tracks)
        // get alignment log from header
        AlignmentParams alignParams = getAlignmentParamsFromHeader(bamFile)
        // build SeqScan
        if (!matchingScan) {
            log.debug("Building new SeqScan")
            matchingScan = seqScanService.buildSeqScan(tracks, alignParams)
            checkIfObsolite(matchingScan)
        }
        // file type
        FileType mergedType = FileType.findByType(FileType.Type.MERGED)
        // create common objects (dataFile and mergingLog)
        DataFile bamDataFile = new DataFile(
                fileName: fileName,
                pathName: pathToBam,
                project: sample.individual.project,
                fileExists: true,
                used: true,
                fileType: mergedType
                )
        bamDataFile.dateFileSystem = new Date(bamFile.lastModified())
        bamDataFile.fileSize = bamFile.length()
        bamDataFile.validate()
        println bamDataFile.errors
        bamDataFile.save(flush:true)
        // create Merging Log
        MergingLog mergingLog = new MergingLog(
                alignmentParams: alignParams,
                executedBy: MergingLog.Execution.DISCOVERY,
                status: MergingLog.Status.FINISHED
                )
        mergingLog.save()
        bamDataFile.mergingLog = mergingLog
        bamDataFile.save()
        mergingLog.seqScan = matchingScan
        mergingLog.save()
        if (matchingScan.state != SeqScan.State.OBSOLETE) {
            matchingScan.state = SeqScan.State.FINISHED
        }
        matchingScan.validate()
        matchingScan.save(flush: true)
    }

    private List<SeqTrack> getSeqTracks(String text) {
        final String lineStart = "@RG"
        List<SeqTrack> seqTracks = new ArrayList<SeqTrack>()
        String rgString = ""
        int nLines = 0
        text.eachLine {String line ->
            if (line.startsWith(lineStart)) {
                rgString += line + "\n"
                nLines++
                List<String> tokens = line.tokenize("\t")
                SeqTrack track = null
                track = parseTokensV1(tokens)
                if (track) {
                    seqTracks << track
                    return
                }
                track = parseTokensV2(tokens)
                if (track) {
                    seqTracks << track
                    return
                }
                track = parseTokensV3(tokens)
                if (track) {
                    seqTracks << track
                    return
                }
                track = parseTokensV4(tokens)
                if (track) {
                    seqTracks << track
                    return
                }
            }
        }
        if (seqTracks.size() != nLines) {
            println "Number of tracks: ${seqTracks.size()} ${nLines}"
            println rgString
            return null
        }
        return seqTracks
    }

    /*
    * parse format ID:runName_s_lane
    */
    private SeqTrack parseTokensV1(List<String> tokens) {
        for(String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_s_")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId);
                    String lane = token.substring(token.lastIndexOf("_")+1)
                    println "${runName} ${lane}"
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV2(List<String> tokens) {
        for(String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_lane")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId);
                    String lane = token.substring(sepId+5, sepId+6)
                    println "${runName} ${lane}"
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV3(List<String> tokens) {
        String runName
        String lane
        for(String token in tokens) {
            if (token.startsWith("SM:")) {
                runName = token.substring(3)
            }
            if (token.startsWith("ID:")) {
                int idxFrom = token.indexOf("_") + 1
                int idxTo = token.indexOf(".")
                if (idxTo > 0) {
                    lane = token.substring(idxFrom, idxTo)
                } else {
                    lane = token.substring(idxFrom)
                }
            }
        }
        println "${runName} ${lane}"
        return getSeqTrack(runName, lane)
    }

    private SeqTrack parseTokensV4(List<String> tokens) {
        for(String token in tokens) {
            if (token.contains("DS:")) {
                token = token.substring(token.indexOf("DS:"))
            }
            if (token.startsWith("DS:")) {
                String runName = token.substring(3)
                int idx = runName.indexOf("_FC") + 3
                String lane = runName.substring(idx)
                println "${runName} ${lane}"
                return getSeqTrack(runName, lane)
            }
        }
        return null
    }

    private boolean assertSample(Sample sample, List<SeqTrack> tracks) {
        for(SeqTrack track in tracks) {
            if (track.sample.id != sample.id) {
                //throw new SampleInconsistentException(sample.toString(), track.toString())
                println "SAMPLE MIXUP ${sample} -- ${track}"
                return false
            }
        }
        return true
    }

    /**
     *
     * This function analyzes seqSacan to check if it contains
     * all and only seqTracks from the provided list
     *
     * @param seqScans
     * @param tracks
     * @return
     */
    private SeqScan getMatchingSeqScan(List<SeqScan> seqScans, List<SeqTrack> tracks) {
        List<MergingAssignment> assignments = MergingAssignment.findAllBySeqTrackInList(tracks)
        for(MergingAssignment assignment in assignments) {
            SeqScan scan = assignment.seqScan
            if (checkIfCorrect(scan, tracks)) {
                return scan
            }
        }
        return null
    }

    private boolean checkIfCorrect(SeqScan scan, List<SeqTrack> tracks) {
        List<MergingAssignment> assignments = MergingAssignment.findAllBySeqScan(scan)
        if (assignments.size() != tracks.size()) {
            return false
        }
        for(MergingAssignment assignment in assignments) {
            SeqTrack seqTrack = assignment.seqTrack
            if (!tracks.contains(seqTrack)) {
                return false
            }
        }
        return true
    }

    /**
     *
     * This function transform a list of run_lane identifiers (String)
     * from bam file header into a list of SeqTrack objects.
     *
     * @param sample
     * @param seqType
     * @param header - List of strings from bam file header
     * @return
     */
    /*
    private List<SeqTrack> getSeqTracks(Sample sample, SeqType seqType, List<String> header) {
        final String separator = "_s_"
        final String preNumber = "_"
        List<SeqTrack> tracks = new Vector<SeqTrack>()
        for(String line in header) {
            println line
            String runName
            String lane
            int sepId = line.indexOf(separator)
            if (sepId > -1) {
                runName = line.substring(0, sepId);
                lane = line.substring(line.lastIndexOf(preNumber) + 1)
            } else {
                sepId = line.indexOf("_lane")
                runName = line.substring(0, sepId);
                lane = line.substring(sepId + 5, sepId + 6)
            }
            //SeqTrack seqTrack = getSeqTrack(sample, seqType, runName, lane)
            SeqTrack seqTrack = getSeqTrack(runName, lane)
            if (!seqTrack) {
                println "no SeqTrack for ${runName} ${lane}"
                continue
            } else {
                tracks << seqTrack
            }
        }
        return tracks
    }
    */

    private SeqTrack getSeqTrack(String runName, String lane) {
        String runString = runName
        if (runName.startsWith("run")) {
            runString = runName.substring(3)
        }
        Run run = Run.findByName(runString)
        SeqTrack seqTrack = SeqTrack.findByRunAndLaneId(run, lane)
        return seqTrack
    }

    /**
     *
     * Helper function
     * Select from a DB a seqTrack belonging to given run, lane
     * sample and seqType. This criteria are over critical thus
     * consistency of seqTrack or of input parameters is checkd.
     *
     * @param sample
     * @param seqType
     * @param runName
     * @param lane
     * @return
     */
    /*
    private SeqTrack getSeqTrack(Sample sample, SeqType seqType, String runName, String lane) {
        String runString = runName.substring(3) // removing "run"
        List<SeqTrack> seqTracks =
            SeqTrack.withCriteria {
                and {
                    eq("sample", sample)
                    eq("seqType", seqType)
                    eq("laneId", lane)
                    run { eq("name", runString) }
                }
            }
        if (seqTracks.size() != 1) {
            log.debug("Number of seqTracks = ${seqTracks.size()}")
        } else {
            return seqTracks.get(0)
        }
        return null
    }
    */

    /**
     *
     * Helper function to obtain all SeqScans belonging
     * to a given sample and sequencing type
     *
     * @param sample
     * @param seqType
     * @return list of seqScans from database
     */
    private List<SeqScan> getSeqScans(Sample sample, SeqType seqType) {
        return SeqScan.findAllBySampleAndSeqType(sample, seqType)
    }

    /**
     * 
     * @param scan
     * @return
     */
    private checkIfObsolite(SeqScan scan) {
        int nLanes = scan.nLanes
        List<SeqScan> allScans = getSeqScans(scan.sample, scan.seqType)
        for(SeqScan otherScan in allScans) {
            if (!otherScan.seqPlatform.equals(scan.seqPlatform)) {
                continue
            }
            if (otherScan.nLanes > nLanes &&
                otherScan.state == SeqScan.State.FINISHED) {
                scan.state = SeqScan.State.OBSOLETE
                scan.save()
            }
        }
    }

    /**
     *
     * This function parses header of a bam file.
     * It is intended to provide a list of run names and
     * lane labels of lanes that contributed to this bam file.
     *
     * This function knows bam files header tags.
     *
     * @param bamFile input file
     * @return list of strings, each string contains one run_lane id
     */
    /*
    private List<String> parseBamHeader(File bamFile) {
        final String lineStart = "@RG"
        final String startToken = "ID:"
        // get the header
        String text = getBamFileHeader(bamFile)
        // select lines starting with @RG
        List<String> lines = new Vector<String>()
        text.eachLine {String line ->
            if (line.startsWith(lineStart)) {
                lines << header 
                List<String> tokens = line.tokenize("\t")
                for (String token in tokens) {
                    // select id only
                    if (token.startsWith(startToken)) {
                        String runName = token.substring(startToken.size())
                        lines << runName
                    }
                }
            }
        }
        return lines
    }
    */

    /**
     * Parses header of bam file for obtain informations
     * about alignment program. If corresponding object
     * is than found in DB it is returned, otherwise a new
     * AlignmentParams object is created.
     *
     * This function knows bam files header tags.
     *
     * @param bamFile
     * @return
     */
    private AlignmentParams getAlignmentParamsFromHeader(File bamFile) {
        final String lineStart = "@PG"
        final String nameTag = "PN:"
        final String versionTag = "VN:"
        String text = getBamFileHeader(bamFile)
        String progName
        String progVersion
        text.eachLine {String line ->
            if (line.startsWith(lineStart)) {
                List<String> tokens = line.tokenize("\t")
                for (String token in tokens) {
                    if (token.startsWith(nameTag)) {
                        progName = token.substring(3)
                    }
                    if (token.startsWith(versionTag)) {
                        progVersion = token.substring(3)
                    }
                }
            }
        }
        println "${progName} ${progVersion}"
        SoftwareTool pipeline =  getPipeline(progName, progVersion)
        AlignmentParams alignParams = AlignmentParams.findByPipeline(pipeline)
        if (!alignParams) {
            alignParams = new AlignmentParams(
                pipeline: pipeline
            )
            alignParams.save()
        }
        return alignParams
    }

    private SoftwareTool getPipeline(String progName, String progVersion) {
        SoftwareTool pipeline =
            SoftwareTool.findByProgramNameIlikeAndProgramVersion(progName, progVersion)
        if (!pipeline) {
            pipeline = getDefaultPipeline()
        }
        return pipeline
    }

    private SoftwareTool getDefaultPipeline() {
        final String defProgram = "Unknown"
        final SoftwareTool.Type type = SoftwareTool.Type.ALIGNMENT
        return SoftwareTool.findByProgramNameIlikeAndType(defProgram, type)
    }

    /**
     * Helper function
     * This function provides a header for a given bam files
     * This functions is using samtools internally
     *
     * @param bamFile
     * @return
     */
    private String getBamFileHeader(File bamFile) {
        // execute samtools
        String cmd = "samtools view -H " + bamFile.getCanonicalPath()
        Process process = Runtime.getRuntime().exec(cmd)
        process.waitFor()
        // get the header
        return process.getInputStream().getText()
    }
}
