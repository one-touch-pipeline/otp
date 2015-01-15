package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException

class MetaDataRegistrationService {

    /**
    *
    * looks into directory pointed by mdPath of a Run object
    * and register files that could be meta-data files
    *
    * @param runId - database id or the Run object
    * @return true if at least one meta-data file was found, false otherwise
    */
    boolean registerInputFiles(long runId) {
        Run run = Run.get(runId)
        List<RunSegment> segments =
            RunSegment.findAllByRunAndMetaDataStatus(run, RunSegment.Status.BLOCKED)
        int nFiles = 0
        for (RunSegment segment in segments) {
            nFiles += registerInputFilesForPath(segment)
            segment.metaDataStatus = RunSegment.Status.PROCESSING
            segment.save(flush: true)
        }
        if (segments.size() > 1) {
            run.multipleSource = true
        }
        run.save(flush: true)
        return (nFiles > 0) ? true : false
    }

    private int registerInputFilesForPath(RunSegment path) {
        File dir = getMetaDataDirectory(path.mdPath, path.run.name)
        return processDirectory(path, dir)
    }

    private File getMetaDataDirectory(String path, String runName) {
       List<String> separators = ["/run", "/"]
       List<String> paths = separators.collect {path + it + runName}
       for (String runDir in paths) {
           File dir = new File(runDir)
           if (dir.canRead() && dir.isDirectory()) {
               return dir
           }
       }
       throw new ProcessingException("None of following directories can be accessed: ${paths}")
    }

    private int processDirectory(RunSegment segment, File dir) {
        int counter = 0
        MetaDataFile metaDataFile
        List<String> fileNames = dir.list()
        for (String fileName in fileNames) {
            if (fileBlacklisted(fileName)) {
                continue
            }
            if (!fileName.endsWith(".tsv")) {
                continue
            }
            if (fileName.contains("fastq") || fileName.contains("align")) {
                if (isFileRegistered(dir.absolutePath, fileName)) {
                    throw new MetaDataFileDuplicationException(segment.run.name, fileName)
                }
                metaDataFile = new MetaDataFile(
                    fileName: fileName,
                    filePath: dir.absolutePath,
                    runSegment: segment,
                    used: false
                )
                metaDataFile.validate()
                metaDataFile.save(flush: true)
                counter++
            }
        }
        return counter
    }

    private boolean fileBlacklisted(String fileName) {
        String[] blacklist = ["wrong", "old"]
        for (String name in blacklist) {
            if (fileName.contains(name)) {
                return true
            }
        }
        return false
    }

    private boolean isFileRegistered(String mdPath, String fileName) {
        MetaDataFile existingFile =
            MetaDataFile.findByFilePathAndFileName(mdPath, fileName)
        return (boolean)existingFile
    }
}
