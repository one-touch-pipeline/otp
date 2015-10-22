package operations.libraryPreperationKit

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils


String libraryName = ''
String newAlias = ''

//---------------------------------

assert libraryName
assert newAlias

LibraryPreparationKitSynonym.withTransaction {

    LibraryPreparationKit libraryPreparationKit = CollectionUtils.exactlyOneElement(LibraryPreparationKit.findAllByName(libraryName))

    LibraryPreparationKitSynonym libraryPreparationKitSynonym = LibraryPreparationKitSynonym.findByName(newAlias)
    assert !libraryPreparationKitSynonym


    libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
            libraryPreparationKit: libraryPreparationKit,
            name: newAlias,
    )

    assert libraryPreparationKitSynonym.save(flush: true, failOnError: true)
    println libraryPreparationKitSynonym
}
''

