// Copyright 2026-present 650 Industries. All rights reserved.

#import <ExpoModulesCore/ExpoSwiftUIViewObjC.h>
#import <ExpoModulesCore/NewComponentRegistry.h>
#import <ExpoModulesCore/NewViewShadowNode.h>
#import <ExpoModulesCore/ViewEventDispatch.h>

#import <React/RCTComponentViewProtocol.h>
#import <React/RCTLog.h>

#include <mutex>

@implementation ExpoSwiftUIViewObjC {
  facebook::react::SharedViewProps _props;
  facebook::react::SharedViewEventEmitter _eventEmitter;
  std::mutex _eventEmitterMutex;
}

- (instancetype)init
{
  if (self = [super init]) {
    // `finalizeUpdates:` reads the props as `NewViewProps`, so the default props must be of that type too.
    _props = expo::views::NewViewShadowNode::defaultSharedProps();
  }
  return self;
}

#if TARGET_OS_OSX
- (void)setReactTag:(nullable NSNumber *)reactTag
{
  self.tag = reactTag.integerValue;
}

- (nullable NSNumber *)reactTag
{
  return @(self.tag);
}
#endif // TARGET_OS_OSX

// Detects when this NSObject is mounted in a UIKit view, like `SwiftUIVirtualViewObjC` does: UIKit calls a private
// selector early in `_isAncestorOfFirstResponder`, so the insertion throws before it goes on. The selector name is built
// from fragments to keep the private API name out of the binary.
- (id)forwardingTargetForSelector:(SEL)aSelector
{
  static SEL hierarchyInsertionSelector;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    NSString *selectorName = [@[ @"_", @"isAncestor", @"OfFirst", @"Responder" ] componentsJoinedByString:@""];
    hierarchyInsertionSelector = NSSelectorFromString(selectorName);
  });
  if (aSelector == hierarchyInsertionSelector) {
    @throw [NSException exceptionWithName:NSInternalInconsistencyException
                                   reason:[NSString stringWithFormat:
                                                      @"A SwiftUI view \"%@\" (tag: %ld) is being mounted inside a standard UIView. "
                                                      @"Wrap your component with `<Host>` from '@expo/ui/swift-ui'.",
                                                      NSStringFromClass([self class]), (long)self.tag]
                                 userInfo:nil];
  }
  return [super forwardingTargetForSelector:aSelector];
}

#pragma mark - UIView(UIViewHierarchy)

// Fabric calls these on every component view. They do nothing, because the view isn't in the UIKit hierarchy.

- (nullable UIView *)superview
{
  return nil;
}

- (NSArray<UIView *> *)subviews
{
  return @[];
}

- (nullable UIWindow *)window
{
  return nil;
}

- (void)removeFromSuperview
{
}

- (void)insertSubview:(UIView *)view atIndex:(NSInteger)index
{
}

- (void)exchangeSubviewAtIndex:(NSInteger)index1 withSubviewAtIndex:(NSInteger)index2
{
}

- (void)addSubview:(UIView *)view
{
}

- (void)insertSubview:(UIView *)view belowSubview:(UIView *)siblingSubview
{
}

- (void)insertSubview:(UIView *)view aboveSubview:(UIView *)siblingSubview
{
}

- (void)bringSubviewToFront:(UIView *)view
{
}

- (void)sendSubviewToBack:(UIView *)view
{
}

- (void)didAddSubview:(UIView *)subview
{
}

- (void)willRemoveSubview:(UIView *)subview
{
}

- (void)willMoveToSuperview:(nullable UIView *)newSuperview
{
}

- (void)didMoveToSuperview
{
}

- (void)willMoveToWindow:(nullable UIWindow *)newWindow
{
}

- (void)didMoveToWindow
{
}

- (BOOL)isDescendantOfView:(UIView *)view
{
  return NO;
}

- (nullable UIView *)viewWithTag:(NSInteger)tag
{
  return nil;
}

- (void)setNeedsLayout
{
}

- (void)layoutIfNeeded
{
}

- (void)layoutSubviews
{
}

#include "ExpoSwiftUIViewObjCSharedImpl+Private.h"

@end

@implementation ExpoSwiftUIViewObjCDev {
  facebook::react::SharedViewProps _props;
  facebook::react::SharedViewEventEmitter _eventEmitter;
  std::mutex _eventEmitterMutex;
}

// `initWithFrame:`, not `init`: `+new` calls `-[UIView init]`, which sends `initWithFrame:` to `self`, so the Swift
// subclass initializes its stored properties first.
- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    _props = expo::views::NewViewShadowNode::defaultSharedProps();
    self.hidden = YES;
#if TARGET_OS_IOS || TARGET_OS_TV
    self.userInteractionEnabled = NO;
#endif
  }
  return self;
}

#if TARGET_OS_IOS || TARGET_OS_TV
- (void)didMoveToSuperview
{
  [super didMoveToSuperview];
  if (self.superview != nil) {
    RCTLogError(
      @"A SwiftUI view \"%@\" (tag: %ld) is being mounted inside a standard UIView. "
      @"Double check that in JSX you have wrapped your component with `<Host>` from '@expo/ui/swift-ui'.",
      NSStringFromClass([self class]), (long)self.tag);
  }
}
#else
- (void)viewDidMoveToSuperview
{
  [super viewDidMoveToSuperview];
  if (self.superview != nil) {
    RCTLogError(
      @"A SwiftUI view \"%@\" (tag: %ld) is being mounted inside a standard NSView. "
      @"Double check that in JSX you have wrapped your component with `<Host>` from '@expo/ui/swift-ui'.",
      NSStringFromClass([self class]), (long)self.tag);
  }
}
#endif // TARGET_OS_IOS || TARGET_OS_TV

#if TARGET_OS_OSX
- (NSInteger)tag
{
  return self.reactTag.integerValue;
}
#endif // TARGET_OS_OSX

#include "ExpoSwiftUIViewObjCSharedImpl+Private.h"

@end
