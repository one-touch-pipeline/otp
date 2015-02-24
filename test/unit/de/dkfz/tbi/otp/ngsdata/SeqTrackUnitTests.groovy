package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


@TestMixin(GrailsUnitTestMixin)
@TestFor(SeqTrack)
@Mock([DataFile, Sample, Individual, Project])
class SeqTrackUnitTests {

    void testIsWithDrawn() {
        SeqTrack seqTrack = new SeqTrack()
        assert null != seqTrack.save(validate: false)
        assertFalse seqTrack.withdrawn

        DataFile dataFile = new DataFile(seqTrack: seqTrack, fileWithdrawn: false)
        assert null != dataFile.save(validate: false)
        assertFalse seqTrack.withdrawn

        dataFile = new DataFile(seqTrack: seqTrack, fileWithdrawn: false)
        assert null != dataFile.save(validate: false)
        assertFalse seqTrack.withdrawn

        dataFile.fileWithdrawn = true
        assert null != dataFile.save(validate: false)
        assertTrue seqTrack.withdrawn
    }

    void testGetProject() {
        Project project = TestData.createProject()
        assert null != project.save(validate: false)

        Individual individual = new Individual (project: project)
        assert null != individual.save(validate: false)

        Sample sample = new Sample(individual: individual)
        assert null != sample.save(validate: false)

        SeqTrack seqTrack = new SeqTrack(sample: sample)
        assert null != seqTrack.save(validate: false)

        assertEquals(seqTrack.project, project)
    }
}
