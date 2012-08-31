package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.JSON

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
        List<FastqcModuleStatus> modules = fastqcResultsService.moduleStatusForDataFile(dataFile)
        Map<String, String> moduleStatus = [:]
        Map<String, String> moduleText = [:]
        modules.each {
            moduleStatus.put(it.module.identifier, (it.status as String).toLowerCase())
            moduleText.put(it.module.identifier, it.module.name)
        }
        return [
            id: dataFile.id,
            fileName: dataFile.fileName,
            moduleStatus: moduleStatus,
            moduleText: moduleText,
            basicStats: fastqcResultsService.basicStatisticsForDataFile(dataFile),
            kmerContent: fastqcResultsService.kmerContentForDataFile(dataFile),
            overrepSeq: fastqcResultsService.overrepresentedSequencesForDataFile(dataFile)
        ]
    }

    /**
     * All images and flat text files from fastqc are zipped.
     * So this closure is used to get content to be rendered by the browser from within the fastqc zip file
     */
    def renderFromZip(String id, String withinZipPath) {
        DataFile dataFile = DataFile.get(id as long)
        String zipPath = fastqcDataFilesService.fastqcOutputFile(dataFile)
        response.outputStream << fastqcDataFilesService.getInputStreamFromZip(zipPath, withinZipPath)
        response.outputStream.flush()
    }
}

