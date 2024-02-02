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
package de.dkfz.tbi.otp.parser.pedion

enum PedionCategory {
    CONTROL('C'),
    TUMOR('T'),
    METASTASIS('M'),

    INFILTRATION_FRONT_RIGHT('A'),
    INFILTRATION_FRONT_LEFT('B'),

    TUMOR_CENTER('Z'),
    TUMOR_CENTER_LEFT('D'),
    TUMOR_CENTER_RIGHT('E'),
    TUMOR_CENTER_UP('F'),
    TUMOR_CENTER_DOWN('G'),
    TUMOR_MARGIN('H'),
    TUMOR_MARGIN_LEFT('L'),
    TUMOR_MARGIN_RIGHT('R'),
    TUMOR_MARGIN_UP('O'),
    TUMOR_MARGIN_DOWN('U'),

    final String letter
    final String usedName

    PedionCategory(String letter) {
        this.letter = letter
        this.usedName = name().toLowerCase().replace("_", "-")
        assert letter.size() == 1
    }

    static final Map<String, String> LETTER_TO_NAME_MAP = values().collectEntries {
        [it.letter, it.usedName]
    }.asImmutable()
}
