import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.*
import de.dkfz.tbi.otp.dataprocessing.*

String realmName

assert realmName

ProcessingOptionService processingOptionService = ctx.ProcessingOptionService

//create default realm
Realm.withTransaction {

    Realm realm = new Realm([
            name                       : realmName,
            jobScheduler               : Realm.JobScheduler.PBS,
            host                       : 'test.host.invalid',
            port                       : -1,
            timeout                    : -1,
            defaultJobSubmissionOptions: '',
    ])

    assert realm.save(flush: true)

    processingOptionService.createOrUpdate(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, realmName)

}
''
