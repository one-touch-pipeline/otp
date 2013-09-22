package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(ExomeEnrichmentKitService)
@Mock([ExomeEnrichmentKitService,
    ExomeEnrichmentKit, ExomeEnrichmentKitIdentifier])
class ExomeEnrichmentKitServiceUnitTests {

    ExomeEnrichmentKitService exomeEnrichmentKitService

    final static String EXOME_ENRICHMENT_KIT ="ExomeEnrichmentKit"

    final static String EXOME_ENRICHMENT_KIT_IDENTIFIER ="ExomeEnrichmentKitIdentifier"

    @Before
    public void setUp() throws Exception {
        exomeEnrichmentKitService = new ExomeEnrichmentKitService()
    }

    @After
    public void tearDown() throws Exception {
        exomeEnrichmentKitService = null
    }

    void testFindExomeEnrichmentKitByNameOrAliasUsingKitName() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        assertEquals(exomeEnrichmentKitIdentifier.exomeEnrichmentKit, exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(EXOME_ENRICHMENT_KIT))
    }

    void testFindExomeEnrichmentKitByNameOrAliasUsingAiasName() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        assertEquals(exomeEnrichmentKitIdentifier.exomeEnrichmentKit, exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(EXOME_ENRICHMENT_KIT_IDENTIFIER))
    }

    void testFindExomeEnrichmentKitByNameOrAliasUsingUnknownName() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        assertNull(exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias("UNKNOWN"))
    }

    void testFindExomeEnrichmentKitByNameOrAliasUsingNull() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        shouldFail(IllegalArgumentException) { exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(null) }
    }

    private ExomeEnrichmentKitIdentifier createExomeEnrichmentKitIdentifier() {
        ExomeEnrichmentKit exomeEnrichmentKit = new ExomeEnrichmentKit(
                        name: EXOME_ENRICHMENT_KIT
                        )
        assertNotNull(exomeEnrichmentKit.save([flush: true]))
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = new ExomeEnrichmentKitIdentifier(
                        name: EXOME_ENRICHMENT_KIT_IDENTIFIER,
                        exomeEnrichmentKit: exomeEnrichmentKit)
        assertNotNull(exomeEnrichmentKitIdentifier.save([flush: true]))
        return exomeEnrichmentKitIdentifier
    }
}

