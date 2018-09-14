import de.dkfz.tbi.otp.ngsdata.*

Project project = DomainFactory.createProject(name: "AProject", realm: Realm.first())
project.validate()
project.save()
