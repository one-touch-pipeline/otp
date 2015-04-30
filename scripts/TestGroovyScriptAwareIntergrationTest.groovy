import de.dkfz.tbi.otp.ngsdata.*

Project project = TestData.createProject(name: "HIPO", dirName: "dirName", realmName: "DKFZ")
project.validate()
project.save()
