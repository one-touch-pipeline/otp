package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMSequenceRecord
import com.google.gson.Gson

/**
 * This class holds multiple ReferenceChromosome objects and facilitate searching them by key.
 */
class Genome {

    private final Map<String, ReferenceChromosome> chromosomes = new LinkedHashMap<String, ReferenceChromosome>()

    /**
     * Check whether genome contain chromosome with given name.
     * @param name
     * @return
     */
    boolean containsChromosome(String name) {
        return chromosomes.containsKey(name)
    }

    void putChromosome(String key, ReferenceChromosome chr) {
        chromosomes.put(key, chr)
    }

    ReferenceChromosome getChromosome(String name) {
        return chromosomes.get(name)
    }

    /**
     * Check presence of chromosome and if not present initialize.
     * @param referenceName
     */
    void ensureChromosomeExists(String referenceName) {
        if (!containsChromosome(referenceName)) {
            ReferenceChromosome chr = new ReferenceChromosome(referenceName)
            putChromosome(referenceName, chr)
        }
    }

    void init(List<SAMSequenceRecord> chrList) {
        for (SAMSequenceRecord chr : chrList){
            String chrName = chr.getSequenceName()
            long chrLength = chr.getSequenceLength() as long
            ReferenceChromosome ref = new ReferenceChromosome(chrName, chrLength)
            putChromosome(chrName, ref)
        }
    }

    /**
     * Make new chromosome named ALL where all the numbers are summed up and add it to chromosomes
     */
    void sumUpAll(){
        ReferenceChromosome all = new ReferenceChromosome("ALL")
        for (ReferenceChromosome chr:chromosomes.values()) {
            all.referenceLength += chr.referenceLength
            all.duplicateR1 += chr.duplicateR1
            all.duplicateR2 += chr.duplicateR2
            all.properPairStrandConflict += chr.properPairStrandConflict
            all.referenceAgreement += chr.referenceAgreement
            all.referenceAgreementStrandConflict += chr.referenceAgreementStrandConflict
            all.mappedQualityLongR1 += chr.mappedQualityLongR1
            all.mappedQualityLongR2 += chr.mappedQualityLongR2
            all.qcBasesMapped += chr.qcBasesMapped
            all.mappedLowQualityR1 += chr.mappedLowQualityR1
            all.mappedLowQualityR2 += chr.mappedLowQualityR2
            all.mappedShortR1 += chr.mappedShortR1
            all.mappedShortR2 += chr.mappedShortR2
            all.notMappedR1 += chr.notMappedR1
            all.notMappedR2 += chr.notMappedR2

            all.endReadAberration += chr.endReadAberration
            all.totalReadCounter += chr.totalReadCounter
            all.qcFailedReads += chr.qcFailedReads
            all.duplicates += chr.duplicates
            all.totalMappedReadCounter += chr.totalMappedReadCounter
            all.pairedInSequencing += chr.pairedInSequencing
            all.pairedRead2 += chr.pairedRead2
            all.pairedRead1 += chr.pairedRead1
            all.properlyPaired += chr.properlyPaired
            all.withItselfAndMateMapped += chr.withItselfAndMateMapped
            all.withMateMappedToDifferentChr += chr.withMateMappedToDifferentChr
            all.withMateMappedToDifferentChrMaq += chr.withMateMappedToDifferentChrMaq
            all.singletons += chr.singletons
        }
        putChromosome("ALL", all)
    }

    /**
     * Export results to JSON string
     * @return
     */
    String genome2JSON() {
        // just to make it easier to debug
        groovy.json.JsonOutput.prettyPrint(new Gson().toJson(chromosomes))
//        new Gson().toJson(chromosomes)
    }
}
