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

enum PedionTissue {
    BLOOD('1A'),
    LIVER('1B'),
    PANCREAS('1C'),
    OVARY('1D'),
    BRAIN('1E'),
    PROSTATE('1F'),
    NEURAL_TISSUE('1G'),
    RECTUM('1H'),
    SMALL_INTESTINE('1I'),
    BONE_MARROW('1J'),
    BLADDER('1K'),
    TONGUE('1L'),
    GINGIVA('1M'),
    DIAPHRAGMA_ORIS('1N'),
    LIPS('1O'),
    LYMPH_NODE('1P'),
    COLON('1Q'),
    SIGMOID_COLON('1R'),
    ADRENAL_GLAND('1S'),
    MUSCLE('1T'),
    SKIN('1U'),
    LUNG('1V'),
    FAT_TISSUE('1W'),
    LOWER_JAW('1X'),
    SALIVARY_GLAND('1Y'),
    UNKNOWN('1Z'),

    final String code
    final String usedName

    PedionTissue(String code) {
        this.code = code
        this.usedName = name().toLowerCase().replace("_", "-")
        assert code.size() == 2
    }

    static final Map<String, String> LETTER_TO_NAME_MAP = values().collectEntries {
        [it.code, it.usedName]
    }.asImmutable()
}
