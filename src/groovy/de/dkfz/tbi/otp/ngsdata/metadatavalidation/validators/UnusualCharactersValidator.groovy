package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.AllCellsValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level

import java.util.regex.Matcher
import java.util.regex.Pattern

@Component
class UnusualCharactersValidator extends AllCellsValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

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
                context.addProblem(cells, Level.WARNING, "'${value}' contains an unusual character: ${unusualCharactersString}", "At least one value contains an unusual character.")
            } else if (unusualCharacters.size() > 1) {
                context.addProblem(cells, Level.WARNING, "'${value}' contains unusual characters: ${unusualCharactersString}", "At least one value contains an unusual character.")
            }
        }
    }
}
