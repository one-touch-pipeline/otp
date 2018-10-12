
import de.dkfz.tbi.otp.ngsdata.*

String projectName = ''

//SeqType seqType = SeqTypeService.exomePairedSeqType
//SeqType seqType = SeqTypeService.wholeGenomePairedSeqType
//SeqType seqType = SeqTypeService.wholeGenomeBisulfitePairedSeqType
//SeqType seqType = SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
//SeqType seqType = SeqTypeService.rnaPairedSeqType
//SeqType seqType = SeqTypeService.rnaSingleSeqType
//SeqType seqType = SeqTypeService.chipSeqPairedSeqType

//---------------------------

List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
    sample {
        individual {
            project {
                eq('name', projectName)
            }
        }
    }
    eq('seqType', seqType)
}

println "found samples for ${projectName} ${seqType}"
println seqTracks.collect {
    "${it.individual.pid} ${it.sampleType.name} ${it.libraryPreparationKit?.name}"
}.unique().sort().join('\n')

println "\n-----------------------------"
println "lanes without libraryPreparationKit\n"
println seqTracks.findAll {
    it.libraryPreparationKit == null
}.collect {
    "${it.individual.pid} ${it.sampleType.name} ${it.run.name} ${it.laneId}"
}.unique().sort().join('\n')


