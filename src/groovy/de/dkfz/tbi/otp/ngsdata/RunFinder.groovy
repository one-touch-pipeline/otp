package de.dkfz.tbi.otp.ngsdata

class RunFinder {

    String dataPath = "$BQ_ROOTPATH/ftp/"
    String project = null
    String type = null
    SeqCenter seqCenter = null
    boolean transfer = false
    String typeLimit = null

    public boolean updateMode = false

    public void setSeqType(String seqTypeDirName) {
        typeLimit = seqTypeDirName
    }

    public void findRuns(String basePath, String project, String dataPath) {
        this.project = project
        if (dataPath) {
            this.dataPath = dataPath
            transfer = true
        }
        File baseDir = buildDirectory("${basePath}/${project}/sequencing/")
        String[] seqTypeNames = baseDir.list()
        for(String seqTypeName in seqTypeNames) {
            if (validSeqType(seqTypeName)) {
                analyzeSeqType(baseDir.getPath(), seqTypeName)
            }
        }
    }

    private File buildDirectory(String path) {
        File dir = new File(path)
        if (!dir.canRead() || !dir.isDirectory()) {
            throw new DirectoryNotReadableException(path)
        }
        return dir
    }

    private boolean validSeqType(String seqTypeName) {
        if (typeLimit && seqTypeName != typeLimit) {
            return false
        }
        SeqType type = SeqType.findByDirName(seqTypeName)
        if (type) {
            this.type = type.dirName
            return true
        }
        return false
    }

    private void analyzeSeqType(String baseDir, String seqType) {
        String dirPath = baseDir + "/" + seqType
        File dir = buildDirectory(dirPath)
        String[] seqCenterNames = dir.list()
        println seqType
        for(String seqCenter in seqCenterNames) {
            if (validateSeqCenter(seqCenter)) {
                analyzeSeqCenter(dirPath, seqCenter)
            }
        }
    }

    private boolean validateSeqCenter(String dirName) {
        seqCenter = SeqCenter.findByDirName(dirName)
        return (boolean) seqCenter
    }

    private void analyzeSeqCenter(String baseDir, String centerDir) {
        String dirPath = baseDir + "/" + centerDir
        File dir = buildDirectory(dirPath)
        String[] runNames = dir.list()
        println "\t" + centerDir
        for(String runDirName in runNames) {
            registerRun(dirPath, runDirName)
        }
    }

    private void registerRun(String baseDir, String runDirName) {
        final String signature = "run"
        String path = dataPath
        if (transfer) {
            path = "${dataPath}/${project}/sequencing/${type}/${seqCenter.dirName}"
        }
        if (!runDirName.contains(signature)) {
            return
        }
        //if (!runDirName.contains("110826_SN169_0223_BD07H6ACXX")) {
        //    return
        //}
        String runName = runDirName.substring(3)
        if (updateMode) {
            if (Run.findByName(runName)) {
                return
            }
        }
        Run run = findOrCreateRun(runName)
        RunInitialPath initialPath = new RunInitialPath(
            dataPath: path,
            mdPath: baseDir,
            run: run
        )
        //initialPath.validate()
        //println initialPath.run
        //println initialPath.errors
        //println initialPath
        initialPath.save(flush: true)
        run.save(flush: true)
    }

    private Run findOrCreateRun(String runName) {
        Run run = Run.findByName(runName)
        if (run) {
            return run
        }
        return createRun(runName)
    }

    private Run createRun(String runName) {
        SeqPlatform platform = guessSeqPlatform(runName)
        Run run = new Run(
            name: runName,
            seqCenter: seqCenter,
            seqPlatform: platform,
            legacyRun: true
        )
        run.save()
        return run
    }

    private SeqPlatform guessSeqPlatform(String runName) {
        if (runName.contains("solid")) {
            return SeqPlatform.findByName("SOLiD")
        }
        return SeqPlatform.findByName("Illumina")
    }
}













