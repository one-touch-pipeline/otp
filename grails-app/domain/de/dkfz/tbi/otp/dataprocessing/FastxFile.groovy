package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Class for storing info regarding fastx files.
 * - A fastx file belongs to one DataFile
 * - A fastx file belongs to one SeqTrack
 */
class FastxFile {

    enum FastxFileType {
        StatsText,
        QualityImage,
        NucleotideDistributionImage,
        FastQ
    }

    FastxFileType type

    static belongsTo = [
        seqTrack: SeqTrack,
        dataFile: DataFile
    ]
}
