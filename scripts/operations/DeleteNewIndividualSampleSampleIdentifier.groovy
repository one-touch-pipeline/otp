/**
 * Delete NEW Individual/Sample/Sample Identifier
 * Only use this script if the domain object is totally new and no data is connected to it at all!
 */

import de.dkfz.tbi.otp.ngsdata.*

String pid = ''


Individual indi = Individual.findByPid(pid)
println "patient " +  indi

def samples = Sample.findAllByIndividual(indi)
println "samples " + samples

samples.each { sample ->
    def sampleIndis = SampleIdentifier.findAllBySample(sample)
    List<SeqTrack> seqTracks = SeqTrack.findAllBySample(sample)
    assert !seqTracks || DataFile.findAllBySeqTrackInList(seqTracks).isEmpty() : "DataFile loaded for ${sample}"
    println "sampleIndis " + sampleIndis
}


Individual.withTransaction() {
    samples.each { sample ->
        def sampleIndis = SampleIdentifier.findAllBySample(sample)
        sampleIndis.each { it.delete(flush: true) }
    }
    samples.each { it.delete(flush: true) }
    indi.delete(flush: true)
}



indi = Individual.findById(indi.id)
println "patient " +  indi

samples.each {
    println 'sample ' + Sample.findById(it.id)
}
''
