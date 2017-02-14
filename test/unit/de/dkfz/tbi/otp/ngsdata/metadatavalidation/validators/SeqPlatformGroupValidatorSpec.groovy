package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Mock([
        Individual,
        LibraryPreparationKit,
        Pipeline,
        ProcessingOption,
        Project,
        MergingWorkPackage,
        ReferenceGenome,
        Sample,
        SampleIdentifier,
        SampleType,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqType,
        SequencingKitLabel,
])
public class SeqPlatformGroupValidatorSpec extends Specification {

    LibraryPreparationKit libraryPreparationKit
    MergingWorkPackage mergingWorkPackage1
    MergingWorkPackage mergingWorkPackage2
    MergingWorkPackage mergingWorkPackage3
    SampleIdentifier sampleIdentifier1
    SampleIdentifier sampleIdentifier2
    SampleIdentifier sampleIdentifier3
    SeqType seqType1
    SeqType seqType2
    SeqType seqType3
    SeqPlatformGroup seqPlatformGroup
    SeqPlatformModelLabel model1
    SeqPlatformModelLabel model2
    SeqPlatformModelLabel model3
    SequencingKitLabel kit1
    SequencingKitLabel kit2
    SequencingKitLabel kit3
    SeqPlatform platform_1_1_1
    SeqPlatform platform_2_2_2
    SeqPlatform platform_3_3_3

    private void createPlatforms() {
        sampleIdentifier1 = DomainFactory.createSampleIdentifier(name: 'SampleIdentifier1')
        sampleIdentifier2 = DomainFactory.createSampleIdentifier(name: 'SampleIdentifier2')
        sampleIdentifier3 = DomainFactory.createSampleIdentifier(name: 'SampleIdentifier3')
        seqType1 = DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        seqType2 = DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        seqType3 = DomainFactory.createSeqType(name: 'SeqType3', libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        mergingWorkPackage1 = DomainFactory.createMergingWorkPackage(sample: sampleIdentifier1.sample, seqType: seqType1)
        mergingWorkPackage2 = DomainFactory.createMergingWorkPackage(sample: sampleIdentifier2.sample, seqType: seqType2)
        mergingWorkPackage3 = DomainFactory.createMergingWorkPackage(sample: sampleIdentifier3.sample, seqType: seqType3)
        seqPlatformGroup = DomainFactory.createSeqPlatformGroup()
        libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        model1 = DomainFactory.createSeqPlatformModelLabel(name: 'Model1', alias: ['Model1Alias'])
        model2 = DomainFactory.createSeqPlatformModelLabel(name: 'Model2', alias: ['Model2Alias'])
        model3 = DomainFactory.createSeqPlatformModelLabel(name: 'Model3', alias: ['Model3Alias'])
        kit1 = DomainFactory.createSequencingKitLabel(name: 'Kit1', alias: ['Kit1Alias'])
        kit2 = DomainFactory.createSequencingKitLabel(name: 'Kit2', alias: ['Kit2Alias'])
        kit3 = DomainFactory.createSequencingKitLabel(name: 'Kit3', alias: ['Kit3Alias'])
        platform_1_1_1 = DomainFactory.createSeqPlatform(
                name: 'Platform1',
                seqPlatformGroup: mergingWorkPackage1.seqPlatformGroup,
                seqPlatformModelLabel: model1,
                sequencingKitLabel: kit1,
        )
        platform_2_2_2 = DomainFactory.createSeqPlatform(
                name: 'Platform2',
                seqPlatformGroup: mergingWorkPackage2.seqPlatformGroup,
                seqPlatformModelLabel: model2,
                sequencingKitLabel: kit2,
        )
        platform_3_3_3 = DomainFactory.createSeqPlatform(
                name: 'Platform3',
                seqPlatformGroup: seqPlatformGroup,
                seqPlatformModelLabel: model3,
                sequencingKitLabel: kit3,
        )
    }

    void 'validate with mixed data, adds expected problems'() {

        given:
        createPlatforms()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${SEQUENCING_TYPE}\t${LIBRARY_LAYOUT}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                "SampleIdentifier1\tSeqType1\tPAIRED\tPlatform1\tModel1\tKit1\t${mergingWorkPackage1.libraryPreparationKit.name}\n" +
                "SampleIdentifier2\tSeqType2\tPAIRED\tPlatform2\tModel2\tKit2\t${mergingWorkPackage2.libraryPreparationKit.name}\n" +
                "SampleIdentifier3\tSeqType3\tPAIRED\tPlatform3\tModel3\tKit3\t${mergingWorkPackage3.libraryPreparationKit.name}\n" +
                "SampleIdentifier1\tSeqType1\tPAIRED\tPlatform1\tModel1\tKit1\t${libraryPreparationKit.name}\n" +
                "SampleIdentifier42\tSeqType1\tPAIRED\tPlatform1\tModel1\tKit1\t${mergingWorkPackage1.libraryPreparationKit.name}\n" +
                "SampleIdentifier1\tSeqType42\tPAIRED\tPlatform1\tModel1\tKit1\t${mergingWorkPackage1.libraryPreparationKit.name}\n" +
                "SampleIdentifier1\tSeqType1\tPAIRED42\tPlatform1\tModel1\tKit1\t${mergingWorkPackage1.libraryPreparationKit.name}\n" +
                "SampleIdentifier1\tSeqType1\tPAIRED\tPlatform42\tModel1\tKit1\t${mergingWorkPackage1.libraryPreparationKit.name}\n" +
                "SampleIdentifier1\tSeqType1\tPAIRED\tPlatform1\tModel42\tKit1\t${mergingWorkPackage1.libraryPreparationKit.name}\n" +
                "SampleIdentifier1\tSeqType1\tPAIRED\tPlatform1\tModel1\tKit42\t42\n"
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[2].cells[0..5] as Set, Level.WARNING,
                        "The combination of sample '${sampleIdentifier3.sample.displayName}' and sequencing type '${seqType3.name}' with sequencing platform group '${seqPlatformGroup.name}' is registered with another sequencing platform group '${mergingWorkPackage3.seqPlatformGroup.name}' in the OTP database."),
                new Problem(context.spreadsheet.dataRows[3].cells[0..2,6] as Set, Level.WARNING,
                        "The combination of sample '${sampleIdentifier1.sample.displayName}' and sequencing type '${seqType1.name}' with library preparation kit '${libraryPreparationKit.name}' is registered with another library preparation kit '${mergingWorkPackage1.libraryPreparationKit.name}' in the OTP database."),

        ]

        when:
        new SeqPlatformGroupValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
