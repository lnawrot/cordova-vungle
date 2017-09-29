#import <Cordova/CDV.h>
#import <VungleSDK/VungleSDK.h>

@interface VunglePlugin : CDVPlugin<VungleSDKDelegate>

@property VungleSDK *sdk;
@property NSString *appId;
@property NSString *placement;

- (void) setup:(CDVInvokedUrlCommand*)command;
- (void) isReady:(CDVInvokedUrlCommand*)command;
- (void) loadVideoAd:(CDVInvokedUrlCommand*)command;
- (void) showVideoAd:(CDVInvokedUrlCommand*)command;

@end

