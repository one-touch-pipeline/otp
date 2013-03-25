package de.dkfz.tbi.otp.ngsdata

class FastqFilePathDataProviderController {

    def fileTypeService
    def lsdfFilesService

    def fetchFastqPaths(String pid, String sampleType, String seqType, String libraryLayout) {
        Individual individual = Individual.findByPid(pid)
        SampleType samplesType = SampleType.findByName(sampleType)
        Sample sample = Sample.findByIndividualAndSampleType(individual, samplesType)
        SeqType seqTypes = SeqType.findByNameAndLibraryLayout(seqType, libraryLayout)
        List<SeqTrack> seqTracks = SeqTrack.findAllBySampleAndSeqType(sample, seqTypes)
        List<String> vbpFullFilePaths = []
        seqTracks.each { SeqTrack seqTrack ->
            List dataFiles = DataFile.findAllBySeqTrack(seqTrack)
            for(DataFile dataFile : dataFiles) {
                if (fileTypeService.isGoodSequenceDataFile(dataFile)) {
                    vbpFullFilePaths << lsdfFilesService.getFileViewByPidPath(dataFile)
                }
            }
        }
        //or only withespaces ("") what is preferred?
        render(vbpFullFilePaths.join("\n"))
    }
}
