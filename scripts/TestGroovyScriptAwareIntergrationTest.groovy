import de.dkfz.tbi.otp.ngsdata.*

Project project = DomainFactory.createProject(name: "HIPO", dirName: "dirName", realm: ConfigService.getDefaultRealm())
project.validate()
project.save()
