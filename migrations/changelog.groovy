databaseChangeLog = {

	changeSet(author: "strubelp", id:"initial_database_dump") {
		sqlFile(path: 'changelogs/2018/initial_database_dump.sql')
	}

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

	changeSet(author: "strubelp", id:"OTP-1837-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-1837.sql')
	}

	include file: 'changelogs/2018/OTP-2790.groovy'

	changeSet(author: "strubelp", id:"OTP-2790") {
		sqlFile(path: 'changelogs/2018/OTP-2790.sql')
	}

	changeSet(author: "strubelp", id:"OTP-2755") {
		sqlFile(path: 'changelogs/2018/OTP-2755.sql')
	}

	include file: 'changelogs/2018/bugfix-datafile-inconsistency.groovy'

	changeSet(author: "strubelp", id: "bugfix-datafile-inconsistency-sql") {
		sqlFile(path: 'changelogs/2018/bugfix-datafile-inconsistency.sql')
	}

	include file: 'changelogs/2018/OTP-2778.groovy'

	changeSet(author: "klinga", id:"OTP-2778") {
		sqlFile(path: 'changelogs/2018/OTP-2778.sql')
	}

	changeSet(author: "strubelp", id:"OTP-2848") {
		sqlFile(path: 'changelogs/2018/OTP-2848.sql')
	}

	include file: 'changelogs/2018/OTP-2854.groovy'

	changeSet(author: "strubelp", id:"OTP-2854-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2854.sql')
	}

	include file: 'changelogs/2018/OTP-2876.groovy'

	changeSet(author: "klinga", id:"OTP-2795-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2795.sql')
	}

	include file: 'changelogs/2018/OTP-2871.groovy'

	changeSet(author: "klinga", id:"OTP-2820-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2820.sql')
	}

	include file: 'changelogs/2018/OTP-2894.groovy'

	changeSet(author: "klinga", id:"OTP-2877-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2877.sql')
	}

	include file: 'changelogs/2018/OTP-2877.groovy'

	changeSet(author: "strubelp", id:"OTP-2785") {
		sqlFile(path: 'changelogs/2018/OTP-2785.sql')
	}

	changeSet(author: "strubel", id:"OTP-2895-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2895.sql')
	}

	include file: 'changelogs/2018/yapsa-notification-fix.groovy'

	include file: 'changelogs/2018/OTP-2856.groovy'

	changeSet(author: "strubelp", id:"OTP-2856-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2856.sql')
	}

	changeSet(author: "strubel", id:"OTP-2919-SQL") {
		sqlFile(path: 'changelogs/2018/OTP-2919.sql')
	}
}
