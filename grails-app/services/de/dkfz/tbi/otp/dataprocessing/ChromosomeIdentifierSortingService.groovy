package de.dkfz.tbi.otp.dataprocessing

/**
 * sort the chromosome identifier
 * the sorting is like this: 1..22 X Y M * 23...1000 A..V
 */
class ChromosomeIdentifierSortingService {

    // the sorting methods are not really part of the service, they also have to be in the closure -> how to do this
    //use enum method
    final int CHROMOSOME_SIZE = Chromosomes.numberOfNumericChromosomes()

    def listComparator = [ compare:
        {identifier1, identifier2 ->
            if (identifier1.isInteger() && identifier2.isInteger()) {
                return compareTwoInteger(identifier1, identifier2)
            } else if (identifier1.isInteger() && !(identifier2.isInteger())) {
                return compareIntegerWithNotInteger(identifier1, identifier2)
            } else if (!(identifier1.isInteger()) && identifier2.isInteger()) {
                return compareNotIntegerWithInteger(identifier1, identifier2)
            } else {
                return compareTwoNotInteger(identifier1, identifier2)
            }
        }
    ] as Comparator

    private int compareTwoInteger(Object identifier1, Object identifier2) {
        Integer identifierAsInteger1 = identifier1.toInteger()
        Integer identifierAsInteger2 = identifier2.toInteger()
        return identifierAsInteger1.intValue()-identifierAsInteger2.intValue()
    }

    private int compareIntegerWithNotInteger(Object identifier1, Object identifier2) {
        Integer identifierAsInteger1 = identifier1.toInteger()
        String identifierAsString2 = String.valueOf(identifier2).toUpperCase()
        if(identifierAsInteger1 <= CHROMOSOME_SIZE) {
            return -1
        }else if (identifierAsString2 == Chromosomes.CHR_X || identifierAsString2 == Chromosomes.CHR_Y) {
            return 1
        }else if (!(identifierAsString2 == Chromosomes.CHR_X && identifierAsString2 == Chromosomes.CHR_Y)) {
            return -1
        }
    }

    private int compareNotIntegerWithInteger(Object identifier1, Object identifier2) {
        String identifierAsString1 = String.valueOf(identifier1).toUpperCase()
        Integer identifierAsInteger2 = identifier2.toInteger()
        if (identifierAsInteger2 <= CHROMOSOME_SIZE) {
            return 1
        } else if (identifierAsString1 == Chromosomes.CHR_X || identifierAsString1 == Chromosomes.CHR_Y) {
            return -1
        } else if (!(identifierAsString1 == Chromosomes.CHR_X && identifierAsString1 == Chromosomes.CHR_Y)) {
            return 1
        }
    }

    private int compareTwoNotInteger(Object identifier1, Object identifier2) {
        String identifierAsString1 = String.valueOf(identifier1).toUpperCase()
        String identifierAsString2 = String.valueOf(identifier2).toUpperCase()
        if (identifierAsString1 == Chromosomes.CHR_X) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_X) {
            return 1
        } else if (identifierAsString1 == Chromosomes.CHR_Y) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_Y) {
            return 1
        } else if (identifierAsString1 == Chromosomes.CHR_M) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_M) {
            return 1
        } else {
            return identifierAsString1.compareTo(identifierAsString2)
        }
    }

    public Map<String, List> sort(Map<String, List> changedIdentifierCoverageData) {
        Map sortedIdentifierCoverageData = new TreeMap<String, List> (listComparator)
        sortedIdentifierCoverageData.putAll(changedIdentifierCoverageData)
        return sortedIdentifierCoverageData
    }
}
