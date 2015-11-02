(function (win) {
    var method = win.$L;
    win.$L= function (code) {
        var message = method(code);
        var splitPattern = /(\{\d+})/g;
        if (message == undefined) {
            return "[" + code + "]";
        } else if (splitPattern.test(message)) {
            var params;
            if (arguments.length === 2 && (arguments[1]) instanceof Array) {
                params = arguments[1];
            } else {
                params = Array.prototype.slice.call(arguments);
                params.shift();
            }
            message = message.split(splitPattern);
            var result = "";
            var pattern = /\{(\d+)}/;
            for (var i = 0; i < message.length; i++) {
                if (pattern.test(message[i])) {
                    result += params[message[i].match(pattern)[1]];
                } else {
                    result += message[i];
                }
            }
            return result;
        }
        else {
            return message
        }
    }
}(this));