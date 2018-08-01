import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*

Project project = DomainFactory.createProject(name: "AProject", realm: ConfigService.getDefaultRealm())
project.validate()
project.save()
