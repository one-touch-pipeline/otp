package de.dkfz.tbi.ngstools.coverageQc

import com.google.gson.Gson

/**
 * This class holds multiple ReferenceChromosome objects and facilitate searching them by key.
 */
 class Genome {

    private final HashMap<String, ReferenceChromosome> chromosomes = new HashMap<String, ReferenceChromosome>()

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
    void assertChromosome(String referenceName) {
        if (!containsChromosome(referenceName)) {
            ReferenceChromosome chr = new ReferenceChromosome(referenceName)
            putChromosome(referenceName, chr)
        }
    }

    /**
     * Export results to JSON string
     * @return
     */
     String genome2JSON() {
        Gson gson = new Gson()
        return gson.toJson(chromosomes)
    }
}
