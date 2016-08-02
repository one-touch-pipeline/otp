import de.dkfz.tbi.otp.ngsdata.*

RunSegment.withTransaction {
    RunSegment runSegment = RunSegment.get()
    assert runSegment
    DataFile.findAllByRunSegment(runSegment).each {
        MetaDataEntry.findAllByDataFile(it).each {
            it.delete()
        }
        assert MetaDataEntry.countByDataFile(it) == 0
        it.delete()
    }
    assert DataFile.countByRunSegment(runSegment) == 0
    MetaDataFile.findAllByRunSegment(runSegment).each {
        it.delete()
    }
    assert MetaDataFile.countByRunSegment(runSegment) == 0
    def runSegmentId = runSegment.id
    assert RunSegment.get(runSegmentId) == runSegment
    runSegment.delete()
    assert RunSegment.get(runSegmentId) == null
    assert false
}
''
