package de.dkfz.tbi.otp.ngsdata

private class SplittedPath {
    String root
    String relative
    String fileName
    String dirPath() {
        return "${root}/${relative}"
    }
    String filePath() {
        return "${dirPath()}/${fileName}"
    }
}


class MergingService {
    /**
     * Dependency injection of SeqScanService
     */
    def seqScanService
    def configService
    def bamHeaderParsingService

    /**
     * needs revision
     * @param ind
     * @param types
     */
    // TODO check if needed
    /*
    List<String> printAllMergedBamForIndividual(Individual ind, List<SeqType> types) {
        if (!ind) {
            throw new IllegalArgumentException("Individual may not be null")
        }
        List<String> mergedBams = new ArrayList<String>()
        String projectPath = ind.project.dirName
        String baseDir = basePath + projectPath + "/sequencing/"
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
    */

    /**
     *
     * This function discovers all files in a directory for
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
        SplittedPath path = new SplittedPath()
        path.root = configService.getProjectRootPath(ind.project)
        types.each { SeqType type ->
            Sample.findAllByIndividual(ind).each { Sample sample ->
                path.relative = getRelativePathToMergedAlignment(type, sample)
                File mergedDir = new File(path.dirPath())
                mergedDir.list().each { String fileName ->
                    path.fileName = fileName
                    if (isNewBamFile(path)) {
                        buildSeqScan(sample, type, path)
                    }
                }
            }
        }
    }

    private boolean isNewBamFile(SplittedPath path) {
        if (!path.fileName.endsWith(".bam")) {
            return false
        }
        if (MergedAlignmentDataFile.findByFilePathAndFileName(path.relative, path.fileName)) {
            return false
        }
        File bamFile = new File(path.filePath())
        if (!bamFile.canRead()) {
            return false
        }
        return true
    }

    /**
     * returns path relative to project base paths
     * to directory with merged alignment files
     * @param type
     * @param sample
     * @return
     */
    private String getRelativePathToMergedAlignment(SeqType type, Sample sample) {
        String projectDir = sample.individual.project.dirName
        String pid = sample.individual.pid
        String sampleType = sample.type.toString().toLowerCase()
        String layout = type.libraryLayout.toLowerCase() 
        return "${projectDir}/sequencing/${type.dirName}/view-by-pid/${pid}/${sampleType}/${layout}/merged-alignment/"
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
    private void buildSeqScan(Sample sample, SeqType seqType, SplittedPath path) {
        File bamFile = new File(path.filePath())
        String text = getBamFileHeader(bamFile)
        List<SeqTrack> tracks = getSeqTracks(text)
        if (!tracks) {
            println "no tracks"
            return
        }
        if (!assertSample(sample, tracks)) {
            println "sample inconsistent"
            return
        }

        // check if the matching seq scan exists
        SeqScan matchingScan = getMatchingSeqScan(tracks)
        // get alignment log from header
        AlignmentParams alignParams = getAlignmentParamsFromHeader(bamFile)
        // build SeqScan
        if (!matchingScan) {
            log.debug("Building new SeqScan")
            matchingScan = seqScanService.buildSeqScan(tracks, alignParams)
            checkIfObsolite(matchingScan)
        }

        // create Merging Log
        MergingLog mergingLog = new MergingLog(
            alignmentParams: alignParams,
            executedBy: MergingLog.Execution.DISCOVERY,
            status: MergingLog.Status.FINISHED,
            seqScan: matchingScan
        )
        verboseSave(mergingLog)

        // create common objects (dataFile and mergingLog)
        MergedAlignmentDataFile bamDataFile = new MergedAlignmentDataFile(
            mergingLog: mergingLog,
            fileName: path.fileName,
            filePath: path.relative,
            fileSystem: path.root,
            fileExists: true,
            indexFileExists: false,
            dateFileSystem: new Date(bamFile.lastModified()),
            fileSize: bamFile.length()
        )
        verboseSave(bamDataFile)

        if (matchingScan.state != SeqScan.State.OBSOLETE) {
            matchingScan.state = SeqScan.State.FINISHED
        }
        verboseSave(matchingScan)
    }

    private List<SeqTrack> getSeqTracks(SplittedPath path) {
        File bamFile = new File(path.filePath())
        String text = getBamFileHeader(bamFile)
        return getSeqTracks(text)
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
                SeqTrack track = bamHeaderParsingService.seqTrackFromTokens(tokens)
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
     * @param tracks
     * @return
     */
    private SeqScan getMatchingSeqScan(List<SeqTrack> tracks) {
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

    private void verboseSave(Object obj) {
        if (obj.validate()) {
            obj.save(flush: true)
        } else {
            println obj
            println obj.errors
        }
    }
}
