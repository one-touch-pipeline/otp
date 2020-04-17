/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.parser.pedion

enum PedionTissue {
    BLOOD('A'),
    LIVER('B'),
    PANCREAS('C'),
    OVAR('D'),
    BRAIN('E'),
    PROSTATE('F'),
    INTESTINE('G'),
    RECTUM('H'),
    SMALL_INTESTINE('I'),
    BONE_MARROW('J'),
    ORGANOID_LIVER('N'),
    ORGANOID_PANCREAS('O'),

    final String letter
    final String usedName

    PedionTissue(String letter) {
        this.letter = letter
        this.usedName = name().replace("_", "-")
        assert letter.size() == 1
    }

    static final Map<String, String> LETTER_TO_NAME_MAP = values().collectEntries {
        [it.letter, it.usedName]
    }.asImmutable()
}
