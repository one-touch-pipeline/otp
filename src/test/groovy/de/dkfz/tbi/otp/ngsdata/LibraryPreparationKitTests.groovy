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

import grails.test.mixin.TestFor
import org.junit.*

@TestFor(LibraryPreparationKit)
class LibraryPreparationKitTests {

    LibraryPreparationKit kit1
    LibraryPreparationKit kit2

    @Before
    void setUp() {
        kit1 = new LibraryPreparationKit(
                name: "kitName1",
                shortDisplayName: "name1",
                )
        kit2 = new LibraryPreparationKit(
                name: "kitName2",
                shortDisplayName: "name2",
                )
    }

    @After
    void tearDown() {
        kit1 = null
        kit2 = null
    }

    @Test
    void testCreateCorrect() {
        assertTrue kit1.validate()
        assertNotNull kit1.save(flush: true)
        assertTrue kit2.validate()
        assertNotNull kit2.save(flush: true)
        assertTrue !kit1.toString().empty
        assertTrue !kit2.toString().empty
    }

    @Test
    void testNameIsNull() {
        kit1.name = null
        assertFalse kit1.validate()
    }

    @Test
    void testNameIsEmpty() {
        kit1.name = ""
        assertFalse kit1.validate()
    }

    @Test
    void testNameNotUnique() {
        assertNotNull kit1.save(flush: true)
        kit2.name = "kitName1"
        assertFalse kit2.validate()
    }
}
