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
static UIImage *currentPreviewImage;
static UIImage *currentThumbnailImage;
static NSString *currentImagePath = nil;
static NSString *base64Image = nil;
static CGSize screenSize;


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

static UIImage * base64ToImage(NSString *base64Image) {
    NSData *imageData = [[NSData alloc] initWithBase64EncodedString:base64Image options:0];
    return [UIImage imageWithData:imageData];
}

- (void)pluginInitialize {
    [super pluginInitialize];

    EAGLContext *eaglCxt = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    context = [CIContext contextWithEAGLContext:eaglCxt];

    screenSize = [[UIScreen mainScreen] bounds].size;
}

- (void)validateInput:(CDVInvokedUrlCommand *)command {
    NSString *pathOrData = [command argumentAtIndex:0 withDefault:nil];
    NSURL *pathUrl = [NSURL URLWithString:pathOrData];
    pathOrData = pathUrl.path;
    NSString *filterType = [command argumentAtIndex:1 withDefault:nil];
    NSNumber *isBase64Image = [command argumentAtIndex:3 withDefault:@(0)];

    if ([isBase64Image intValue] == 0)
    {
        if (pathOrData.length && filterType.length && ![currentImagePath isEqualToString:pathOrData]) {
            currentImagePath = pathOrData;
            currentEditingImage = [UIImage imageWithContentsOfFile:pathOrData];

            float ratio = screenSize.width / currentEditingImage.size.width;
            CGSize newSize = CGSizeMake(currentEditingImage.size.width * ratio, currentEditingImage.size.height * ratio);
            UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
            [currentEditingImage drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
            currentPreviewImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();

            CGSize thumbnailSize = CGSizeMake(currentPreviewImage.size.width * 0.2f, currentPreviewImage.size.height * 0.2f);
            UIGraphicsBeginImageContextWithOptions(thumbnailSize, NO, 0.0);
            [currentPreviewImage drawInRect:CGRectMake(0, 0, thumbnailSize.width, thumbnailSize.height)];
            currentThumbnailImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
        }
    }
    else if ([isBase64Image intValue] == 1) {
        if (pathOrData.length && filterType.length && ![pathOrData isEqualToString:base64Image]) {
            base64Image = pathOrData;
            currentEditingImage = base64ToImage(pathOrData);

            float ratio = screenSize.width / currentEditingImage.size.width;
            CGSize newSize = CGSizeMake(currentEditingImage.size.width * ratio, currentEditingImage.size.height * ratio);
            UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
            [currentEditingImage drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
            currentPreviewImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();

            CGSize thumbnailSize = CGSizeMake(currentPreviewImage.size.width * 0.2f, currentPreviewImage.size.height * 0.2f);
            UIGraphicsBeginImageContextWithOptions(thumbnailSize, NO, 0.0);
            [currentPreviewImage drawInRect:CGRectMake(0, 0, thumbnailSize.width, thumbnailSize.height)];
            currentThumbnailImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
        }
    }
    else {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
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

- (void)applyEffectForReview:(CDVInvokedUrlCommand*)command {
    [self validateInput:command];

    NSString *filterType = [command argumentAtIndex:1 withDefault:nil];
    NSNumber *compressionQuality = [command argumentAtIndex:2 withDefault:@(0.5)];

    [self filterImage:currentPreviewImage filter:filterType compressionQuality:compressionQuality completion:^(NSData *data) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
        dispatch_sync(dispatch_get_main_queue(), ^{
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        });
    }];
}

- (void)applyEffectForThumbnail:(CDVInvokedUrlCommand *)command {
    [self validateInput:command];

    NSString *filterType = [command argumentAtIndex:1 withDefault:nil];
    NSNumber *compressionQuality = [command argumentAtIndex:2 withDefault:@(0.5)];

    [self filterImage:currentThumbnailImage filter:filterType compressionQuality:compressionQuality completion:^(NSData *data) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
        dispatch_sync(dispatch_get_main_queue(), ^{
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        });
    }];
}

- (void)filterImage:(UIImage *)image filter:(NSString *)filterType compressionQuality:(NSNumber *)quality completion:(void(^)(NSData *))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @autoreleasepool {
            UIImage *result = [self applySelectedEffect:image effect:filterType];
            NSData *data = UIImageJPEGRepresentation(result, [quality floatValue]);
            completion(data);
        }
    });
}

