package de.dkfz.tbi.otp.ngsdata

class DataFileController {

    def lsdfFilesService

    def scaffold = DataFile

    def showDetails = {

        DataFile dataFile = DataFile.get(params.id)

        List<String> keys = new Vector<String>()
        List<String> values = new Vector<String>()

        keys << "file name"
        values << dataFile.fileName

        keys <<  "full path"
        values << lsdfFilesService.getFilePath(dataFile)

        keys << "view-by-pid name"
        values << dataFile.vbpFileName

        keys << "view-by-pid path"
        values << lsdfFilesService.getViewByPidPath(dataFile)

        return [keys: keys, values: values]
    }
}
