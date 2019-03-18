/*
 * Copyright 2011-2019 The OTP authors
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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*

import static org.junit.Assert.assertEquals

@TestMixin(GrailsUnitTestMixin)
class ChromosomesTests {

    List<Chromosomes> allChromosomes
    List<Chromosomes> numericChromosomes
    List<Chromosomes> characterChromosomes

    List<String> allChromosomesLabels
    List<String> numericChromosomesLabels
    List<String> characterChromosomesLabels

    @Before
    void setUp() {
        allChromosomes = [
            Chromosomes.CHR_1, Chromosomes.CHR_2, Chromosomes.CHR_3, Chromosomes.CHR_4, Chromosomes.CHR_5,
            Chromosomes.CHR_6, Chromosomes.CHR_7, Chromosomes.CHR_8, Chromosomes.CHR_9, Chromosomes.CHR_10,
            Chromosomes.CHR_11, Chromosomes.CHR_12, Chromosomes.CHR_13, Chromosomes.CHR_14, Chromosomes.CHR_15,
            Chromosomes.CHR_16, Chromosomes.CHR_17, Chromosomes.CHR_18, Chromosomes.CHR_19, Chromosomes.CHR_20,
            Chromosomes.CHR_21, Chromosomes.CHR_22, Chromosomes.CHR_X, Chromosomes.CHR_Y, Chromosomes.CHR_M,
        ]

        numericChromosomes = [
            Chromosomes.CHR_1, Chromosomes.CHR_2, Chromosomes.CHR_3, Chromosomes.CHR_4, Chromosomes.CHR_5,
            Chromosomes.CHR_6, Chromosomes.CHR_7, Chromosomes.CHR_8, Chromosomes.CHR_9, Chromosomes.CHR_10,
            Chromosomes.CHR_11, Chromosomes.CHR_12, Chromosomes.CHR_13, Chromosomes.CHR_14, Chromosomes.CHR_15,
            Chromosomes.CHR_16, Chromosomes.CHR_17, Chromosomes.CHR_18, Chromosomes.CHR_19, Chromosomes.CHR_20,
            Chromosomes.CHR_21, Chromosomes.CHR_22,
        ]

        characterChromosomes = [
            Chromosomes.CHR_X, Chromosomes.CHR_Y, Chromosomes.CHR_M,
        ]

        allChromosomesLabels = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "X", "Y", "M"]

        numericChromosomesLabels = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22"]

        characterChromosomesLabels = ["X", "Y", "M"]

    }

    @After
    void tearDown() {
        allChromosomes = null
        numericChromosomes = null
        characterChromosomes = null
        allChromosomesLabels = null
        numericChromosomesLabels = null
        characterChromosomesLabels = null
    }

    @Test
    void testNumeric() {
        List<Chromosomes> numericChromosomesAct = Chromosomes.numeric()
        assertEquals(numericChromosomes, numericChromosomesAct)
    }

    @Test
    void testCharacter() {
        List<Chromosomes> characterChromosomesAct = Chromosomes.character()
        assertEquals(characterChromosomes, characterChromosomesAct)
    }

    @Test
    void testAll() {
        List<Chromosomes> allChromosomesAct = Chromosomes.all()
        assertEquals(allChromosomes, allChromosomesAct)
    }

    @Test
    void testNumericLabels() {
        List<Chromosomes> numericLabelsAct = Chromosomes.numericLabels()
        assertEquals(numericChromosomesLabels, numericLabelsAct)
    }

    @Test
    void testCharacterLabels() {
        List<Chromosomes> characterLabelsAct = Chromosomes.characterLabels()
        assertEquals(characterChromosomesLabels, characterLabelsAct)
    }

    @Test
    void testAllLabels() {
        List<Chromosomes> allLabelsAct = Chromosomes.allLabels()
        assertEquals(allChromosomesLabels, allLabelsAct)
    }
}
