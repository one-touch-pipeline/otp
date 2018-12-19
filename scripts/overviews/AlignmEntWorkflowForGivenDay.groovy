import de.dkfz.tbi.otp.dataprocessing.*

//-----------
//input

int year = 2018
int month =12
int day = 17


//---------------------------
//work area

Date dateStart = new Date(year - 1900, month - 1, day)
Date dateEnd = dateStart.plus(1)

List<String> output = []

List<RoddyBamFile> bamFile = RoddyBamFile.findAllByDateCreatedBetween(dateStart, dateEnd)

output << "Alignment workflows started on: ${dateStart}"
output << "Count of files: ${bamFile.size()}"
output << "Files: "

output <<  [
        'otp id',
        'mwp identifier',
        'latest',
        'withdrawn',
        'qcTrafficLightStatus',
        'project',
        'individual',
        'sampleType',
        'seqTypeName',
        'libraryLayout',
        'bulk or single cell',
        'libraryPreparationKit',
        'referenceGenom',
        'is fasttrack',
        'dateCreated',
        'workDirectory',
].join('|')

output << bamFile.collect {
    [

            it.id,
            it.identifier,
            it.isMostRecentBamFile() ? 'latest':'',
            it.withdrawn ? 'withdrawn' : '',
            it.qcTrafficLightStatus,
            it.project,
            it.individual,
            it.sampleType,
            it.seqType.seqTypeName,
            it.seqType.libraryLayout,
            it.seqType.singleCell ? 'single cell' : 'bulk',
            it.mergingWorkPackage.libraryPreparationKit ?: '',
            it.referenceGenome,
            (it.processingPriority >= ProcessingPriority.FAST_TRACK.priority) ? 'FAST_TRACK' : 'NORMAL',
            it.dateCreated,
            it.workDirectory,
    ].join('|')
}.join('\n')


println output.join('\n')
