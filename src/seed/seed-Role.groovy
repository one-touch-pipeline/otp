import de.dkfz.tbi.otp.security.Role

seed = {
    [
            Role.ROLE_USER,
            Role.ROLE_ADMIN,
            Role.ROLE_OPERATOR,
            Role.ROLE_SWITCH_USER,
    ].each { String name ->
        role(
                meta: [
                        key   : 'authority',
                        update: 'false',
                ],
                authority: name,
        )
    }
}
