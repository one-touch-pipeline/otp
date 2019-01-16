package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import grails.test.mixin.web.*
import org.joda.time.*
import org.junit.*
import org.junit.rules.*
import org.springframework.context.*
import spock.lang.*
import spock.util.mop.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        AntibodyTarget,
        ChipSeqSeqTrack,
        DataFile,
        ExomeSeqTrack,
        FileType,
        IlseSubmission,
        Individual,
        LibraryPreparationKit,
        MetaDataEntry,
        MetaDataFile,
        MetaDataKey,
        OtrsTicket,
        Project,
        ProjectCategory,
        Realm,
        Run,
        RunSegment,
        ProcessingOption,
        Sample,
        SampleIdentifier,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        SoftwareToolIdentifier,
])
@TestMixin(ControllerUnitTestMixin)
class MetadataImportServiceSpec extends Specification {

    final static String TICKET_NUMBER = "2000010112345678"

    @Rule
    TemporaryFolder temporaryFolder

    void 'getImplementedValidations returns descriptions of validations'() {

        given:
        MetadataImportService service = new MetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(MetadataValidator) >>
                    [
                            'validator1': [
                                    getDescriptions: { ['description1', 'description2'] }
                            ] as MetadataValidator,
                            'validator2': [
                                    getDescriptions: { ['description3'] }
                            ] as MetadataValidator,
                    ]
        }

