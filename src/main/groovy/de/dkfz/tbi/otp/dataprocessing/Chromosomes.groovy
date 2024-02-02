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

/**
 * ENUM for the chromosome names
 */
enum Chromosomes {
    CHR_1("1"), CHR_2("2"), CHR_3("3"), CHR_4("4"), CHR_5("5"), CHR_6("6"), CHR_7("7"), CHR_8("8"), CHR_9("9"), CHR_10("10"), CHR_11("11"),
    CHR_12("12"), CHR_13("13"), CHR_14("14"), CHR_15("15"), CHR_16("16"), CHR_17("17"), CHR_18("18"), CHR_19("19"), CHR_20("20"),
    CHR_21("21"), CHR_22("22"), CHR_X("X"), CHR_Y("Y"), CHR_M("M")

    final String chr

    private Chromosomes(String chr) {
        this.chr = chr
    }

    String getAlias() {
        return chr
    }

    static List<Chromosomes> numeric() {
        List<Chromosomes> numericChromosomes = []
        Chromosomes.values().each { Chromosomes chromosome ->
            if (chromosome.chr.isInteger()) {
                numericChromosomes.add(chromosome)
            }
        }
        return numericChromosomes
    }

    static List<Chromosomes> character() {
        List<Chromosomes> characterChromosomes = []
        Chromosomes.values().each { Chromosomes chromosome ->
            if (!chromosome.chr.isInteger()) {
                characterChromosomes.add(chromosome)
            }
        }
        return characterChromosomes
    }

    static List<Chromosomes> all() {
        return Arrays.asList(Chromosomes.values())
    }

    static List<String> allLabels() {
        return all()*.chr
    }

    static List<String> numericLabels() {
        return numeric()*.chr
    }

    static List<String> characterLabels() {
        return character()*.chr
    }

    static int numberOfNumericChromosomes() {
        return numeric().size()
    }

    static String overallChromosomesLabel() {
        return "ALL"
    }
}
