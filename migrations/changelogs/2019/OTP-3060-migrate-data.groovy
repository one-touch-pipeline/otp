import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.dataprocessing.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.ngsdata.*

databaseChangeLog = {
    changeSet(author: "kosnac", id: "OTP-3060-MIGRATION-SQL") {
        grailsChange {
            change {
                SeqTrack.withNewSession {
                    SampleIdentifier.createCriteria().list {
                        sample {
                            individual {
                                project {
                                    eq('sampleIdentifierParserBeanName', SampleIdentifierParserBeanName.HIPO2)
                                }
                            }
                        }
                    }.each { SampleIdentifier sampleIdentifier ->
                        SeqTrack.createCriteria().list {
                            eq('sample', sampleIdentifier.sample)
                            seqType {
                                eq('singleCell', true)
                            }
                        }.each { SeqTrack seqTrack ->
                            seqTrack.cellPosition = ctx.hipo2SampleIdentifierParser.tryParseCellPosition(sampleIdentifier.name)
                            seqTrack.save(flush: true)
                        }
                    }
                }
            }
            checkSum "OTP-3060-MIGRATION-SQL"
        }
    }
}
