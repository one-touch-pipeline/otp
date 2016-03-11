package overviews

/*
 * This script is used to create the OTRS notification email for an ilseid or a list of ilseids
 */

String ilseIds = """
"""

List ilseIdList = ilseIds.split("\n").findAll()

println ctx.ilseNotificationService.createIlseNotificationForIlseIds(ilseIdList)

''

