//
//  ImageFilter.h
//  HelloWorld
//
//  Created by dat tran on 3/3/17.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>

@interface ImageFilter : CDVPlugin

- (void)applyChromeEffect:(CDVInvokedUrlCommand*)command;

@end
