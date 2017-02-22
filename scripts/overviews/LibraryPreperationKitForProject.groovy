import de.dkfz.tbi.otp.ngsdata.*

String projectName = ''

SeqType seqType = SeqType.exomePairedSeqType
//SeqType seqType = SeqType.wholeGenomePairedSeqType

println "sample with libraryPreparationKit"
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

