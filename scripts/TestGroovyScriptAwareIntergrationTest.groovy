import de.dkfz.tbi.otp.ngsdata.*

Project project = DomainFactory.createProject(name: "HIPO", dirName: "dirName", realmName: "DKFZ")
project.validate()
project.save()
