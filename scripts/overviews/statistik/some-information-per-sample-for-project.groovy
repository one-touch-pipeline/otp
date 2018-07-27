package overviews.statistik

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*


/**
 * Creates an overview of used seqPlatform and libraryPreparationKit and nreads per sample for the given Projects and SeqTypes.
 * The sample is defined by project name, pid, sample type, and seqType.
 * For each it shows all different seqPlatform and libraryPreparationKit, separated by semicolon.
 * Furthermore it shows all nReads values, separated by semicolon.
 */

List<Project> projects = """
""".split('\n')*.trim().findAll().collect {
    CollectionUtils.exactlyOneElement(Project.findAllByName(it))
}

List<SeqType> seqTypes = [
        SeqType.wholeGenomeBisulfitePairedSeqType,
        SeqType.wholeGenomeBisulfiteTagmentationPairedSeqType
]



println SeqTrack.createCriteria().list {
    sample {
        individual {
            'in'('project', projects)
        }
    }
    'in'('seqType', seqTypes)
}.groupBy { SeqTrack seqTrack ->
    [
            seqTrack.project.name,
            seqTrack.individual.pid,
            seqTrack.sampleType.name,
            seqTrack.seqType.toString(),
    ].join(',')
}.collect { String key, List<SeqTrack> seqTracks ->
    [
            key,
            seqTracks*.seqPlatform*.toString().unique().sort().join(';'),
            seqTracks*.libraryPreparationKit*.name.unique().sort().join(';'),
            DataFile.findAllBySeqTrackInList(seqTracks)*.nReads.join(';'),
    ].join(',')
}.sort().join('\n')
