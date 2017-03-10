var exec = require('cordova/exec');

exports.applyEffect = function(path, filterType, compressQuality, isBase64Image, success, error) {
    exec(success, error, "ImageFilter", "applyEffect", [path, filterType, compressQuality, isBase64Image]);
};

exports.applyEffectForReview = function(path, filterType, compressQuality, isBase64Image, success, error) {
    exec(success, error, "ImageFilter", "applyEffectForReview", [path, filterType, compressQuality, isBase64Image]);
};

exports.applyEffectForThumbnail = function(path, filterType, compressQuality, isBase64Image, success, error) {
    exec(success, error, "ImageFilter", "applyEffectForThumbnail", [path, filterType, compressQuality, isBase64Image]);
};
