#import "VunglePlugin.h"

@implementation VunglePlugin

@synthesize appId;
@synthesize placement;

- (void) pluginInitialize {
  [super pluginInitialize];
}

- (void) setup: (CDVInvokedUrlCommand*)command {
  NSLog(@"setup");

  CDVPluginResult *pluginResult;
  NSArray* args = command.arguments;

  NSUInteger argc = [args count];
  if( argc >= 1 ) {
    NSDictionary* options = [command argumentAtIndex:0 withDefault:[NSNull null]];

    [self _setup:options];
  }
}

- (void) _setup:(NSDictionary*)options {
  if ((NSNull *)options == [NSNull null]) return;

  NSString* str = nil;

  str = [options objectForKey:@"appId"];
  if (str && [str length] > 0) {
    self.appId = str;
  }

  str = [options objectForKey:@"placement"];
  if (str && [str length] > 0) {
    self.placement = str;
  }

  NSArray* placementIDsArray = @[self.placement];

  self.sdk = [VungleSDK sharedSDK];
  [self.sdk setDelegate:self];
  [self.sdk setLoggingEnabled:YES];
  NSError *error = nil;

  if(![self.sdk startWithAppId:self.appId placements:placementIDsArray error:&error]) {
      NSLog(@"Error while starting VungleSDK %@", [error localizedDescription]);
      NSString* jsonData = [NSString stringWithFormat:@"{'error':'%@'}", error];
      [self fireEvent:@"" event:@"vungle.error" withData:jsonData];
  } else {
    NSString* jsonData = @"{'ready':'wow'}";
    [self fireEvent:@"" event:@"vungle.error" withData:jsonData];
  }
}

- (void) isReady:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    NSString *callbackId = command.callbackId;

    BOOL isReady = [self.sdk isAdCachedForPlacementID:self.placement];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isReady];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void) loadVideoAd: (CDVInvokedUrlCommand*)command {
    NSString *callbackId = command.callbackId;

    // [self _loadVideoAd];
    NSError *error;
    [self.sdk loadPlacementWithID:self.placement error:&error];

    if (error) {
        NSLog(@"Error encountered playing ad: %@", error);
        NSString* jsonData = [NSString stringWithFormat:@"{'error':'%@'}", error];
        [self fireEvent:@"" event:@"vungle.error" withData:jsonData];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:error];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    } else {
      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }
}

- (void) _loadVideoAd {
  NSError *error;
  [self.sdk loadPlacementWithID:self.placement error:&error];

  if (error) {
      NSLog(@"Error encountered playing ad: %@", error);
      NSString* jsonData = [NSString stringWithFormat:@"{'error':'%@'}", error];
      [self fireEvent:@"" event:@"vungle.error" withData:jsonData];
  }
}

- (void) showVideoAd: (CDVInvokedUrlCommand*)command {
    NSString *callbackId = command.callbackId;

    // [self _showVideoAd];
    NSError *error;
    [self.sdk playAd:self.viewController options:nil placementID:self.placement error:&error];
    if (error) {
        NSLog(@"Error encountered playing ad: %@", error);
        NSString* jsonData = [NSString stringWithFormat:@"{'error':'%@'}", error];
        [self fireEvent:@"" event:@"vungle.error" withData:jsonData];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonData];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    } else {
      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"tralalal"];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }
}

- (void) _showVideoAd {
  NSError *error;
  [self.sdk playAd:self.viewController options:nil placementID:self.placement error:&error];

  if (error) {
      NSLog(@"Error encountered playing ad: %@", error);
      NSString* jsonData = [NSString stringWithFormat:@"{'error':'%@'}", error];
      [self fireEvent:@"" event:@"vungle.error" withData:jsonData];
  }
}

- (void) fireEvent:(NSString *)obj event:(NSString *)eventName withData:(NSString *)jsonStr {
  NSString* js;
  if(obj && [obj isEqualToString:@"window"]) {
    js = [NSString stringWithFormat:@"var evt=document.createEvent(\"UIEvents\");evt.initUIEvent(\"%@\",true,false,window,0);window.dispatchEvent(evt);", eventName];
  } else if(jsonStr && [jsonStr length]>0) {
    js = [NSString stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@',%@);", eventName, jsonStr];
  } else {
    js = [NSString stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@');", eventName];
  }
  [self.commandDelegate evalJs:js];
}


- (void) vungleAdPlayabilityUpdate:(BOOL)isAdPlayable placementID:(NSString *)placementID {
    NSLog(@"-->> Delegate Callback: vungleAdPlayabilityUpdate");
    if (isAdPlayable) {
      [self fireEvent:@"" event:@"vungle.ready" withData:nil];
    }
}

- (void) vungleWillShowAdForPlacementID:(nullable NSString *)placementID {
    NSLog(@"-->> Delegate Callback: vungleSDKwillShowAd");
    [self fireEvent:@"" event:@"vungle.start" withData:nil];
}

- (void) vungleWillCloseAdWithViewInfo:(VungleViewInfo *)info placementID:(NSString *)placementID {
    NSLog(@"-->> Delegate Callback: vungleWillCloseAdWithViewInfo");
    NSString* jsonData = [NSString stringWithFormat:@"{'result':'%@','clicked':'%@'}", info.completedView ? "true" : "false", info.didDownload ? "true" : "false"];
    [self fireEvent:@"" event:@"vungle.finish" withData:jsonData];
}

@end

