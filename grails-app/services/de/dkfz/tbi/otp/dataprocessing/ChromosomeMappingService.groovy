package de.dkfz.tbi.otp.dataprocessing

class ChromosomeMappingService {

    public static final Map<String, String> hg19_1_24 = createMappingWithPrefix("chr")

    public static final Map<String, String> thousandGenomes = createMappingWithPrefix("")

    private static Map<String, String> createMappingWithPrefix(String prefixPattern) {
        List<String> numericChromosomes = 1..22
        List<String> nonNumericChromosomes = ["X", "Y"]
        List<String> chromosomes = [numericChromosomes, nonNumericChromosomes].flatten()
        int chromosomeIndex = 1
        return chromosomes.collectEntries {
            [(prefixPattern + it): chromosomeIndex++]
        }
    }
}
