package de.dkfz.tbi.otp.ngsdata

class DataFileController {

    def lsdfFilesService
    def scaffold = DataFile

    def showDetails = {

        DataFile dataFile = DataFile.get(params.id)
        List<MetaDataEntry> entries = MetaDataEntry.findAllByDataFile(dataFile, [sort:"key.id"])

        List<String> keys = new Vector<String>()
        List<String> values = new Vector<String>()

        keys <<  "fullPath"
        values << lsdfFilesService.getFileFinalPath(dataFile)

        keys << "view-by-pid path"
        values << lsdfFilesService.getFileViewByPidPath(dataFile)

        return [dataFile: dataFile, entries: entries, values: values]
    }
}
