package de.dkfz.tbi.ngstools.bedUtils

abstract class TargetIntervalsFactory {

    static TargetIntervals create(String bedFilePath, List<String> referenceGenomeEntryNames) {
        return new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
    }
}
