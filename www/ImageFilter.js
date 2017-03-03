var exec = require('cordova/exec');

exports.applyChromeEffect = function(path, filterType, success, error) {
    exec(success, error, "ImageFilter", "applyChromeEffect", [path, filterType]);
};
