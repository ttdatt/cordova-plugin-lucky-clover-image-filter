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
    if (!path.length && !filterType.length) {
        if (![currentImagePath isEqualToString:path]) {
            currentImagePath = path;
            currentEditingImage = [UIImage imageWithContentsOfFile:path];
        }
    }
}

- (void)applyChromeEffect:(CDVInvokedUrlCommand*)command {
    [self validateInput:command];
    
    NSString *filterType = [command argumentAtIndex:1 withDefault:nil];
    NSData *data = [self filterImage:currentEditingImage filter:filterType];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (NSData *)filterImage:(UIImage *)image filter:(NSString *)filterType {
    @autoreleasepool {
        CIImage *ciImage = [[CIImage alloc] initWithImage:image];
        ciImage = [ciImage imageByApplyingFilter:filterType withInputParameters:nil];
        CGImageRef ref = [context createCGImage:ciImage fromRect:ciImage.extent];
        UIImage *img = [[UIImage alloc] initWithCGImage:ref];
        NSData *data = UIImageJPEGRepresentation(img, 0.6);
        return data;
    }
}

@end