        expect:
        containSame(service.getImplementedValidations(), ['description1', 'description2', 'description3'])
    }

    void 'validate creates context and calls validators'() {

        given:
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        String directoryStructureName = HelperUtils.uniqueString

        File testDirectory = TestCase.createEmptyTestDirectory()
        Path metadataFile = testDirectory.toPath().resolve('metadata.tsv')
        metadataFile.bytes = 'Header\nI am metadata!'.getBytes(MetadataValidationContext.CHARSET)

        MetadataImportService service = new MetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(MetadataValidator) >>
                    [
                            'validator1': [
                                    validate: { MetadataValidationContext context ->
                                        context.addProblem(Collections.emptySet(), Level.ERROR, 'message1')
                                        context.addProblem(Collections.emptySet(), Level.ERROR, 'message2')
                                    }
                            ] as MetadataValidator,
                            'validator2': [
                                    validate: { MetadataValidationContext context ->
                                        context.addProblem(Collections.emptySet(), Level.ERROR, 'message3')
                                    }
                            ] as MetadataValidator,
                    ]
            getBean(directoryStructureName, DirectoryStructure) >> directoryStructure
        }

        when:
        MetadataValidationContext context = service.validate(metadataFile, directoryStructureName)

        then:
        containSame(context.problems*.message, ['message1', 'message2', 'message3'])
        context.directoryStructure == directoryStructure
        context.metadataFile == metadataFile
        context.spreadsheet.header.cells[0].text == 'Header'
        context.spreadsheet.dataRows[0].cells[0].text == 'I am metadata!'

        cleanup:
        testDirectory.deleteDir()
    }

    void 'getDirectoryStructureBeanName, when called with AUTO_DETECT_DIRECTORY_STRUCTURE_NAME, returns DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME'() {

        expect:
        MetadataImportService.getDirectoryStructureBeanName(MetadataImportService.AUTO_DETECT_DIRECTORY_STRUCTURE_NAME,
                null) == MetadataImportService.DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME
    }

    void 'getDirectoryStructureBeanName, when called with a string != AUTO_DETECT_DIRECTORY_STRUCTURE_NAME, returns that string'() {

        expect:
        MetadataImportService.getDirectoryStructureBeanName('abcdef', null) == 'abcdef'
    }

    @Unroll
    void 'validateAndImportWithAuth, when there are no problems, calls importMetadataFile and returns created MetaDataFile'() {
        given:
        SeqCenter seqCenter = DomainFactory.createSeqCenter(autoImportable: true, autoImportDir: "/auto-import-dir")
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        String directoryStructureName = HelperUtils.uniqueString

        File testDirectory = TestCase.createEmptyTestDirectory()
        File runDirectory = new File(testDirectory, 'run')
        assert runDirectory.mkdir()
        Path metadataFile = testDirectory.toPath().resolve('metadata.tsv')
        metadataFile.bytes = (
                "${RUN_ID}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${CENTER_NAME}\n" +
                        ("run\tplatform\tmodelAlias\t${seqCenter.name}\n" * 2)
        ).getBytes(MetadataValidationContext.CHARSET)

        MetaDataFile metadataFileObject = new MetaDataFile()
        MetadataImportService service = Spy(MetadataImportService) {
            1 * importMetadataFile(_, align, RunSegment.ImportMode.MANUAL, TICKET_NUMBER, null, automaticNotification) >> metadataFileObject
            1 * copyMetaDataFileIfRequested(_)
        }
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(MetadataValidator) >> [:]
            getBean(directoryStructureName, DirectoryStructure) >> directoryStructure
        }
        MetadataImportService.PathWithMd5sum pathWithMd5sum = new MetadataImportService.PathWithMd5sum(metadataFile, null)

        when:
        List<ValidateAndImportResult> results = service.validateAndImportWithAuth([pathWithMd5sum], directoryStructureName, align, false, TICKET_NUMBER, null, automaticNotification)

        then:
        results[0].context.metadataFile == metadataFile
        results[0].metadataFile == metadataFileObject

        cleanup:
        testDirectory.deleteDir()

        where:
        align << [true, false]
        automaticNotification << [true, false]
    }

    void 'validateAndImportWithAuth, when there are errors, returns null as metadataFile'() {
        given:
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        String directoryStructureName = HelperUtils.uniqueString

        Path metadataFile = Paths.get('relative_path')
        MetadataImportService.PathWithMd5sum pathWithMd5sum = new MetadataImportService.PathWithMd5sum(metadataFile, null)

        MetadataImportService service = new MetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBean(directoryStructureName, DirectoryStructure) >> directoryStructure
        }

        when:
        List<ValidateAndImportResult> results = service.validateAndImportWithAuth([pathWithMd5sum], directoryStructureName, false, false, TICKET_NUMBER, null, true)

        then:
        results[0].context.metadataFile == metadataFile
        results[0].metadataFile == null
    }

    void 'validateAndImportWithAuth, validate all before import the first'() {
        given:
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        String directoryStructureName = HelperUtils.uniqueString
        SeqCenter seqCenter = DomainFactory.createSeqCenter(autoImportable: false)
        MetadataValidationContext context1 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("import1_meta.tsv")])
        MetaDataFile metadataFile1 = DomainFactory.createMetaDataFile()
        MetadataValidationContext context2 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("import2_meta.tsv")])
        MetaDataFile metadataFile2 = DomainFactory.createMetaDataFile()

        List<MetadataImportService.PathWithMd5sum> pathWithMd5sums = [
                new MetadataImportService.PathWithMd5sum(context1.metadataFile, HelperUtils.randomMd5sum),
                new MetadataImportService.PathWithMd5sum(context2.metadataFile, HelperUtils.randomMd5sum),
        ]

        int imported = 0

        MetadataImportService service = Spy(MetadataImportService) {
            1 * validate(context1.metadataFile, directoryStructureName) >> { assert imported == 0; context1 }
            1 * validate(context2.metadataFile, directoryStructureName) >> { assert imported == 0; context2 }
            1 * importMetadataFile(context1, true, RunSegment.ImportMode.MANUAL, TICKET_NUMBER, null, true) >> {
                imported++
                metadataFile1
            }
            1 * importMetadataFile(context2, true, RunSegment.ImportMode.MANUAL, TICKET_NUMBER, null, true) >> {
                imported++
                metadataFile2
            }
            0 * importMetadataFile(*_)
        }
        service.applicationContext = Mock(ApplicationContext) {
            getBean(directoryStructureName, DirectoryStructure) >> directoryStructure
        }
        service.fileSystemService = new TestFileSystemService()

        when:
        List<ValidateAndImportResult> validateAndImportResults = service.validateAndImportWithAuth(pathWithMd5sums, directoryStructureName, true, false, TICKET_NUMBER, null, true)

        then:
        validateAndImportResults*.context == [context1, context2]
        validateAndImportResults*.metadataFile == [metadataFile1, metadataFile2]
        imported == 2
    }

    void 'validateAndImportMultiple when all are valid, returns import results'() {

        given:
        SeqCenter seqCenter = DomainFactory.createSeqCenter(autoImportable: true, autoImportDir: "/auto-import-dir")
        MetadataValidationContext context1 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${seqCenter.autoImportDir}/001111/data/1111_meta.tsv")])
        MetaDataFile metadataFile1 = DomainFactory.createMetaDataFile()
        MetadataValidationContext context2 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${seqCenter.autoImportDir}/002222/data/2222_meta.tsv")])
        MetaDataFile metadataFile2 = DomainFactory.createMetaDataFile()

        MetadataImportService service = Spy(MetadataImportService) {
            1 * validate(context1.metadataFile, MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME) >> context1
            1 * validate(context2.metadataFile, MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME) >> context2
            1 * importMetadataFile(context1, true, RunSegment.ImportMode.AUTOMATIC, TICKET_NUMBER, null, true) >> metadataFile1
            1 * importMetadataFile(context2, true, RunSegment.ImportMode.AUTOMATIC, TICKET_NUMBER, null, true) >> metadataFile2
        }
        service.fileSystemService = new TestFileSystemService()

        expect:
        List<ValidateAndImportResult> validateAndImportResults = service.validateAndImportMultiple(TICKET_NUMBER, '1111+2222')

        validateAndImportResults*.context == [context1, context2]
        validateAndImportResults*.metadataFile == [metadataFile1, metadataFile2]
    }

    void 'validateAndImportMultiple when some are invalid, throws AutoImportFailedException'() {

        given:
        SeqCenter seqCenter = DomainFactory.createSeqCenter(autoImportable: true, autoImportDir: "/auto-import-dir")
        Problems problems = new Problems()
        problems.addProblem([] as Set, Level.ERROR, "An Error occurred")
        MetadataValidationContext context1 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${seqCenter.autoImportDir}/001111/data/1111_meta.tsv"), problems: problems])
        MetadataValidationContext context2 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${seqCenter.autoImportDir}/002222/data/2222_meta.tsv")])
        MetadataValidationContext context3 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${seqCenter.autoImportDir}/003333/data/3333_meta.tsv"), problems: problems])

        MetadataImportService service = Spy(MetadataImportService) {
            1 * validate(context1.metadataFile, MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME) >> context1
            1 * validate(context2.metadataFile, MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME) >> context2
            1 * validate(context3.metadataFile, MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME) >> context3
            1 * importMetadataFile(context2, true, RunSegment.ImportMode.AUTOMATIC, TICKET_NUMBER, null, true) >> DomainFactory.createMetaDataFile()
        }

        service.fileSystemService = new TestFileSystemService()


        when:
        service.validateAndImportMultiple(TICKET_NUMBER, '1111+2222+3333')

        then:
        MultiImportFailedException e = thrown()
        e.failedValidations == [context1, context3]
        e.allPaths == [context1, context2, context3]*.metadataFile
    }

    @Unroll
    void 'parseIlseNumbers returns correct collection of ILSe numbers'(String string, List<Integer> numbers) {

        expect:
        MetadataImportService.parseIlseNumbers(string) == numbers

        where:
        string                          || numbers
        '6072'                          || [6072]
        '6160+6061'                     || [6160, 6061]
        '6083+6085'                     || [6083, 6085]
        '6127-6128'                     || [6127, 6128]
        '6005-6009'                     || [6005, 6006, 6007, 6008, 6009]
        '6108,6136-6138'                || [6108, 6136, 6137, 6138]
        '6012,6013,6015-6019,6021-6022' || [6012, 6013, 6015, 6016, 6017, 6018, 6019, 6021, 6022]
        '6160 & 6061'                   || [6160, 6061]
        '6160 + 6061'                   || [6160, 6061]
        '6127 - 6128'                   || [6127, 6128]
    }

    @Unroll
    void 'parseIlseNumbers throws exception when input is invalid'(String string, String exceptionMessage) {

        when:
        MetadataImportService.parseIlseNumbers(string)

        then:
        IllegalArgumentException e = thrown()
        e.message == exceptionMessage

        where:
        string      || exceptionMessage
        '123'       || "Cannot parse '123' as an ILSe number or a range of ILSe numbers."
        '1234567'   || "Cannot parse '1234567' as an ILSe number or a range of ILSe numbers."
        '1111-2222' || "Range of ILSe numbers is too large: '1111-2222'"
    }

    void 'mayImport, when maximumProblemLevel is less than WARNING, returns true'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        assert context.maximumProblemLevel.intValue() < Level.WARNING.intValue()

        expect:
        MetadataImportService.mayImport(context, false, HelperUtils.randomMd5sum)
    }

    void 'mayImport, when maximumProblemLevel is greater than WARNING, returns false'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), Level.ERROR, 'my message')
        assert context.maximumProblemLevel.intValue() > Level.WARNING.intValue()

        expect:
        !MetadataImportService.mayImport(context, true, context.metadataFileMd5sum)
    }

    @Unroll
    void 'mayImport, when maximumProblemLevel is WARNING and MD5 sum matches, returns ignoreWarnings'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), Level.WARNING, 'my message')
        assert context.maximumProblemLevel == Level.WARNING

        expect:
        MetadataImportService.mayImport(context, ignoreWarnings, context.metadataFileMd5sum) == ignoreWarnings

        where:
        ignoreWarnings << [true, false]
    }

    void 'mayImport, when maximumProblemLevel is WARNING and ignoreWarnings is false and MD5 sum does not match, returns false'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), Level.WARNING, 'my message')
        assert context.maximumProblemLevel == Level.WARNING

        expect:
        !MetadataImportService.mayImport(context, false, HelperUtils.randomMd5sum)
        exactlyOneElement(context.problems).message == 'my message'
    }

    void 'mayImport, when maximumProblemLevel is WARNING and ignoreWarnings is true and MD5 sum does not match, adds problem and returns false'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), Level.WARNING, HelperUtils.uniqueString)
        assert context.maximumProblemLevel == Level.WARNING

        expect:
        !MetadataImportService.mayImport(context, true, HelperUtils.randomMd5sum)
        context.problems.find {
            it.level == Level.INFO && it.message == 'Not ignoring warnings, because the metadata file has changed since the previous validation.'
        }
    }


    @Unroll
    void 'importMetadataFile imports correctly'(boolean runExists, boolean includeOptional, boolean align, RunSegment.ImportMode importMode) {

        given:
        SeqCenter seqCenter = DomainFactory.createSeqCenter(name: 'center1')
        SeqCenter seqCenter2 = DomainFactory.createSeqCenter(name: 'center2')
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'platform1',
                seqPlatformModelLabel: DomainFactory.createSeqPlatformModelLabel(name: 'model1'),
        )
        SeqPlatform seqPlatform2 = DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: 'platform2',
                seqPlatformModelLabel: DomainFactory.createSeqPlatformModelLabel(name: 'model2'),
        )
        Date runDate = new LocalDate(2016, 4, 13).toDate()
        Date run2Date = new LocalDate(2016, 6, 6).toDate()
        if (runExists) {
            DomainFactory.createRun(
                    name: 'run1',
                    dateExecuted: runDate,
                    seqCenter: seqCenter,
                    seqPlatform: seqPlatform,
            )
        }
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket(automaticNotification: true)
        SeqType mySeqType = DomainFactory.createSeqType(name: 'MY_SEQ_TYP', libraryLayout: 'SINGLE')
        SeqType mySeqTypeTag = DomainFactory.createSeqType(name: 'MY_SEQ_TYP_TAGMENTATION', libraryLayout: 'SINGLE')
        SeqType exomeSingle = DomainFactory.createExomeSeqType(SeqType.LIBRARYLAYOUT_SINGLE)
        SeqType exomePaired = DomainFactory.createExomeSeqType(SeqType.LIBRARYLAYOUT_PAIRED)
        SeqType chipSeqMyPaired = DomainFactory.createChipSeqType('MY_PAIRED')
        SeqType chipSeqMySingle = DomainFactory.createChipSeqType('MY_SINGLE')
        Sample sample1 = DomainFactory.createSampleIdentifier(name: 'in_db').sample
        Sample sample2
        def (SoftwareTool pipeline1, SoftwareTool pipeline2, SoftwareTool unknownPipeline) =
        ['pipeline1', 'pipeline2', 'unknown'].collect {
            DomainFactory.createSoftwareToolIdentifier(
                    name: it,
                    softwareTool: DomainFactory.createSoftwareTool(
                            type: SoftwareTool.Type.BASECALLING,
                    ),
            ).softwareTool
        }
        LibraryPreparationKit kit1 = DomainFactory.createLibraryPreparationKit(name: 'kit1')
        LibraryPreparationKit kit2 = DomainFactory.createLibraryPreparationKit(name: 'kit2')
        AntibodyTarget target1 = DomainFactory.createAntibodyTarget(name: 'target1')
        AntibodyTarget target2 = DomainFactory.createAntibodyTarget(name: 'target2')
        FileType fileType = DomainFactory.createFileType(
                type: FileType.Type.SEQUENCE,
                signature: '_',
        )

        MetadataImportService service = new MetadataImportService()
        service.sampleIdentifierService = Mock(SampleIdentifierService) {
            parseAndFindOrSaveSampleIdentifier('parse_me') >> {
                SampleIdentifier identifier = DomainFactory.createSampleIdentifier(name: 'parse_me')
                sample2 = identifier.sample
                return identifier
            }
        }
        SamplePair samplePair = Mock(SamplePair)

        service.seqTrackService = Mock(SeqTrackService)
        service.seqPlatformService = Mock(SeqPlatformService) {
            findSeqPlatform(seqPlatform.name, seqPlatform.seqPlatformModelLabel.name, null) >> seqPlatform
            findSeqPlatform(seqPlatform2.name, seqPlatform2.seqPlatformModelLabel.name, null) >> seqPlatform2
        }
        service.trackingService = Mock(TrackingService) {
            createOrResetOtrsTicket(otrsTicket.ticketNumber, null, true) >> otrsTicket
        }
        service.libraryPreparationKitService = Mock(LibraryPreparationKitService) {
            findByNameOrImportAlias(kit1.name) >> kit1
            findByNameOrImportAlias(kit2.name) >> kit2
        }
        service.seqTypeService = Mock(SeqTypeService) {
            findByNameOrImportAlias("MY_SEQ_TYP", [libraryLayout: "SINGLE", singleCell: false]) >> mySeqType
            findByNameOrImportAlias("MY_SEQ_TYP_TAGMENTATION", [libraryLayout: "SINGLE", singleCell: false]) >> mySeqTypeTag
            findByNameOrImportAlias("EXON", [libraryLayout: "SINGLE", singleCell: false]) >> exomeSingle
            findByNameOrImportAlias("EXON", [libraryLayout: "PAIRED", singleCell: false]) >> exomePaired
            findByNameOrImportAlias("ChIP Seq", [libraryLayout: "MY_PAIRED", singleCell: false]) >> chipSeqMyPaired
            findByNameOrImportAlias("ChIP Seq", [libraryLayout: "MY_SINGLE", singleCell: false]) >> chipSeqMySingle
        }
        GroovyMock(SamplePair, global: true)
        1 * SamplePair.findMissingDiseaseControlSamplePairs() >> [samplePair]
        1 * samplePair.save()

        File file = new File(new File(TestCase.getUniqueNonExistentPath(), 'run1'), 'metadata.tsv')
        DirectoryStructure directoryStructure = [
                getColumnTitles: { [FASTQ_FILE.name()] },
                getDataFilePath: { MetadataValidationContext context, ValueTuple valueTuple ->
                    return Paths.get(file.parent, valueTuple.getValue(FASTQ_FILE.name()))
                },
        ] as DirectoryStructure

        String md5a = HelperUtils.getRandomMd5sum()
        String md5b = HelperUtils.getRandomMd5sum()
        String md5c = HelperUtils.getRandomMd5sum()
        String md5d = HelperUtils.getRandomMd5sum()
        String md5e = HelperUtils.getRandomMd5sum()
        String md5f = HelperUtils.getRandomMd5sum()
        String md5g = HelperUtils.getRandomMd5sum()
        String md5h = HelperUtils.getRandomMd5sum()
        String metadata = """
${FASTQ_FILE}                   fastq_a     s_1_1_      s_1_2_      s_2_1_      s_2_2_      s_3_1_      fastq_g     fastq_b
${MD5}                          ${md5a}     ${md5b}     ${md5c}     ${md5d}     ${md5e}     ${md5f}     ${md5g}     ${md5h}
${RUN_ID}                       run1        run1        run1        run1        run1        run1        run1        run2
${CENTER_NAME}                  center1     center1     center1     center1     center1     center1     center1     center2
${INSTRUMENT_PLATFORM}          platform1   platform1   platform1   platform1   platform1   platform1   platform1   platform2
${INSTRUMENT_MODEL}             model1      model1      model1      model1      model1      model1      model1      model2
${RUN_DATE}                     2016-04-13  2016-04-13  2016-04-13  2016-04-13  2016-04-13  2016-04-13  2016-04-13  2016-06-06
${LANE_NO}                      4           1           1           2           2           2           3           5
${BARCODE}                      -           barcode8    barcode8    barcode7    barcode7    barcode6    -           -
${SEQUENCING_TYPE}              MY_SEQ_TYP  EXON        EXON        ChIP Seq    ChIP Seq    ChIP Seq    EXON        MY_SEQ_TYP
${LIBRARY_LAYOUT}               SINGLE      PAIRED      PAIRED      MY_PAIRED   MY_PAIRED   MY_SINGLE   SINGLE      SINGLE
${SAMPLE_ID}                    parse_me    in_db       in_db       parse_me    parse_me    in_db       parse_me    parse_me
${TAGMENTATION_BASED_LIBRARY}   -           -           -           -           -           -           -           true
"""
        if (includeOptional) {
            metadata += """
${INSERT_SIZE}                  -           -           -           234         234         -           456         -
${PIPELINE_VERSION}             -           pipeline1   pipeline1   -           -           -           pipeline2   -
${LIB_PREP_KIT}                 -           kit1        kit1        kit2        kit2        UNKNOWN     UNKNOWN     -
${ANTIBODY_TARGET}              -           -           -           target1     target1     target2     -           -
${ANTIBODY}                     -           -           -           antibody1   antibody1   -           -           -
${ILSE_NO}                      -           1234        1234        -           -           2345        -           -
"""
        }
        List<List<String>> lines = metadata.readLines().findAll()*.split(/ {2,}/).transpose()
        if (!includeOptional) {
            lines = lines.subList(0, 2)
        }
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                lines.collect { it*.replaceFirst(/^-$/, '').join('\t') }.join('\n'),
                [metadataFile: file.toPath(), directoryStructure: directoryStructure]
        )
        int seqTrackCount = includeOptional ? 6 : 1
        int dataFileCount = includeOptional ? 8 : 1

        when:
        MetaDataFile result = service.importMetadataFile(context, align, importMode, otrsTicket.ticketNumber, null, otrsTicket.automaticNotification)

        then:

        // runs
        Run.count == (includeOptional ? 2 : 1)
        Run run = Run.findWhere(
                name: 'run1',
                dateExecuted: runDate,
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
        )
        run != null
        Run run2 = Run.findWhere(
                name: 'run2',
                dateExecuted: run2Date,
                seqCenter: seqCenter2,
                seqPlatform: seqPlatform2,
        )
        (run2 != null) == includeOptional

        // runSegment
        RunSegment.count == 1
        RunSegment runSegment = RunSegment.findWhere(
                align: align,
                otrsTicket: otrsTicket,
                importMode: importMode
        )
        runSegment != null

        // metadataFile
        MetaDataFile.count == 1
        MetaDataFile metadataFile = MetaDataFile.findWhere(
                fileName: file.name,
                filePath: file.parent,
                md5sum: context.metadataFileMd5sum,
                runSegment: runSegment,
        )
        result == metadataFile

        SeqTrack.count == seqTrackCount
        DataFile.count == dataFileCount
        MetaDataKey.count == context.spreadsheet.header.cells.size()
        MetaDataEntry.count == context.spreadsheet.header.cells.size() * dataFileCount

        Map commonDataFileProperties = [
                pathName  : '',
                used      : true,
                runSegment: runSegment,
                fileType  : fileType,
        ]
        Map commonRun1DataFileProperties = commonDataFileProperties + [
                dateExecuted: runDate,
                run         : run,
        ]
        Map commonRun2DataFileProperties = commonDataFileProperties + [
                dateExecuted: run2Date,
                run         : run2,
        ]

        // seqTrack1
        SeqTrack seqTrack1 = SeqTrack.findWhere(
                laneId: '4',
                insertSize: 0,
                run: run,
                sample: sample2,
                seqType: mySeqType,
                pipelineVersion: unknownPipeline,
                kitInfoReliability: InformationReliability.UNKNOWN_UNVERIFIED,
                libraryPreparationKit: null,
        )
        seqTrack1.ilseId == null
        DataFile dataFile1 = DataFile.findWhere(commonRun1DataFileProperties + [
                fileName   : 'fastq_a',
                vbpFileName: 'fastq_a',
                md5sum     : md5a,
                project    : sample2.project,
                mateNumber : 1,
                seqTrack   : seqTrack1,
        ])
        dataFile1 != null
        MetaDataKey key = MetaDataKey.findWhere(name: MD5.name())
        key != null
        MetaDataEntry.findWhere(
                dataFile: dataFile1,
                key: key,
                value: md5a,
                source: MetaDataEntry.Source.MDFILE,
        )

        if (includeOptional) {

            // seqTrack2
            SeqTrack seqTrack2 = ExomeSeqTrack.findWhere(
                    laneId: '1_barcode8',
                    insertSize: 0,
                    run: run,
                    sample: sample1,
                    seqType: exomePaired,
                    pipelineVersion: pipeline1,
                    kitInfoReliability: InformationReliability.KNOWN,
                    libraryPreparationKit: kit1,
            )
            assert seqTrack2.ilseId == 1234
            assert DataFile.findWhere(commonRun1DataFileProperties + [
                    fileName   : 's_1_1_',
                    vbpFileName: 's_1_1_',
                    md5sum     : md5b,
                    project    : sample1.project,
                    mateNumber : 1,
                    seqTrack   : seqTrack2,
            ])
            assert DataFile.findWhere(commonRun1DataFileProperties + [
                    fileName   : 's_1_2_',
                    vbpFileName: 's_1_2_',
                    md5sum     : md5c,
                    project    : sample1.project,
                    mateNumber : 2,
                    seqTrack   : seqTrack2,
            ])

            // seqTrack3
            SeqTrack seqTrack3 = ChipSeqSeqTrack.findWhere(
                    laneId: '2_barcode7',
                    insertSize: 234,
                    run: run,
                    sample: sample2,
                    seqType: chipSeqMyPaired,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.KNOWN,
                    libraryPreparationKit: kit2,
                    antibodyTarget: target1,
                    antibody: 'antibody1',
            )
            assert seqTrack3.ilseId == null
            assert DataFile.findWhere(commonRun1DataFileProperties + [
                    fileName   : 's_2_1_',
                    vbpFileName: 's_2_1_',
                    md5sum     : md5d,
                    project    : sample2.project,
                    mateNumber : 1,
                    seqTrack   : seqTrack3,
            ])
            assert DataFile.findWhere(commonRun1DataFileProperties + [
                    fileName   : 's_2_2_',
                    vbpFileName: 's_2_2_',
                    md5sum     : md5e,
                    project    : sample2.project,
                    mateNumber : 2,
                    seqTrack   : seqTrack3,
            ])

            // seqTrack4
            SeqTrack seqTrack4 = ChipSeqSeqTrack.findWhere(
                    laneId: '2_barcode6',
                    insertSize: 0,
                    run: run,
                    sample: sample1,
                    seqType: chipSeqMySingle,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
                    libraryPreparationKit: null,
                    antibodyTarget: target2,
                    antibody: null,
            )
            assert seqTrack4.ilseId == 2345
            assert DataFile.findWhere(commonRun1DataFileProperties + [
                    fileName   : 's_3_1_',
                    vbpFileName: 's_3_1_',
                    md5sum     : md5f,
                    project    : sample1.project,
                    mateNumber : 1,
                    seqTrack   : seqTrack4,
            ])

            // seqTrack5
            SeqTrack seqTrack5 = ExomeSeqTrack.findWhere(
                    laneId: '3',
                    insertSize: 456,
                    run: run,
                    sample: sample2,
                    seqType: exomeSingle,
                    pipelineVersion: pipeline2,
                    kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
                    libraryPreparationKit: null,
            )
            assert seqTrack5.ilseId == null
            assert DataFile.findWhere(commonRun1DataFileProperties + [
                    fileName   : 'fastq_g',
                    vbpFileName: 'fastq_g',
                    md5sum     : md5g,
                    project    : sample2.project,
                    mateNumber : 1,
                    seqTrack   : seqTrack5,
            ])

            // seqTrack6
            SeqTrack seqTrack6 = SeqTrack.findWhere(
                    laneId: '5',
                    insertSize: 0,
                    run: run2,
                    sample: sample2,
                    seqType: mySeqTypeTag,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.UNKNOWN_UNVERIFIED,
                    libraryPreparationKit: null,
            )
            assert seqTrack6.ilseId == null
            assert DataFile.findWhere(commonRun2DataFileProperties + [
                    fileName   : 'fastq_b',
                    vbpFileName: 'fastq_b',
                    md5sum     : md5h,
                    project    : sample2.project,
                    mateNumber : 1,
                    seqTrack   : seqTrack6,
            ])
        }

        seqTrackCount * service.seqTrackService.decideAndPrepareForAlignment(!null)
        seqTrackCount * service.seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(!null, false)

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(SamplePair)

        where:
        runExists | includeOptional | align | importMode
        false     | true            | false | RunSegment.ImportMode.AUTOMATIC
        true      | false           | true  | RunSegment.ImportMode.MANUAL
    }


    void 'extractBarcode, when both BARCODE and FASTQ_FILE columns are missing, returns null'() {
        given:
        Row row = MetadataValidationContextFactory.createContext().spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result == null
    }

    void 'extractBarcode, when BARCODE column is missing and filename contains no barcode, returns null extracted from FASTQ_FILE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\nfile.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(FASTQ_FILE.name())] as Set
    }

    void 'extractBarcode, when BARCODE column is missing and filename contains barcode, returns barcode extracted from FASTQ_FILE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE.name()}\nfile_ACGTACGT_.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(FASTQ_FILE.name())] as Set
    }

    void 'extractBarcode, when no entry in BARCODE column and FASTQ_FILE column is missing, returns null extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("SOME_COLUMN\t${BARCODE}\nsome_value").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when no entry in BARCODE column and filename contains no barcode, returns null extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${BARCODE}\nfile.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when no entry in BARCODE column and filename contains barcode, returns null extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${BARCODE}\nfile_ACGTACGT_.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when BARCODE column contains entry and FASTQ_FILE column is missing, returns barcode extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${BARCODE}\nACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when BARCODE column contains entry and filename contains no barcode, returns barcode extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${BARCODE}\nfile.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when BARCODE column contains entry and filename contains same barcode, returns barcode extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${BARCODE}\nfile_ACGTACGT_.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when BARCODE column contains entry and filename contains barcode which is substring, returns barcode extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${BARCODE}\nfile_CGTACG_.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }

    void 'extractBarcode, when BARCODE column contains entry and filename contains different barcode, returns barcode extracted from BARCODE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${BARCODE}\nfile_TGCATGCA_.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(BARCODE.name())] as Set
    }


    void 'extractMateNumber, when LIBRARY_LAYOUT and FASTQ_FILE columns are missing, returns null'() {
        given:
        Row row = MetadataValidationContextFactory.createContext().spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result == null
    }

    void 'extractMateNumber, when LIBRARY_LAYOUT column is missing and mate number cannot be extracted from filename, returns null'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\nfile.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result == null
    }

    void 'extractMateNumber, when LIBRARY_LAYOUT column is missing and mate number can be extracted from filename, returns mate number extracted from FASTQ_FILE cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\ns_101202_7_2.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == '2'
        result.cells == [row.getCellByColumnTitle(FASTQ_FILE.name())] as Set
    }

    void 'extractMateNumber, when library layout is SINGLE and FASTQ_FILE column is missing, returns 1 extracted from LIBRARY_LAYOUT cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${LIBRARY_LAYOUT}\nSINGLE").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == '1'
        result.cells == [row.getCellByColumnTitle(LIBRARY_LAYOUT.name())] as Set
    }

    void 'extractMateNumber, when library layout is SINGLE and mate number cannot be extracted from filename, returns 1 extracted from LIBRARY_LAYOUT cell'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${LIBRARY_LAYOUT}\nfile.fastq.gz\tSINGLE").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == '1'
        result.cells == [row.getCellByColumnTitle(LIBRARY_LAYOUT.name())] as Set
    }

    void 'extractMateNumber, when library layout is SINGLE and mate number can be extracted from filename, returns 1 extracted from LIBRARY_LAYOUT cell'(String filename) {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${LIBRARY_LAYOUT}\n${filename}\tSINGLE").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == '1'
        result.cells == [row.getCellByColumnTitle(LIBRARY_LAYOUT.name())] as Set

        where:
        filename                | _
        's_101202_7_1.fastq.gz' | _
        's_101202_7_2.fastq.gz' | _
    }

    void 'extractMateNumber, when library layout is not SINGLE and FASTQ_FILE column is missing, returns null'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${LIBRARY_LAYOUT}\nTRIPLE").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result == null
    }

    void 'extractMateNumber, when library layout is not SINGLE and mate number cannot be extracted from filename, returns null'() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${LIBRARY_LAYOUT}\nfile.fastq.gz\tTRIPLE").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result == null
    }

    void 'extractMateNumber, when library layout is not SINGLE and mate number N can be extracted from filename, returns N extracted from FASTQ_FILE cell'(String filename, String mateNumber) {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${LIBRARY_LAYOUT}\n${filename}\tTRIPLE").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == mateNumber
        result.cells == [row.getCellByColumnTitle(FASTQ_FILE.name())] as Set

        where:
        filename                || mateNumber
        's_101202_7_1.fastq.gz' || '1'
        's_101202_7_2.fastq.gz' || '2'
    }

    void 'extractMateNumber, when the mate number in the filename does not match the number in the mate cell, return the number from the mate cell'(String filename, String mateNumber) {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${MATE}\n${filename}\t${mateNumber}").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == mateNumber
        result.cells == [row.getCellByColumnTitle(MATE.name())] as Set

        where:
        filename                || mateNumber
        's_101202_7_2.fastq.gz' || '1'
        's_101202_7_1.fastq.gz' || '2'
    }

    void 'extractMateNumber, when mateNumber can not be parsed to int, return null'(String mateNumber) {
        given:
        Row row = MetadataValidationContextFactory.createContext("${MATE}\t\n${mateNumber}\t").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result == null

        where:
        mateNumber | _
        ''         | _
        'abc'      | _
    }

    void 'copyMetaDataFileIfRequested, if data not on midterm, do nothing'() {
        given:
        MetadataImportService service = new MetadataImportService(
                lsdfFilesService: Mock(LsdfFilesService) {
                    0 * _
                }
        )
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        expect:
        service.copyMetaDataFileIfRequested(context)
    }


    private Map setupForcopyMetaDataFileIfRequested(String contextContent, int numberOfExecution = 1) {
        String ilseId = '1234'
        SeqCenter seqCenter = DomainFactory.createSeqCenter(copyMetadataFile: true)
        String content = """\
                ${ILSE_NO.name()}\t${CENTER_NAME.name()}
                ${ilseId}\t${seqCenter.name}
                """.stripIndent()
        File file = temporaryFolder.newFile('metadataFile.tsv')
        file.text = content
        File target = new File(temporaryFolder.newFolder(), 'target')
        File targetFile = new File(target, file.name)
        DomainFactory.createDefaultRealmWithProcessingOption()

        MetadataImportService service = Spy(MetadataImportService) {
            1 * getIlseFolder(_, _) >> target
        }
        service.lsdfFilesService = Mock(LsdfFilesService) {
            numberOfExecution * createDirectory(_ as File, _ as Realm) >> {
                assert target.mkdir()
            }
        }
        service.remoteShellHelper = Mock(RemoteShellHelper) {
            numberOfExecution * executeCommandReturnProcessOutput(_,_) >> { Realm realm, String cmd ->
                return LocalShellHelper.executeAndWait(cmd)
            }
        }

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                content,
                [
                        directoryStructure: new DataFilesOnGpcfMidTerm(),
                        content           : (contextContent ?: content).bytes,
                        metadataFile      : file.toPath(),
                ]
        )
        return [
                context   : context,
                service   : service,
                target    : target,
                targetFile: targetFile,
        ]
    }

    @ConfineMetaClassChanges([LocalShellHelper])
    void 'copyMetaDataFileIfRequested, if metadata does not exist and copying failed, should fail'() {
        given:
        Map data = setupForcopyMetaDataFileIfRequested('something')

        when:
        data.service.copyMetaDataFileIfRequested(data.context)

        then:
        RuntimeException e = thrown()
        e.message.contains('Copying of metadatafile')
        e.cause.message.contains('targetFile.bytes == context.content')
    }

    @ConfineMetaClassChanges([LocalShellHelper])
    void 'copyMetaDataFileIfRequested, if metadata does not exist and copying fine, all fine'() {
        given:
        Map data = setupForcopyMetaDataFileIfRequested(null)

        when:
        data.service.copyMetaDataFileIfRequested(data.context)

        then:
        data.targetFile.exists()
    }

    @ConfineMetaClassChanges([LocalShellHelper])
    void 'copyMetaDataFileIfRequested, if metadata already exist and content differ, should fail'() {
        given:
        Map data = setupForcopyMetaDataFileIfRequested(null, 0)
        assert data.target.mkdirs()
        data.targetFile.text = 'something'

        when:
        data.service.copyMetaDataFileIfRequested(data.context)

        then:
        RuntimeException e = thrown()
        e.message.contains('Copying of metadatafile')
        e.cause.message.contains('targetFile.bytes == context.content')
    }

    @ConfineMetaClassChanges([LocalShellHelper])
    void 'copyMetaDataFileIfRequested, if metadata already exist and content is the same, do nothing'() {
        given:
        Map data = setupForcopyMetaDataFileIfRequested(null, 0)
        assert data.target.mkdirs()
        data.targetFile.bytes = data.context.content

        when:
        data.service.copyMetaDataFileIfRequested(data.context)

        then:
        data.targetFile.exists()
    }


    void "test getIlseFolder, invalid input, should fail"() {
        given:
        SeqCenter seqCenter = DomainFactory.createSeqCenter()

        when:
        new MetadataImportService().getIlseFolder(input, seqCenter)

        then:
        def e = thrown(AssertionError)
        e.message.contains('assert ilseId =~ /^\\d{4,6}$/')

        where:
        input << [null, '', '123', '1234567', '12a3']
    }

    void "test getIlseFolder, valid input"() {
        given:
        SeqCenter seqCenter = DomainFactory.createSeqCenter(dirName: "SEQ_FACILITY")

        expect:
        output == new MetadataImportService(configService: new TestConfigService((OtpProperty.PATH_SEQ_CENTER_INBOX): "/test")).getIlseFolder(input, seqCenter)

        where:
        input    || output
        '1234'   || new File("/test/SEQ_FACILITY/001/001234")
        '123456' || new File("/test/SEQ_FACILITY/123/123456")
    }
}
