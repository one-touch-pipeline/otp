module("format-timespan");
test("msec", function () {
    equal($.otp.workflows.formatTimespan(0), "0 msec");
    equal($.otp.workflows.formatTimespan(999), "999 msec");
});
test("sec", function () {
    equal($.otp.workflows.formatTimespan(1000), "1 sec 0 msec");
    equal($.otp.workflows.formatTimespan(59999), "59 sec 999 msec");
});
test("min", function () {
    equal($.otp.workflows.formatTimespan(60000), "1 min 0 sec");
    equal($.otp.workflows.formatTimespan(3599999), "59 min 59 sec");
});
test("hour", function () {
    equal($.otp.workflows.formatTimespan(3600000), "1 h 0 min");
    equal($.otp.workflows.formatTimespan(86399999), "23 h 59 min");
});
test("day", function () {
    equal($.otp.workflows.formatTimespan(86400000), "1 day(s) 0 h");
    equal($.otp.workflows.formatTimespan(950399000), "10 day(s) 23 h");
});
