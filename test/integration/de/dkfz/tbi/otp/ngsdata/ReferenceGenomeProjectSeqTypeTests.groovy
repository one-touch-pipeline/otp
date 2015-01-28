package de.dkfz.tbi.otp.ngsdata

import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase

class ReferenceGenomeProjectSeqTypeTests {

    Project project
    SeqType seqType
    SampleType sampleType
    ReferenceGenome referenceGenome



    @Before
    void setup() {
        project = Project.build()
        seqType = SeqType.build()
        referenceGenome = ReferenceGenome.build()
        sampleType = null
    }



    private ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Date deprecatedDate, boolean doSave = true) {
        ReferenceGenomeProjectSeqType domain = new ReferenceGenomeProjectSeqType(
                        project: project,
                        seqType: seqType,
                        referenceGenome: referenceGenome,
                        sampleType: sampleType,
                        deprecatedDate: deprecatedDate)
        if (doSave) {
            domain.save(flush: true)
        }
        return domain
    }



    @Test
    void testUnique_NoDuplication_WithDifferentProjects() {
        createReferenceGenomeProjectSeqType(null)
        project = Project.build()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }


    @Test
    void testUnique_NoDuplication_WithDifferentSeqType() {
        createReferenceGenomeProjectSeqType(null)
        seqType = SeqType.build()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }


    @Test
    void testUnique_NoDuplication_WithAndWithoutSampleType() {
        createReferenceGenomeProjectSeqType(null)
        sampleType = SampleType.build()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }


    @Test
    void testUnique_NoDuplication_WithDifferentSampleType() {
        sampleType = SampleType.build()
        createReferenceGenomeProjectSeqType(null)
        sampleType = SampleType.build()

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }


    @Test
    void testUnique_NoDuplication_WithDeprecatedDateAndWithoutSampleType() {
        createReferenceGenomeProjectSeqType(new Date())

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }


    @Test
    void testUnique_NoDuplication_WithDeprecatedDateAndSampleType() {
        sampleType = SampleType.build()
        createReferenceGenomeProjectSeqType(new Date())

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        assert newDomain.validate()
    }



    @Test
    void testUnique_HasDuplication_WithoutSampleType() {
        createReferenceGenomeProjectSeqType(null)

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "referenceGenome", "validator.invalid", referenceGenome)
    }


    @Test
    void testUnique_HasDuplication_WithSampleType() {
        sampleType = SampleType.build()
        createReferenceGenomeProjectSeqType(null)

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "referenceGenome", "validator.invalid", referenceGenome)
    }


    @Test
    void testProjectIsNull() {
        project = null

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "project", "nullable", project)
    }


    @Test
    void testSeqTypeIsNull() {
        seqType = null

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "seqType", "nullable", seqType)
    }


    @Test
    void testReferenceGenomeIsNull() {
        referenceGenome = null

        ReferenceGenomeProjectSeqType newDomain = createReferenceGenomeProjectSeqType(null, false)
        TestCase.assertValidateError(newDomain, "referenceGenome", "nullable", referenceGenome)
    }

}
