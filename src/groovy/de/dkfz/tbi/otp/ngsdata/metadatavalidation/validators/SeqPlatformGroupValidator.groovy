package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqPlatformGroupValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService


    @Override
    Collection<String> getDescriptions() {
        return ['The combination of sample, sequencing type and sequencing platform group is registered in the OTP database.',
                'The combination of sample, sequencing type and library preparation kit is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_ID.name(),
                SEQUENCING_TYPE.name(),
                LIBRARY_LAYOUT.name(),
                INSTRUMENT_PLATFORM.name(),
                INSTRUMENT_MODEL.name(),
                SEQUENCING_KIT.name(),
                LIB_PREP_KIT.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_KIT.name()) {
            optionalColumnMissing(context, columnTitle)
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            SeqType seqType = SeqType.findByNameAndLibraryLayout(it.getValue(SEQUENCING_TYPE.name()), it.getValue(LIBRARY_LAYOUT.name()))
            if (!seqType) {
                return
            }

            MergingWorkPackage mergingWorkPackage = MergingWorkPackage.findBySampleAndSeqType(
                    SampleIdentifier.findByName(it.getValue(SAMPLE_ID.name()))?.sample,
                    seqType
            )
            if (!mergingWorkPackage) {
                return
            }

            SeqPlatformGroup seqPlatformGroup = SeqPlatformService.findSeqPlatform(
                    it.getValue(INSTRUMENT_PLATFORM.name()),
                    it.getValue(INSTRUMENT_MODEL.name()),
                    it.getValue(SEQUENCING_KIT.name())
            )?.seqPlatformGroup
            if (!seqPlatformGroup) {
                return
            }

            if (mergingWorkPackage.seqPlatformGroup != seqPlatformGroup) {
                context.addProblem(
                        it.cells.findAll() { cell ->
                            cell.text != it.getValue(LIB_PREP_KIT.name())
                        },
                        Level.WARNING,
                        "The combination of sample '${mergingWorkPackage.sample.displayName}' and sequencing type '${mergingWorkPackage.seqType.name}' with sequencing platform group '${seqPlatformGroup.name}' is registered with another sequencing platform group '${mergingWorkPackage.seqPlatformGroup.name}' in the OTP database."
                )
            }

            if (!seqType.isWgbs()) {
                LibraryPreparationKit libraryPreparationKit = libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(it.getValue(LIB_PREP_KIT.name()))
                if (mergingWorkPackage.libraryPreparationKit != libraryPreparationKit) {
                    context.addProblem(
                            it.cells.findAll() { cell ->
                                !(cell.text == it.getValue(INSTRUMENT_PLATFORM.name()) || cell.text == it.getValue(INSTRUMENT_MODEL.name()) || cell.text == it.getValue(SEQUENCING_KIT.name()))
                            },
                            Level.WARNING,
                            "The combination of sample '${mergingWorkPackage.sample.displayName}' and sequencing type '${mergingWorkPackage.seqType.name}' with library preparation kit '${libraryPreparationKit?.name}' is registered with another library preparation kit '${mergingWorkPackage.libraryPreparationKit?.name}' in the OTP database."
                    )
                }
            }
        }
    }
}
