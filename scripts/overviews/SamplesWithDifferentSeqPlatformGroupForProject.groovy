import de.dkfz.tbi.otp.ngsdata.*

String projectName = ''

assert projectName : 'Please select a project'

List<SeqType> seqTypes = [
        SeqType.exomePairedSeqType,
        SeqType.wholeGenomePairedSeqType,
]

println "sample with different seqplatformgroups"
List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
    sample {
        individual {
            project {
                eq('name', projectName)
            }
        }
    }
    'in'('seqType', seqTypes)
}

println seqTracks.collect {
    [[it.individual.pid, it.sampleType.name, it.seqType.name, it.seqType.libraryLayout], it.seqPlatformGroup?.name]
}.unique().groupBy {
    it[0]
}.findAll { key, value ->
    value.size() > 1
}.collect { key, value ->
    "${key.join(' ')}   ${value*.get(1).join(',')}"
}.sort().join('\n')
