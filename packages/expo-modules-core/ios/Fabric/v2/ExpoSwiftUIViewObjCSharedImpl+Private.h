// Copyright 2026-present 650 Industries. All rights reserved.

// The `RCTComponentViewProtocol` implementation that `ExpoSwiftUIViewObjC` (NSObject) and `ExpoSwiftUIViewObjCDev`
// (UIView) share. `ExpoSwiftUIViewObjC.mm` includes it inside each `@implementation` block, which declares these ivars:
//   facebook::react::SharedViewProps _props;
//   facebook::react::SharedViewEventEmitter _eventEmitter;
//   std::mutex _eventEmitterMutex; // Guards `_eventEmitter`, because events can be dispatched from any thread.

#pragma mark - RCTComponentViewProtocol

+ (facebook::react::ComponentDescriptorProvider)componentDescriptorProvider
{
  return expo::views::componentDescriptorProvider(self);
}

+ (std::vector<facebook::react::ComponentDescriptorProvider>)supplementalComponentDescriptorProviders
{
  return {};
}

+ (BOOL)shouldBeRecycled
{
  // Like the views of `ExpoNativeViewObjC`, the views aren't recycled.
  return NO;
}

- (void)mountChildComponentView:(nonnull UIView *)childComponentView index:(NSInteger)index
{
  // Implemented in `SwiftUINativeView.swift`
}

- (void)unmountChildComponentView:(nonnull UIView *)childComponentView index:(NSInteger)index
{
  // Implemented in `SwiftUINativeView.swift`
}

- (void)updateProps:(const facebook::react::Props::Shared &)props oldProps:(const facebook::react::Props::Shared &)oldProps
{
  _props = std::static_pointer_cast<const facebook::react::ViewProps>(props);
}

- (void)updateEventEmitter:(const facebook::react::EventEmitter::Shared &)eventEmitter
{
  std::lock_guard<std::mutex> lock(_eventEmitterMutex);
  _eventEmitter = std::static_pointer_cast<const facebook::react::ViewEventEmitter>(eventEmitter);
}

- (void)updateState:(const facebook::react::State::Shared &)state oldState:(const facebook::react::State::Shared &)oldState
{
  // `NewViewShadowNode` has no state.
}

- (void)updateLayoutMetrics:(const facebook::react::LayoutMetrics &)layoutMetrics
           oldLayoutMetrics:(const facebook::react::LayoutMetrics &)oldLayoutMetrics
{
  // SwiftUI lays out the view, not Yoga.
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
  // The views have no commands.
}

- (void)finalizeUpdates:(RNComponentViewUpdateMask)updateMask
{
  if (updateMask & RNComponentViewUpdateMaskProps) {
    const auto &props = static_cast<const expo::views::NewViewProps &>(*_props);
    [self updateDecodedProps:props.decodedProps.get()];
  }
}

- (void)prepareForRecycle
{
  std::lock_guard<std::mutex> lock(_eventEmitterMutex);
  _eventEmitter.reset();
}

- (void)invalidate
{
  [self prepareForRecycle];
}

- (facebook::react::Props::Shared)props
{
  return _props;
}

- (BOOL)isJSResponder
{
  return NO;
}

- (void)setIsJSResponder:(BOOL)isJSResponder
{
  // SwiftUI handles the touches, not the JS responder system.
}

- (void)setPropKeysManagedByAnimated_DO_NOT_USE_THIS_IS_BROKEN:(nullable NSSet<NSString *> *)propKeys
{
  // Animated can't set the props of these views.
}

- (nullable NSSet<NSString *> *)propKeysManagedByAnimated_DO_NOT_USE_THIS_IS_BROKEN
{
  return nil;
}

- (void)updateClippedSubviewsWithClipRect:(CGRect)clipRect relativeToView:(UIView *_Nonnull)clipView
{
  // SwiftUI clips the views, not React Native.
}

#pragma mark - Methods to override in Swift

- (void)updateDecodedProps:(nullable const void *)decodedProps
{
  // Implemented in `SwiftUINativeView.swift`
}

#pragma mark - EXViewEventEmitting

- (void)dispatchEvent:(NSString *)name encodePayload:(nullable EXViewEventPayloadEncoder)encodePayload
{
  facebook::react::SharedViewEventEmitter eventEmitter;
  {
    std::lock_guard<std::mutex> lock(_eventEmitterMutex);
    eventEmitter = _eventEmitter;
  }
  expo::views::dispatchViewEvent(eventEmitter, name, encodePayload);
}