- (UIImage *)applySelectedEffect:(UIImage *)image effect:(NSString *)effect {
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
    else if ([effect isEqualToString:@"warm"])
        return [self applyWarmEffect:image];
    else if ([effect isEqualToString:@"light"])
        return [self applyLightEffect:image];
    else
        return nil;
}

- (UIImage *)applyAgedEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.00516)
                  brightness:@(0.04124)
                    contrast:@(0.8763)
                       gamma:@(0.7474)
                    exposure:@(0.1804)
                     sharpen:@(1.0103)
                         hue:nil
                         red:@(0.7835)
                       green:@(0.719)
                        blue:@(0.616)];
}

- (UIImage *)applyBlackWhiteEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.0f)
                  brightness:nil
                    contrast:nil
                       gamma:@(1.2282)
                    exposure:@(0.2062)
                     sharpen:@(0.268)
                         hue:nil
                         red:nil
                       green:nil
                        blue:nil];
}

- (UIImage *)applyColdEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.216495)
                  brightness:@(-0.134021)
                    contrast:@(0.85567)
                       gamma:@(1.061856)
                    exposure:@(0.603093)
                     sharpen:nil
                         hue:nil
                         red:@(0.708763)
                       green:@(0.832474)
                        blue:nil];
}

- (UIImage *)applyRosyEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(0.79897)
                  brightness:@(-0.164948)
                    contrast:@(0.819588)
                       gamma:@(0.881443)
                    exposure:@(0.474227)
                     sharpen:nil
                         hue:nil
                         red:nil
                       green:@(0.822165)
                        blue:@(0.876289)];
}

- (UIImage *)applyIntenseEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(2.0)
                  brightness:nil
                    contrast:nil
                       gamma:nil
                    exposure:@(0.12371)
                     sharpen:nil
                         hue:nil
                         red:nil
                       green:nil
                        blue:nil];
}

- (UIImage *)applyWarmEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(1.2577)
                  brightness:@(-0.085)
                    contrast:@(0.964)
                       gamma:@(0.8763)
                    exposure:@(0.4536)
                     sharpen:nil
                         hue:nil
                         red:@(0.83)
                       green:@(0.8092)
                        blue:@(0.7938)];
}

- (UIImage *)applyLightEffect:(UIImage *)img {
    return [self applyFilter:img
                  saturation:@(1.4484)
                  brightness:@(-0.0592)
                    contrast:@(0.7629)
                       gamma:@(0.7835)
                    exposure:@(0.4124)
                     sharpen:@(-0.0825)
                         hue:nil
                         red:nil
                       green:nil
                        blue:nil];
}

