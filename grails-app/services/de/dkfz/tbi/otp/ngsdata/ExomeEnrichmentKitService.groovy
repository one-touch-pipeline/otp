package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.job.processing.ProcessingException

/**
 *
 */
class ExomeEnrichmentKitService {

    /**
     * look up the {@link ExomeEnrichmentKit} by its name and by its aliases ({@link ExomeEnrichmentKitSynonym})
     *
     * @param nameOrAlias the name used for look up
     * @return the found {@link ExomeEnrichmentKit} or <code>null</code>
     */
    public ExomeEnrichmentKit findExomeEnrichmentKitByNameOrAlias(String nameOrAlias) {
        notNull(nameOrAlias, "the input 'nameOrAlias' is null")
        ExomeEnrichmentKit exomeEnrichmentKit = ExomeEnrichmentKit.findByName(nameOrAlias)
        if (!exomeEnrichmentKit) { //not found by name, try aliases
            exomeEnrichmentKit = ExomeEnrichmentKitSynonym.findByName(nameOrAlias)?.exomeEnrichmentKit
        }
        return exomeEnrichmentKit
    }

    /**
     * Validates if there is only one {@link ExomeEnrichmentKit}s for one {@link Sample}.
     * Checks if the kit for the new lane coincides with the preliminary kits.
     *
     * For one sample there should be one, and only one, kit.
     */
    public void validateExomeEnrichmentKit(Sample sample, ExomeEnrichmentKit newExomeEnrichmentKit) {
        notNull(sample, "The input sample of the method validateExomeEnrichmentKit is null")
        notNull(newExomeEnrichmentKit, "The input newExomeEnrichmentKit of the method validateExomeEnrichmentKit is null")
        SeqType exomeSeqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, "PAIRED")
        List<ExomeEnrichmentKit> oldExomeEnrichmentKits = ExomeSeqTrack.findAllBySampleAndSeqType(sample, exomeSeqType)*.exomeEnrichmentKit
        List<ExomeEnrichmentKit> distinctOldKits = oldExomeEnrichmentKits.unique().findAll()
        // If this is the first lane for this sample, no conflict is possible, happy return!
        if (distinctOldKits.empty) {
            return
        }
        // If there is one and ONLY one old enrichment kit, it must be the same as the new one
        else if (distinctOldKits.size() == 1 && distinctOldKits.first() == newExomeEnrichmentKit) {
            return
        }
        // If the new kit and old kit(s) don't match throw an exception
        else {
            throw new ProcessingException("The newly added Kit ${newExomeEnrichmentKit} conflicts with the already existing kits of sample ${sample} : ${distinctOldKits}")
        }
    }


    /**
     * The missing kits of old lanes of a {@link Sample} are inferred from new lanes of the same sample.
     * This is possible, since the different lanes of one sample should be prepared with the same {@link ExomeEnrichmentKit}.
     */
    public void inferKitInformationForOldLaneFromNewLane(RunSegment runSegment) {
        notNull(runSegment, "The input of the method inferKitInformationForOldLaneFromNewLane is null")
        List<SeqTrack> seqTracksPerSegment = DataFile.findAllByRunSegment(runSegment)*.seqTrack.unique()
        SeqType exomeSeqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, "PAIRED")
        List<ExomeSeqTrack> exomeSeqTracksPerSegment = seqTracksPerSegment.findAll { it.seqType == exomeSeqType }
        exomeSeqTracksPerSegment.each { ExomeSeqTrack track ->
            List<ExomeSeqTrack> exomeSeqTracksPerSample = ExomeSeqTrack.findAllBySampleAndSeqType(track.sample, exomeSeqType)
            List<ExomeSeqTrack> reliableExomeSeqTracks = exomeSeqTracksPerSample.findAll { it.kitInfoReliability == InformationReliability.KNOWN }
            List<ExomeEnrichmentKit> reliableKits = reliableExomeSeqTracks*.exomeEnrichmentKit.unique()
            if (!reliableKits.empty) {
                isTrue(reliableKits.size() == 1, "There is more than one explicitly defined exome enrichment kit for sample ${track.sample}")
                ExomeEnrichmentKit inferredKit = reliableKits.first()
                exomeSeqTracksPerSample.findAll { it.exomeEnrichmentKit == null }.each {
                    it.exomeEnrichmentKit = inferredKit
                    it.kitInfoReliability = InformationReliability.INFERRED
                    it.save()
                }
            } else {
                //nothing can be inferred if there is no kit available from at least one lane of the sample
                log.debug("There is no exome enrichtment kit available for sample " + track.sample)
            }
        }
    }

}
