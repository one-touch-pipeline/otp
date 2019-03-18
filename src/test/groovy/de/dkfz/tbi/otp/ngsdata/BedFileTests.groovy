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

package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.*

@TestFor(BedFile)
@Mock([ReferenceGenome, LibraryPreparationKit])
class BedFileTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    ReferenceGenome refGen
    LibraryPreparationKit kit1
    LibraryPreparationKit kit2
    BedFile bedFile1
    BedFile bedFile2

    @Before
    void setUp() {
        refGen = DomainFactory.createReferenceGenome([
                name: "refGen",
                path: "filePath",
                fileNamePrefix: "prefix",
                length: ARBITRARY_REFERENCE_GENOME_LENGTH,
                lengthWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                lengthRefChromosomes: ARBITRARY_REFERENCE_GENOME_LENGTH,
                lengthRefChromosomesWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                chromosomeSuffix: "",
                chromosomePrefix: "",
                ])
        kit1 = new LibraryPreparationKit(
                name: "kitName1",
                shortDisplayName: "name1",
                )
        assertNotNull kit1.save(flush: true)
        kit2 = new LibraryPreparationKit(
                name: "kitName2",
                shortDisplayName: "name2",
                )
        assertNotNull kit2.save(flush: true)
        bedFile1 = new BedFile(
                fileName: "fileName1",
                targetSize: 1,
                referenceGenome: refGen,
                libraryPreparationKit: kit1
                )
        bedFile2 = new BedFile(
                fileName: "fileName2",
                targetSize: 111,
                referenceGenome: refGen,
                libraryPreparationKit: kit2
                )
    }

    @After
    void tearDown() {
        refGen = null
        kit1 = null
        kit2 = null
        bedFile1 = null
        bedFile2 = null
    }

    @Test
    void testCreateCorrect() {
        assertTrue bedFile1.validate()
        assertNotNull bedFile1.save(flush: true)
        assertTrue bedFile2.validate()
        assertNotNull bedFile2.save(flush: true)
        assertTrue !bedFile1.toString().empty
        assertTrue !bedFile2.toString().empty
    }

    @Test
    void testFileNameNull() {
        bedFile1.fileName = null
        assertFalse bedFile1.validate()
    }

    @Test
    void testFileNameEmpty() {
        bedFile1.fileName = ""
        assertFalse bedFile1.validate()
    }

    @Test
    void testFileNameNotUnique() {
        assertNotNull bedFile1.save(flush: true)
        bedFile2.fileName = "fileName1"
        assertFalse bedFile2.validate()
        bedFile2.referenceGenome = DomainFactory.createReferenceGenome()
        assertTrue bedFile2.validate()
    }

    @Test
    void testGenomeKitPrimaryKey() {
        assertNotNull bedFile1.save(flush: true)
        bedFile2.libraryPreparationKit = kit1
        assertFalse bedFile2.validate()
    }

    @Test
    void testMinTargetSize() {
        bedFile1.targetSize = 0
        assertFalse bedFile1.validate()
        bedFile2.targetSize = -1
        assertFalse bedFile2.validate()
    }
}
