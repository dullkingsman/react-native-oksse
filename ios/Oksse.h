
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNOksseSpec.h"

@interface Oksse : NSObject <NativeOksseSpec>
#else
#import <React/RCTBridgeModule.h>

@interface Oksse : NSObject <RCTBridgeModule>
#endif

@end
