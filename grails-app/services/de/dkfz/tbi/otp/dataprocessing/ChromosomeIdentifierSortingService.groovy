/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import static org.springframework.util.Assert.notNull

/**
 * sort the chromosome identifier
 * the sorting is like this: 1..22 X Y M * 23...1000 A..V
 */
@Transactional
class ChromosomeIdentifierSortingService {

    static final int CHROMOSOME_SIZE = Chromosomes.numberOfNumericChromosomes()

    Closure listComparator = { String identifier1, String identifier2 ->
        if (identifier1.isInteger() && identifier2.isInteger()) {
            return compareTwoInteger(identifier1, identifier2)
        } else if (identifier1.isInteger() && !(identifier2.isInteger())) {
            return compareIntegerWithNotInteger(identifier1, identifier2)
        } else if (!(identifier1.isInteger()) && identifier2.isInteger()) {
            return compareNotIntegerWithInteger(identifier1, identifier2)
        }
        return compareTwoNotInteger(identifier1, identifier2)
    }

    private int compareTwoInteger(String identifier1, String identifier2) {
        Integer identifierAsInteger1 = identifier1.toInteger()
        Integer identifierAsInteger2 = identifier2.toInteger()
        return identifierAsInteger1.intValue() - identifierAsInteger2.intValue()
    }

    private int compareIntegerWithNotInteger(String identifier1, String identifier2) {
        Integer identifierAsInteger1 = identifier1.toInteger()
        String identifierAsString2 = String.valueOf(identifier2).toUpperCase()
        if (identifierAsInteger1 <= CHROMOSOME_SIZE) {
            return -1
        }
        return identifierAsString2 == Chromosomes.CHR_X.chr || identifierAsString2 == Chromosomes.CHR_Y.chr ||
                identifierAsString2 == Chromosomes.CHR_M.chr ? 1 : -1
    }

    private int compareNotIntegerWithInteger(String identifier1, String identifier2) {
        return -(compareIntegerWithNotInteger(identifier2, identifier1))
    }

    private int compareTwoNotInteger(String identifier1, String identifier2) {
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
        }
        return identifierAsString1 <=> identifierAsString2
    }

    /**
     * Creates a new list with the elements of the given collection sorted by {@link #listComparator}
     * @param chromosomeIdentifier the chromosome identifiers to sort
     * @return the sorted chromosome identifiers
     */
    List<String> sortIdentifiers(Collection<String> chromosomeIdentifiers) {
        notNull(chromosomeIdentifiers, "the input for the method sortIdentifiers is null")
        return chromosomeIdentifiers.sort(listComparator)
    }
}
