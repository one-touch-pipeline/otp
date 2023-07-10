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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.AbstractAllCellsValidator
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel

import java.util.regex.Matcher
import java.util.regex.Pattern

@Component
class UnusualCharactersValidator extends AbstractAllCellsValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    static final Pattern NORMAL_CHARACTERS =
            Pattern.compile('[^0-9A-Za-z' + Pattern.quote(' !"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~') + ']')

    @Override
    Collection<String> getDescriptions() {
        return []  // Nothing worth mentioning
    }

    @Override
    void validateValue(AbstractMetadataValidationContext context, String value, Set<Cell> cells) {
        Matcher matcher = NORMAL_CHARACTERS.matcher(value)
        Set<Character> unusualCharacters = [] as Set<Character>
        while (matcher.find()) {
            String character = matcher.group()
            assert character.length() == 1
            unusualCharacters.add(character.toCharacter())
        }
        if (unusualCharacters) {
            String unusualCharactersString = unusualCharacters.collect { "'${it}' (0x${Integer.toHexString((int)it.charValue())})" }.join(', ')
            if (unusualCharacters.size() == 1) {
                context.addProblem(cells, LogLevel.WARNING, "'${value}' contains an unusual character: ${unusualCharactersString}", "At least one value contains an unusual character.")
            } else if (unusualCharacters.size() > 1) {
                context.addProblem(cells, LogLevel.WARNING, "'${value}' contains unusual characters: ${unusualCharactersString}", "At least one value contains an unusual character.")
            }
        }
    }
}
