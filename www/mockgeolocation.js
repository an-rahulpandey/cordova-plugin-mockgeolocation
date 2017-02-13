var exec = require('cordova/exec');

exports.check = function(args, success, error) {
    exec(success, error, "mockgeolocation", "check", [args]);
};
