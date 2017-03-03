var exec = require('cordova/exec');

exports.coolMethod = function(path, filterType, success, error) {
    exec(success, error, "ImageFilter", "applyChromeEffect", [path, filterType]);
};
