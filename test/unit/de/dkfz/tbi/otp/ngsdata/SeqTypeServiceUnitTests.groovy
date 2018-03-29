package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Test

@TestFor(SeqTypeService)
@Build([SeqType])
class SeqTypeServiceUnitTests {

    SeqTypeService seqTypeService

    final static String SEQ_TYPE_NAME = "SeqTypeName"
    final static String SECOND_SEQ_TYPE_NAME = "SecondSeqTypeName"

    final static String SEQ_TYPE_DIR = "SeqTypeDir"

    final static String SEQ_TYPE_DISPLAY_NAME = "SeqTypeDisplayName"

    final static String SEQ_TYPE_LIBRARY_LAYOUT = "SeqTypeLibraryLayout"
    final static String UNKNOWN_LIB_LAYOUT = "Unknown_Lib_Layout"

    final static String SEQ_TYPE_ALIAS = "SeqTypeAlias"
    final static Set<String> SEQ_TYPE_ALIAS_SET = ["SeqTypeAlias"]

    final static boolean SINGLE_CELL_TRUE = true
    final static boolean SINGLE_CELL_FALSE = false


    @Before
    public void setUp() throws Exception {
        seqTypeService = new SeqTypeService()
    }

    @After
    public void tearDown() throws Exception {
        seqTypeService = null
    }

    @Test
    void testAlignableSeqTypes() {
        DomainFactory.createDefaultOtpAlignableSeqTypes()

        List<SeqType> alignableSeqTypes = SeqTypeService.alignableSeqTypes()
        assert 2 == alignableSeqTypes.size()
        assert alignableSeqTypes.find{ it.name == SeqTypeNames.EXOME.seqTypeName && it.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED}
        assert alignableSeqTypes.find{ it.name == SeqTypeNames.WHOLE_GENOME.seqTypeName && it.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED}
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeNameAndSeqTypeDirAndSeqTypeDisplayNameAndSeqTypeAliasAndSingle() {
        assertEquals(
                seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE),
                SeqType.findByNameAndDirNameAndLibraryLayoutAndSingleCell(SEQ_TYPE_NAME, SEQ_TYPE_DIR,  SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        )
    }

    @Test
    void testCreateSeqTypeUsingNullAndSeqTypeDirAndSeqTypeDisplayNameAndSeqTypeAliasAndSingle() {
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(null, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndNullAndSeqTypeDisplayNameAndSeqTypeAliasAndSingle() {
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE_NAME, null, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeDisplayNameAndSeqTypeAliasAndNull() {
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, null, SINGLE_CELL_FALSE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeAliasAndSingleTwice() {
        seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, null, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, null, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeDisplayNameAndSeqTypeAliasAndSingleTwice() {
        seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_FALSE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeDisplayNameAndSeqTypeAliasAndSingleAndSingleCell() {
        assertEquals(
                seqTypeService.createSeqType(SEQ_TYPE_NAME, SEQ_TYPE_DIR, SEQ_TYPE_DISPLAY_NAME, SEQ_TYPE_ALIAS_SET, SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_TRUE),
                SeqType.findByNameAndDirNameAndLibraryLayoutAndSingleCell(SEQ_TYPE_NAME, SEQ_TYPE_DIR,  SeqType.LIBRARYLAYOUT_SINGLE, SINGLE_CELL_TRUE)
        )
    }

    @Test
    void testFindSequencingKitLabelByNameOrAlias_FindByName() {
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS])
        assert seqType == service.findSeqTypeByNameOrAlias(SEQ_TYPE_NAME)
    }

    @Test
    void testFindSequencingKitLabelByNameOrAlias_FindByAlias() {
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS])
        assert seqType == service.findSeqTypeByNameOrAlias(SEQ_TYPE_ALIAS)
    }

    @Test
    void testFindSequencingKitLabelByNameOrAlias_FindNothing_ReturnNull() {
        assert null == service.findSeqTypeByNameOrAlias(SEQ_TYPE_ALIAS)
    }

    @Test
    void testAddNewAliasToSeqType_InputNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError){
            SeqTypeService.addNewAliasToSeqType(null, SEQ_TYPE_ALIAS)
        }
    }

    @Test
    void testAddNewAliasToSeqType_InputAliasIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqTypeService.addNewAliasToSeqType(SEQ_TYPE_NAME, null)
        }
    }

    @Test
    void testAddNewAliasToSeqType_LabelWithInputNameDoesNotExist_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqTypeService.addNewAliasToSeqType(SEQ_TYPE_NAME, SEQ_TYPE_ALIAS)
        }
    }

    @Test
    void testAddNewAliasToSeqType_AliasExistsAlready_ShouldFail() {
        DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS])
        TestCase.shouldFail(AssertionError) {
            SeqTypeService.addNewAliasToSeqType(SEQ_TYPE_NAME, SEQ_TYPE_ALIAS)
        }
    }

    @Test
    void testAddNewAliasToSeqType_AliasExistsForDifferentSeqType() {
        DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [])
        DomainFactory.createSeqType(name: SECOND_SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS])
        TestCase.shouldFail(AssertionError) {
            SeqTypeService.addNewAliasToSeqType(SEQ_TYPE_NAME, SEQ_TYPE_ALIAS)
        }
    }

    @Test
    void testAddNewAliasToSeqType_AllFine() {
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [])
        assert !seqType.alias.contains(SEQ_TYPE_ALIAS)
        SeqTypeService.addNewAliasToSeqType(SEQ_TYPE_NAME, SEQ_TYPE_ALIAS)
        assert seqType.alias.contains(SEQ_TYPE_ALIAS)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindByName() {
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS], libraryLayout: SEQ_TYPE_LIBRARY_LAYOUT)
        assert seqType == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_NAME, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_FALSE)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindByAlias() {
        SeqType seqType = DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS], libraryLayout: SEQ_TYPE_LIBRARY_LAYOUT)
        assert seqType == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_ALIAS, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_FALSE)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindNothing_ReturnNull() {
        assert null == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_NAME, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_FALSE)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindByName_InvalidLibraryLayout_ReturnNull() {
        DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS], libraryLayout: UNKNOWN_LIB_LAYOUT)
        assert null == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_NAME, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_FALSE)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindByAlias_InvalidLibraryLayout_ReturnNull() {
        DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS], libraryLayout: UNKNOWN_LIB_LAYOUT)
        assert null == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_ALIAS, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_FALSE)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindByAlias_WrongSingleCell_ReturnNull() {
        DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS], libraryLayout: UNKNOWN_LIB_LAYOUT, singleCell: SINGLE_CELL_TRUE)
        assert null == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_ALIAS, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_FALSE)
    }

    @Test
    void testFindSeqTypeByNameOrAliasAndLibraryLayout_FindByAlias_SingleCellTrue() {
        DomainFactory.createSeqType(name: SEQ_TYPE_NAME, alias: [SEQ_TYPE_ALIAS], libraryLayout: UNKNOWN_LIB_LAYOUT, singleCell: SINGLE_CELL_TRUE)
        assert null == service.findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(SEQ_TYPE_ALIAS, SEQ_TYPE_LIBRARY_LAYOUT, SINGLE_CELL_TRUE)
    }
}
