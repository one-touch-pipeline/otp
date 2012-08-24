package de.dkfz.tbi.otp.dataprocessing.common

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This is a helper class for dataprocessing related classes and scripts.
 * and should not be used outside of this scope! Its task is to collect
 * different data from a given input object. Works as a kind of shortcut.
 */
class RuntimeInfoCollection {

    @Autowired
    ConfigService configService

    SeqTrack seqTrack
    Run run
    Sample sample
    Individual individual
    Project project
    Realm realm

    public RuntimeInfoCollection(DataFile df) {
        this(df.seqTrack)
    }

    public RuntimeInfoCollection(SeqTrack seq) {
        if (configService == null) {
            configService = new ConfigService()
        }
        seqTrack = seq
        run = seq.run
        sample = seq.sample
        individual = sample.individual
        project = individual.project
        realm = configService.getRealm(project, Realm.OperationType.DATA_PROCESSING)
    }
}