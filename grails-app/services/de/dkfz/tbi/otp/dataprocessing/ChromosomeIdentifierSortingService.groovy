package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
/**
 * sort the chromosome identifier
 * the sorting is like this: 1..22 X Y M * 23...1000 A..V
 */
class ChromosomeIdentifierSortingService {

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
        if (identifierAsInteger1 <= CHROMOSOME_SIZE) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_X.chr || identifierAsString2 == Chromosomes.CHR_Y.chr ||
        identifierAsString2 == Chromosomes.CHR_M.chr) {
            return 1
        } else {
            return -1
        }
    }

    private int compareNotIntegerWithInteger(Object identifier1, Object identifier2) {
        return -(compareIntegerWithNotInteger(identifier2, identifier1))
    }

    private int compareTwoNotInteger(Object identifier1, Object identifier2) {
        String identifierAsString1 = String.valueOf(identifier1).toUpperCase()
        String identifierAsString2 = String.valueOf(identifier2).toUpperCase()
        if (identifierAsString1 == Chromosomes.CHR_X.chr) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_X.chr) {
            return 1
        } else if (identifierAsString1 == Chromosomes.CHR_Y.chr) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_Y.chr) {
            return 1
        } else if (identifierAsString1 == Chromosomes.CHR_M.chr) {
            return -1
        } else if (identifierAsString2 == Chromosomes.CHR_M.chr) {
            return 1
        } else {
            return identifierAsString1.compareTo(identifierAsString2)
        }
    }

    /**
     * Creates a new list with the elements of the given collection sorted by {@link #listComparator}
     * @param chromosomeIdentifier the chromosome identifiers to sort
     * @return the sorted chromosome identifiers
     */
    public List<String> sortIdentifiers(Collection<String> chromosomeIdentifiers) {
        notNull(chromosomeIdentifiers, "the input for the method sortIdentifiers is null")
        return chromosomeIdentifiers.sort(listComparator)
    }
}
