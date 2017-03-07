//
//  ImageFilter.m
//  HelloWorld
//
//  Created by dat tran on 3/3/17.
//
//

#import "ImageFilter.h"

@implementation ImageFilter

static CIContext *context;
static UIImage *currentEditingImage;
static NSString *currentImagePath = nil;

//CIPhotoEffectChrome
//CIPhotoEffectFade
//CIPhotoEffectInstant
//CIPhotoEffectMono
//CIPhotoEffectNoir
//CIPhotoEffectProcess
//CIPhotoEffectTonal
//CIPhotoEffectTransfer

static NSString* toBase64(NSData* data) {
    SEL s1 = NSSelectorFromString(@"cdv_base64EncodedString");
    SEL s2 = NSSelectorFromString(@"base64EncodedString");
    SEL s3 = NSSelectorFromString(@"base64EncodedStringWithOptions:");
    
    if ([data respondsToSelector:s1]) {
        NSString* (*func)(id, SEL) = (void *)[data methodForSelector:s1];
        return func(data, s1);
    } else if ([data respondsToSelector:s2]) {
        NSString* (*func)(id, SEL) = (void *)[data methodForSelector:s2];
        return func(data, s2);
    } else if ([data respondsToSelector:s3]) {
        NSString* (*func)(id, SEL, NSUInteger) = (void *)[data methodForSelector:s3];
        return func(data, s3, 0);
    } else {
        return nil;
    }
}

- (void)pluginInitialize {
    [super pluginInitialize];
    
    EAGLContext *eaglCxt = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    context = [CIContext contextWithEAGLContext:eaglCxt];
}

- (void)validateInput:(CDVInvokedUrlCommand *)command {
    NSString *path = [command argumentAtIndex:0 withDefault:nil];
    NSURL *pathUrl = [NSURL URLWithString:path];
    path = pathUrl.path;
    NSString *filterType = [command argumentAtIndex:1 withDefault:nil];
    if (path.length && filterType.length) {
        if (![currentImagePath isEqualToString:path]) {
            currentImagePath = path;
            currentEditingImage = [UIImage imageWithContentsOfFile:path];
        }
    }
}

- (void)applyEffect:(CDVInvokedUrlCommand*)command {
    [self validateInput:command];
    
    NSString *filterType = [command argumentAtIndex:1 withDefault:nil];
    NSNumber *compressionQuality = [command argumentAtIndex:2 withDefault:@(0.5)];

    [self filterImage:currentEditingImage filter:filterType compressionQuality:compressionQuality completion:^(NSData *data) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
        dispatch_sync(dispatch_get_main_queue(), ^{
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        });
    }];
}

- (void)filterImage:(UIImage *)image filter:(NSString *)filterType compressionQuality:(NSNumber *)quality completion:(void(^)(NSData *))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @autoreleasepool {
            CIImage *ciImage = [[CIImage alloc] initWithImage:image];

            ciImage = [self applySelectedEffect:ciImage effect:filterType];

            CGImageRef ref = [context createCGImage:ciImage fromRect:ciImage.extent];
            UIImage *img = [[UIImage alloc] initWithCGImage:ref];
            NSData *data = UIImageJPEGRepresentation(img, [quality floatValue]);
            completion(data);
            CFRelease(ref);
        }
    });
}

- (CIImage *)applySelectedEffect:(CIImage *)image effect:(NSString *)effect {
    if ([effect isEqualToString:@"aged"])
        return [self applyAgedEffect:image];
    else if ([effect isEqualToString:@"blackWhite"])
        return [self applyBlackWhiteEffect:image];
    else if ([effect isEqualToString:@"cold"])
        return [self applyColdEffect:image];
    else if ([effect isEqualToString:@"rosy"])
        return [self applyRosyEffect:image];
    else if ([effect isEqualToString:@"intense"])
        return [self applyIntenseEffect:image];
    else
        return nil;
}

- (CIImage *)applyAgedEffect:(CIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.4072f)
                  brightness:@(-0.1495f)
                    contrast:@(0.768f)
                       gamma:nil
                    exposure:@(0.8866f)
                     sharpen:nil
                         hue:nil
                         red:@(0.8222165f)
                       green:@(0.6469f)
                        blue:@(0.5232f)];
}

- (CIImage *)applyBlackWhiteEffect:(CIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.0f)
                  brightness:nil
                    contrast:nil
                       gamma:nil
                    exposure:@(0.64433f)
                     sharpen:@(0.8763f)
                         hue:nil
                         red:nil
                       green:nil
                        blue:nil];
}

- (CIImage *)applyColdEffect:(CIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.3093f)
                  brightness:@(-0.0722f)
                    contrast:@(0.8763f)
                       gamma:@(1.0979f)
                    exposure:@(0.9639f)
                     sharpen:nil
                         hue:nil
                         red:@(0.451f)
                       green:@(0.6881f)
                        blue:nil];
}