- (UIImage *)applyNoEffect:(UIImage *)img {
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

- (UIImage *)applyFilter:(UIImage *)image saturation:(NSNumber *)saturation brightness:(NSNumber *)brightness contrast:(NSNumber *)contrast gamma:(NSNumber *)gamma exposure:(NSNumber *)exposure sharpen:(NSNumber *)sharpen hue:(NSNumber *)hue red:(NSNumber *)red green:(NSNumber *)green blue:(NSNumber *)blue {

    UIImage *result;
    NSMutableArray<GPUImageFilter*> *filters = [NSMutableArray new];
    GPUImageFilter *tmp;
    if (saturation) {
        GPUImageFilter *filter = [self applySaturation:result saturation:saturation];
        [filters addObject:filter];
        tmp = filter;
    }
    if (brightness) {
        GPUImageFilter *filter = [self applyBrightness:result brightness:brightness];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (contrast) {
        GPUImageFilter *filter = [self applyContrast:result contrast:contrast];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (gamma){
        GPUImageFilter *filter = [self applyGamma:result value:gamma];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (exposure){
        GPUImageFilter *filter = [self applyExposure:result value:exposure];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (sharpen){
        GPUImageFilter *filter = [self applySharpen:result value:sharpen];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (hue){
        GPUImageFilter *filter = [self applyHue:result value:hue];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (red){
        GPUImageFilter *filter = [self applyRed:result value:red];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (green){
        GPUImageFilter *filter = [self applyGreen:result value:green];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }
    if (blue){
        GPUImageFilter *filter = [self applyBlue:result value:blue];
        [filters addObject:filter];
        [tmp addTarget:filter];
        tmp = filter;
    }

    GPUImagePicture *stillImageSource = [[GPUImagePicture alloc] initWithImage:image];
    GPUImageFilterGroup *group = [GPUImageFilterGroup new];
    [group setInitialFilters:[NSArray arrayWithObject:filters.firstObject]];
    [group setTerminalFilter:filters.lastObject];

    [stillImageSource addTarget:group];
    [group useNextFrameForImageCapture];
    [stillImageSource processImage];

    result = [group imageFromCurrentFramebuffer];

    return result;
}

- (GPUImageFilter *)applyBrightness:(UIImage *)image brightness:(NSNumber*)brightness {
    GPUImageBrightnessFilter *filter = [GPUImageBrightnessFilter new];
    [filter setBrightness:[brightness floatValue]];
    return filter;

}
- (GPUImageFilter *)applySaturation:(UIImage *)image saturation:(NSNumber*)saturation {
    GPUImageSaturationFilter *filter = [GPUImageSaturationFilter new];
    [filter setSaturation:[saturation floatValue]];
    return filter;
}
- (GPUImageFilter *)applyContrast:(UIImage *)image contrast:(NSNumber*)contrast {
    GPUImageContrastFilter *filter = [GPUImageContrastFilter new];
    [filter setContrast:[contrast floatValue]];
    return filter;
}

- (GPUImageFilter *)applyExposure:(UIImage *)image value:(NSNumber*)value {
    GPUImageExposureFilter *filter = [GPUImageExposureFilter new];
    [filter setExposure:[value floatValue]];
    return filter;
}

- (GPUImageFilter *)applyGamma:(UIImage *)image value:(NSNumber*)value {
    GPUImageGammaFilter *filter = [GPUImageGammaFilter new];
    [filter setGamma:[value floatValue]];
    return filter;
}

- (GPUImageFilter *)applySharpen:(UIImage *)image value:(NSNumber*)value {
    GPUImageSharpenFilter *filter = [GPUImageSharpenFilter new];
    [filter setSharpness:[value floatValue]];
    return filter;
}

- (GPUImageFilter *)applyHue:(UIImage *)image value:(NSNumber*)value {
    GPUImageHueFilter *filter = [GPUImageHueFilter new];
    [filter setHue:[value floatValue]];
    return filter;
}

- (GPUImageFilter*)applyRed:(UIImage*)image value:(NSNumber *)value {
    GPUImageRGBFilter *filter = [GPUImageRGBFilter new];
    [filter setRed:[value floatValue]];
    return filter;
}
- (GPUImageFilter*)applyGreen:(UIImage*)image value:(NSNumber *)value {
    GPUImageRGBFilter *filter = [GPUImageRGBFilter new];
    [filter setGreen:[value floatValue]];
    return filter;
}
- (GPUImageFilter*)applyBlue:(UIImage*)image value:(NSNumber *)value {
    GPUImageRGBFilter *filter = [GPUImageRGBFilter new];
    [filter setBlue:[value floatValue]];
    return filter;
}

@end
