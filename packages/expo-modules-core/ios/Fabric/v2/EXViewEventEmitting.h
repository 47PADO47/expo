// Copyright 2026-present 650 Industries. All rights reserved.

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 Takes the payload of an event: `value` points to a `facebook::jsi::Value`, which the callee copies.
 */
typedef void (^EXViewEventPayloadWriter)(const void *value) NS_SWIFT_NAME(ViewEventPayloadWriter);

/**
 Encodes the payload of an event on the JavaScript thread. It gets the `facebook::jsi::Runtime` and passes the payload
 to `write` before it returns. When it doesn't call `write`, the payload is an empty object.
 */
typedef void (^EXViewEventPayloadEncoder)(const void *runtime, NS_NOESCAPE EXViewEventPayloadWriter write)
  NS_SWIFT_NAME(ViewEventPayloadEncoder);

/**
 A view of Expo Modules v2 that sends events to JavaScript through its Fabric event emitter. `ViewEventDispatcher`
 calls it.
 */
NS_SWIFT_NAME(ViewEventEmitting)
@protocol EXViewEventEmitting <NSObject>

/**
 Dispatches the event with the given name, for example `onTap`, to JavaScript. Without `encodePayload`, the payload is
 an empty object. It can be called from any thread. Before Fabric gives the view its event emitter, it does nothing.
 */
- (void)dispatchEvent:(NSString *)name
        encodePayload:(nullable EXViewEventPayloadEncoder)encodePayload
  NS_SWIFT_NAME(dispatchEvent(_:encodePayload:)) NS_SWIFT_NONISOLATED;

@end

NS_ASSUME_NONNULL_END
