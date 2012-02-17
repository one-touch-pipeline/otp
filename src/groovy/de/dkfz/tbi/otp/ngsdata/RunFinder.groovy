package de.dkfz.tbi.otp.ngsdata

class RunFinder {

    SeqCenter seqCenter = null

    public void findRuns(String basePath) {
        File baseDir = buildDirectory(basePath)
        String[] seqTypeNames = baseDir.list()
        for(String seqTypeName in seqTypeNames) {
            if (validSeqType(seqTypeName)) {
                analyzeSeqType(basePath, seqTypeName)
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
        SeqType type = SeqType.findByDirName(seqTypeName)
        return (boolean)type
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
        final String dataPath = "/icgc/ftp/"
        String dir = buildDirectory(baseDir + "/" + runDirName)
        if (!runDirName.contains(signature)) {
            return
        }
        String runName = runDirName.substring(3)
        Run run = Run.findByName(runName)
        if (run != null) {
            println "Duplicated run ${runName}"
            return
        }
        SeqPlatform platform = guessSeqPlatform(runName)
        run = new Run(
            name: runName,
            seqCenter: seqCenter,
            seqPlatform: platform,
            dataPath : dataPath,
            mdPath : baseDir
        )
        run.save(flush: true)
    }

    private SeqPlatform guessSeqPlatform(String runName) {
        if (runName.contains("solid")) {
            return SeqPlatform.findByName("SOLiD")
        }
        return SeqPlatform.findByName("Illumina")
    }
}