- (CIImage *)applyRosyEffect:(CIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.76289f)
                  brightness:nil
                    contrast:nil
                       gamma:nil
                    exposure:@(0.58763f)
                     sharpen:nil
                         hue:nil
                         red:nil
                       green:@(0.66495f)
                        blue:@(0.8995f)];
}

- (CIImage *)applyIntenseEffect:(CIImage *)img {
    return [self applyFilter:img
                  saturation:@(1.618557f)
                  brightness:@(-0.1495f)
                    contrast:@(0.76804f)
                       gamma:nil
                    exposure:@(0.8866f)
                     sharpen:nil
                         hue:nil
                         red:nil
                       green:@(0.83247f)
                        blue:@(0.8505f)];
}

- (CIImage *)applyNoEffect:(CIImage *)img {
    return [self applyFilter:img
                  saturation:nil
                  brightness:nil
                    contrast:nil
                       gamma:nil
                    exposure:nil
                     sharpen:nil
                         hue:nil
                         red:nil
                       green:nil
                        blue:nil];
}

- (CIImage *)applyFilter:(CIImage*)image saturation:(NSNumber *)saturation brightness:(NSNumber*)brightness contrast:(NSNumber*)contrast gamma:(NSNumber*)gamma exposure:(NSNumber*)exposure sharpen:(NSNumber*)sharpen hue:(NSNumber*)hue red:(NSNumber*)red green:(NSNumber*)green blue:(NSNumber*)blue {

    CIImage *result = image;

    if (saturation)
        result = [self applySaturation:result saturation:saturation];
    if (brightness)
        result = [self applyBrightness:result brightness:brightness];
    if (contrast)
        result = [self applyContrast:result contrast:contrast];
    if (gamma)
        result = [self applyGamma:result value:gamma];
    if (exposure)
        result = [self applyExposure:result value:exposure];
    if (sharpen)
        result = [self applySharpen:result value:sharpen];
    if (hue)
        result = [self applyHue:result value:hue];
    if (red)
        result = [self applyRed:result value:red];
    if (green)
        result = [self applyGreen:result value:green];
    if (blue)
        result = [self applyBlue:result value:blue];

    return result;
}

- (CIImage *)applyBrightness:(CIImage *)image brightness:(NSNumber*)brightness {
    return [image imageByApplyingFilter:@"CIColorControls" withInputParameters:@{kCIInputBrightnessKey: brightness}];
}
- (CIImage *)applySaturation:(CIImage *)image saturation:(NSNumber*)saturation {
    return [image imageByApplyingFilter:@"CIColorControls" withInputParameters:@{kCIInputSaturationKey: saturation}];
}
- (CIImage *)applyContrast:(CIImage *)image contrast:(NSNumber*)contrast {
    return [image imageByApplyingFilter:@"CIColorControls" withInputParameters:@{kCIInputContrastKey: contrast}];
}

- (CIImage *)applyExposure:(CIImage *)image value:(NSNumber*)value {
    return [image imageByApplyingFilter:@"CIExposureAdjust" withInputParameters:@{kCIInputEVKey: value}];
}

- (CIImage *)applyGamma:(CIImage *)image value:(NSNumber*)value {
    return [image imageByApplyingFilter:@"CIGammaAdjust" withInputParameters:@{@"inputPower": value}];
}

- (CIImage *)applySharpen:(CIImage *)image value:(NSNumber*)value {
    return [image imageByApplyingFilter:@"CISharpenLuminance" withInputParameters:@{kCIInputSharpnessKey: value}];
}

- (CIImage *)applyHue:(CIImage *)image value:(NSNumber*)value {
    return [image imageByApplyingFilter:@"CIHueAdjust" withInputParameters:@{kCIInputAngleKey: value}];
}

- (CIImage*)applyRed:(CIImage*)image value:(NSNumber *)value {
    CIVector *vector = [CIVector vectorWithX:[value floatValue]];
    return [image imageByApplyingFilter:@"CIColorMatrix" withInputParameters:@{@"inputRVector": vector}];
}
- (CIImage*)applyGreen:(CIImage*)image value:(NSNumber *)value {
    CIVector *vector = [CIVector vectorWithX:0 Y:[value floatValue]];
    return [image imageByApplyingFilter:@"CIColorMatrix" withInputParameters:@{@"inputGVector": vector}];
}
- (CIImage*)applyBlue:(CIImage*)image value:(NSNumber *)value {
    CIVector *vector = [CIVector vectorWithX:0 Y:0 Z:[value floatValue]];
    return [image imageByApplyingFilter:@"CIColorMatrix" withInputParameters:@{@"inputBVector": vector}];
}

@end
