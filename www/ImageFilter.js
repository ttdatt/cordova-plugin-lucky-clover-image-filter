var exec = require('cordova/exec');

exports.applyEffect = function(path, filterType, compressQuality, success, error) {
    exec(success, error, "ImageFilter", "applyEffect", [path, filterType, compressQuality]);
};
