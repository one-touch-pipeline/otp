package de.dkfz.tbi.otp.ngsdata

class MergingService {

    def SeqScanService

    String basePath = "$ROOT_PATH/project/"

    /**
     *
     * @param ind
     * @param types
     */
    List<String> printAllMergedBamForIndividual(Individual ind, List<SeqType> types) {
        if (ind == null) {
            return
        }
        List<String> mergedBams = new ArrayList<String>()
        String projectPath = ind.project.dirName
        String baseDir = basePath + "/" + projectPath + "/sequencing/"
        types.each {SeqType type ->
            ind.samples.each {Sample sample ->
                String path = baseDir + type.dirName + "/view-by-pid/" +
                        ind.pid + "/" + sample.type.toString().toLowerCase() +
                        "/" + type.libraryLayout.toLowerCase() +
                        "/merged-alignment/"
                File mergedDir = new File(path)
                mergedDir.list().each {String fileName ->
                    if (fileName.endsWith(".bam"))
                        mergedBams << fileName
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
        if (ind == null) {
            return
        }
        List<SeqType> types = SeqType.findAll()
        String projectPath = ind.project.dirName
        String baseDir = basePath + "/" + projectPath + "/sequencing/"
        types.each {SeqType type ->
            ind.samples.each {Sample sample ->
                String path = baseDir + type.dirName + "/view-by-pid/" +
                        ind.pid + "/" + sample.type.toString().toLowerCase() +
                        "/" + type.libraryLayout.toLowerCase() +
                        "/merged-alignment/"
                File mergedDir = new File(path)
                mergedDir.list().each {String fileName ->
                    if (fileName.endsWith(".bam")) {
                        log.debug("Discovered ${fileName}")
                        buildSeqScan(sample, type, path, fileName)
                    }
                }
            }
        }
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
        // check if already exists
        DataFile file = DataFile.findByFileNameAndPathName(fileName, pathToBam)
        if (file != null) {
            log.debug("File ${fileName} in ${pathToBam} already registered")
            return
        }
        // parse header
        File bamFile = new File(pathToBam, fileName)
        if (!bamFile.canRead()) {
            log.debug("Can not read file ${bamFile}")
            return
        }
        List<String> header = parseBamHeader(bamFile)
        if (header == null) {
            log.debug("No hader for file ${bamFile}")
            return
        }

        // get list of seq Tracks
        List<SeqTrack> tracks = getSeqTracks(sample, seqType, header)
        // get all possible seq scans
        List<SeqScan> seqScans = getSeqScans(sample, seqType)
        // check if the matching seq scan exists
        SeqScan matchingScan = getMatchingSeqScan(seqScans, tracks)
        // database
        // get alignment log from header
        AlignmentParams alignParams = getAlignmentParamsFromHeader(bamFile)
        // build SeqScan
        if (matchingScan == null) {
            log.debug("Building new SeqScan")
            matchingScan = seqScanService.buildSeqScan(tracks, alignParams)
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
        // create Merging Log
        MergingLog mergingLog = new MergingLog(
                alignmentParams: alignParams,
                executedBy: MergingLog.Execution.UPLOAD,
                status: MergingLog.Status.FINISHED
                )
        mergingLog.addToDataFiles(bamDataFile)
        matchingScan.addToMergingLogs(mergingLog)
        if (matchingScan.state != SeqScan.State.OBSOLETE) {
            matchingScan.state = SeqScan.State.FINISHED
        }
        matchingScan.validate()
        matchingScan.save(flush: true)
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
        SeqTrack[] arrTracks = (SeqTrack[]) tracks.toArray()
        for(int i=0; i<seqScans.size(); i++) {
            SeqScan scan = seqScans.get(i)
            if (scan.seqTracks.size() != tracks.size()) {
                log.debug("size does not match ${scan.seqTracks.size()} ${tracks.size()}")
                continue
            }
            boolean match = true
            SeqTrack[] scanTracks = (SeqTrack[])scan.seqTracks.toArray()
            for(int j=0; j<scanTracks.length; j++) {
                long refId = scanTracks[j].id
                boolean hasPair = false
                for(int k=0; k<arrTracks.length; k++) {
                    if (arrTracks[k].id == refId)
                        hasPair = true
                }
                if (!hasPair) match = false
            }
            if (match) {
                log.debug("SeqScan Found !!")
                return scan
            }
        }
        return null
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
    private List<SeqTrack> getSeqTracks(Sample sample, SeqType seqType, List<String> header) {
        final String separator = "_s_"
        final String preNumber = "_"
        List<SeqTrack> tracks = new Vector<SeqTrack>()
        for(int i=0; i<header.size(); i++) {
            String line = header.get(i)
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
            SeqTrack seqTrack = getSeqTrack(sample, seqType, runName, lane)
            if (seqTrack == null) {
                log.debug("no SeqTrack for ${runName} ${lane}")
                continue
            } else {
                tracks << seqTrack
            }
        }
        return tracks
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
        return 0
    }

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
     * This function parses header of a bam file.
     * It is intended to provide a list of run names and
     * lane labels of lanes that contributed to this bam file.
     *
     * This function knows bam files header tags.
     *
     * @param bamFile input file
     * @return list of strings, each string contains one run_lane id
     */
    private List<String> parseBamHeader(File bamFile) {
        final String lineStart = "@RG"
        final String startToken = "ID:"
        // get the header
        String text = getBamFileHeader(bamFile)
        // select lines starting with @RG
        List<String> lines = new Vector<String>()
        text.eachLine {String line ->
            if (line.startsWith(lineStart)) {
                List<String> tokens = line.tokenize("\t")
                for(int i=0; i<tokens.size(); i++) {
                    String token = tokens.get(i)
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
                for(int i=0; i<tokens.size(); i++) {
                    String token = tokens.get(i)
                    if (token.startsWith(nameTag)) {
                        progName = token.substring(3)
                    }
                    if (token.startsWith(versionTag)) {
                        progVersion = token.substring(3)
                    }
                }
            }
        }
        AlignmentParams alignParams = AlignmentParams.findByProgramNameAndProgramVersion(progName, progVersion)
        if (alignParams == null) {
            alignParams = new AlignmentParams(
                    programName: progName,
                    programVersion: progVersion
                    )
            alignParams.save()
        }
        alignParams.refresh()
        return alignParams
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
