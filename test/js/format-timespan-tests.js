module("format-timespan");
test("msec", function () {
    equal($.otp.formatTimespan(0), "0 msec");
    equal($.otp.formatTimespan(999), "999 msec");
});
test("sec", function () {
    equal($.otp.formatTimespan(1000), "1 sec 0 msec");
    equal($.otp.formatTimespan(59999), "59 sec 999 msec");
});
test("min", function () {
    equal($.otp.formatTimespan(60000), "1 min 0 sec");
    equal($.otp.formatTimespan(3599999), "59 min 59 sec");
});
test("hour", function () {
    equal($.otp.formatTimespan(3600000), "1 h 0 min");
    equal($.otp.formatTimespan(86399999), "23 h 59 min");
});
test("day", function () {
    equal($.otp.formatTimespan(86400000), "1 day(s) 0 h");
    equal($.otp.formatTimespan(950399000), "10 day(s) 23 h");
});
