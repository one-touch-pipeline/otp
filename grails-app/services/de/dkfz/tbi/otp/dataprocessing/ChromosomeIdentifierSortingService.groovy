package de.dkfz.tbi.otp.dataprocessing

class ChromosomeIdentifierSortingService {

    /**
     * Override the compare function to sort the chromosome identifier
     * the sorting is like this: 1..22 X Y M * 23...1000 A..V
     */
    Comparator<Object> sortIdentifier = new Comparator<Object>() {

        final int CHROMOSOME_SIZE = 23
        final String CHROMOSOME_X = "X"
        final String CHROMOSOME_Y = "Y"
        final String CHROMOSOME_M = "M"

        @Override
        public int compare(Object identifier1, Object identifier2) {
            if (identifier1.isInteger() && identifier2.isInteger()) {
                return compareTwoInteger(identifier1, identifier2)
            } else if (identifier1.isInteger() && !(identifier2.isInteger())) {
                return compareIntegerWithNotInteger(identifier1, identifier2)
            } else if (!(identifier1.isInteger()) && identifier2.isInteger()) {
                return compareNotIntegerWithInteger(identifier1, identifier2)
            } else {
                return compareTwoIntegerIdentifier(identifier1, identifier2)
            }
        }

        public int compareTwoInteger(Object identifier1, Object identifier2){
            Integer identifierAsInteger1 = identifier1.toInteger()
            Integer identifierAsInteger2 = identifier2.toInteger()
            return identifierAsInteger1.intValue()-identifierAsInteger2.intValue()
        }

        public int compareIntegerWithNotInteger(Object identifier1, Object identifier2){
            Integer identifierAsInteger1 = identifier1.toInteger()
            String identifierAsString2 = String.valueOf(identifier2).toUpperCase()
            if(identifierAsInteger1 < CHROMOSOME_SIZE) {
                return -1
            }else if (identifierAsString2 == CHROMOSOME_X || identifierAsString2 == CHROMOSOME_Y) {
                return 1
            }else if (!(identifierAsString2 == CHROMOSOME_X && identifierAsString2 == CHROMOSOME_Y)) {
                return -1
            }
        }

        public int compareNotIntegerWithInteger(Object identifier1, Object identifier2){
            String identifierAsString1 = String.valueOf(identifier1).toUpperCase()
            Integer identifierAsInteger2 = identifier2.toInteger()
            if (identifierAsInteger2 < CHROMOSOME_SIZE) {
                return 1
            } else if (identifierAsString1 == CHROMOSOME_X || identifierAsString1 == CHROMOSOME_Y) {
                return -1
            } else if (!(identifierAsString1 == CHROMOSOME_X && identifierAsString1 == CHROMOSOME_Y)) {
                return 1
            }
        }

        public int compareTwoIntegerIdentifier(Object identifier1, Object identifier2){
            String identifierAsString1 = String.valueOf(identifier1).toUpperCase()
            String identifierAsString2 = String.valueOf(identifier2).toUpperCase()
            if (identifierAsString1 == CHROMOSOME_X) {
                return -1
            } else if (identifierAsString2 == CHROMOSOME_X) {
                return 1
            } else if (identifierAsString1 == CHROMOSOME_Y) {
                return -1
            } else if (identifierAsString2 == CHROMOSOME_Y) {
                return 1
            } else if (identifierAsString1 == CHROMOSOME_M) {
                return -1
            } else if (identifierAsString2 == CHROMOSOME_M) {
                return 1
            } else {
                return identifierAsString1.compareTo(identifierAsString2)
            }
        }
    }

    public Map<String, List> sort(Map<String, List> changedIdentifierCoverageData) {
        Map sortedIdentifierCoverageData = new TreeMap<String, List>(sortIdentifier)
        sortedIdentifierCoverageData.putAll(changedIdentifierCoverageData)
        return sortedIdentifierCoverageData
    }
}
