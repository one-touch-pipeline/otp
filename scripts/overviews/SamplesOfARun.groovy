//Overview of samples of a run

//Show for a run all samples with SeqType and creation date grouped by run segment

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*



Run run = Run.findByName("140819_SN7001149_0212_BC4H1BACXX")

List output = []
def segments = RunSegment.findAllByRun(run).sort{it.id}
segments.each { segment ->
    output << "segment: ${segment.id}"
    def dataFiles = DataFile.findAllByRunSegment(segment)
    output << dataFiles.findAll{it?.seqTrack}.collect { "${it.dateCreated.format('yyyy-MM-dd')}  ${it.seqTrack.sample} ${it.seqTrack.seqType} ${SampleIdentifier.findAllBySample(it.seqTrack.sample)}" }.sort().unique().join("\n")
    output << ""
}

println output.join('\n')
''
