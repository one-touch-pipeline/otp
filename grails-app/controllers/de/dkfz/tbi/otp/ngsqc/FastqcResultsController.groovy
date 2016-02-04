package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*

class RenderFileCommand {
    Long id
    String withinZipPath
}

class FastqcResultsController {

    def fastqcResultsService
    def fastqcDataFilesService

    def show() {
        long id = params.id as long
        DataFile dataFile = DataFile.get(id)
        if (!dataFile || !fastqcResultsService.isFastqcAvailable(dataFile)) {
            response.sendError(404)
            return
        }
        FastqcProcessedFile fastqc = fastqcResultsService.fastqcProcessedFile(dataFile)
        List<FastqcModuleStatus> modules = fastqcResultsService.moduleStatusForDataFile(fastqc)
        Map<String, String> moduleStatus = [:]
        Map<String, String> moduleText = [:]
        modules.each {
            moduleStatus.put(it.module.identifier, (it.status as String).toLowerCase())
            moduleText.put(it.module.identifier, it.module.name)
        }
        return [
            id: dataFile.id,
            pid: dataFile.individual.displayName,
            runName: dataFile.run.name,
            laneId: dataFile.seqTrack.laneId,
            mateNumber: dataFile.mateNumber,
            fileName: dataFile.fileName,
            moduleStatus: moduleStatus,
            moduleText: moduleText,
            basicStats: fastqcResultsService.basicStatisticsForDataFile(fastqc),
            kmerContent: fastqcResultsService.kmerContentForDataFile(fastqc),
            overrepSeq: fastqcResultsService.overrepresentedSequencesForDataFile(fastqc)
        ]
    }

    /**
     * All images and flat text files from fastqc are zipped.
     * So this closure is used to get content to be rendered by the browser from within the fastqc zip file
     */
    def renderFile(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        DataFile dataFile = DataFile.get(cmd.id)
        String zipPath = fastqcDataFilesService.fastqcOutputFile(dataFile)
        response.outputStream << fastqcDataFilesService.getInputStreamFromZip(zipPath, cmd.withinZipPath)
        response.outputStream.flush()
    }
}

