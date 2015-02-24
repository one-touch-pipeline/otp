import de.dkfz.tbi.otp.ngsdata.*

println "HELLO"
Project project = TestData.createProject(name: "HIPO", dirName: "dirName", realmName: "DKFZ")
project.validate()
project.save()
println "created project ${project}"
