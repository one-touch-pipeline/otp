databaseChangeLog = {

	changeSet(author: "strubelp", id:"OTP-2633-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2633.sql')
	}

	changeSet(author: "strubelp", id:"fixed-alias-tables-sql") {
		sqlFile(path: 'changelogs/2018/fixed-alias-tables.sql')
	}

	changeSet(author: "kaercher", id:"OTP-2793") {
		sqlFile(path: 'changelogs/2018/otp2793.sql')
	}

	include file: 'changelogs/2018/OTP-2791.groovy'
}
