package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.job.processing.ProcessingException

/**
 *
 */
class LibraryPreparationKitService {

    /**
     * look up the {@link LibraryPreparationKit} by its name and by its aliases ({@link LibraryPreparationKitSynonym})
     *
     * @param nameOrAlias the name used for look up
     * @return the found {@link LibraryPreparationKit} or <code>null</code>
     */
    public LibraryPreparationKit findLibraryPreparationKitByNameOrAlias(String nameOrAlias) {
        notNull(nameOrAlias, "the input 'nameOrAlias' is null")
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.findByName(nameOrAlias)
        if (!libraryPreparationKit) { //not found by name, try aliases
            libraryPreparationKit = LibraryPreparationKitSynonym.findByName(nameOrAlias)?.libraryPreparationKit
        }
        return libraryPreparationKit
    }

    /**
     * Validates if there is only one {@link LibraryPreparationKit}s for one {@link Sample}.
     * Checks if the kit for the new lane coincides with the preliminary kits.
     *
     * For one sample there should be one, and only one, kit.
     */
    public void validateLibraryPreparationKit(Sample sample, LibraryPreparationKit newLibraryPreparationKit) {
        notNull(sample, "The input sample of the method validateLibraryPreparationKit is null")
        notNull(newLibraryPreparationKit, "The input newLibraryPreparationKit of the method validateLibraryPreparationKit is null")
        SeqType exomeSeqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, "PAIRED")
        List<LibraryPreparationKit> oldLibraryPreparationKits = ExomeSeqTrack.findAllBySampleAndSeqType(sample, exomeSeqType)*.libraryPreparationKit
        List<LibraryPreparationKit> distinctOldKits = oldLibraryPreparationKits.unique().findAll()
        // If this is the first lane for this sample, no conflict is possible, happy return!
        if (distinctOldKits.empty) {
            return
        }
        // If there is one and ONLY one old library preparation kit, it must be the same as the new one
        else if (distinctOldKits.size() == 1 && distinctOldKits.first() == newLibraryPreparationKit) {
            return
        }
        // If the new kit and old kit(s) don't match throw an exception
        else {
            throw new ProcessingException("The newly added Kit ${newLibraryPreparationKit} conflicts with the already existing kits of sample ${sample} : ${distinctOldKits}")
        }
    }


    /**
     * The missing kits of old lanes of a {@link Sample} are inferred from new lanes of the same sample.
     * This is possible, since the different lanes of one sample should be prepared with the same {@link LibraryPreparationKit}.
     */
    public void inferKitInformationForOldLaneFromNewLane(RunSegment runSegment) {
        notNull(runSegment, "The input of the method inferKitInformationForOldLaneFromNewLane is null")
        //get all DataFiles for the RunSegment, get the SeqTracks of them, filter out null elements of the list and make the result unique
        List<SeqTrack> seqTracksPerSegment = DataFile.findAllByRunSegment(runSegment)*.seqTrack.findAll().unique()
        SeqType exomeSeqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, "PAIRED")
        List<ExomeSeqTrack> exomeSeqTracksPerSegment = seqTracksPerSegment.findAll { it.seqType == exomeSeqType }
        exomeSeqTracksPerSegment.each { ExomeSeqTrack track ->
            List<ExomeSeqTrack> exomeSeqTracksPerSample = ExomeSeqTrack.findAllBySampleAndSeqType(track.sample, exomeSeqType)
            List<ExomeSeqTrack> reliableExomeSeqTracks = exomeSeqTracksPerSample.findAll { it.kitInfoReliability == InformationReliability.KNOWN }
            List<LibraryPreparationKit> reliableKits = reliableExomeSeqTracks*.libraryPreparationKit.unique()
            if (!reliableKits.empty) {
                isTrue(reliableKits.size() == 1, "There is more than one explicitly defined library preparation kit for sample ${track.sample}")
                LibraryPreparationKit inferredKit = reliableKits.first()
                exomeSeqTracksPerSample.findAll { it.libraryPreparationKit == null }.each {
                    it.libraryPreparationKit = inferredKit
                    it.kitInfoReliability = InformationReliability.INFERRED
                    it.save()
                }
            } else {
                //nothing can be inferred if there is no kit available from at least one lane of the sample
                log.debug("There is no library preparation kit available for sample " + track.sample)
            }
        }
    }

}
