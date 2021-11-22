# Logcat (LOG.kt)
Study the relevant sources in:
- [LOG.kt](lib-android/src/main/java/cy/ac/ucy/cs/anyplace/lib/android/utils/LOG.kt)
- [ActivityExtensions.kt](lib-android/src/main/java/cy/ac/ucy/cs/anyplace/lib/android/extensions/ActivityExtensions.kt)

It uses Java StackFrames to exclude classes/method names of:
- helper classes, like `LOG` or methods in ActivityExtensions
- lambdas, anonymous classes, or internal classes (like `kotlinx.*`)

## `LOG` methods
They are wrappers on top of Android `Log` methods:
- Error: LOG.E
- Warning: LOG.W
- Info: LOG.I
- Debug: LOG.D (variants: D1, D2, D3, D4, D5)
- Verbose: LOG.V (variants: V1, V2, V3, V4, V5)

## Setting debug level:
It controls the Debug and Verbose variant methods.
The log level can be set using: `DBG.LEVEL` (in [LOG.kt](lib-android/src/main/java/cy/ac/ucy/cs/anyplace/lib/android/utils/LOG.kt)).
Any LOG.Di or LOG.Vi, where i>= to the `DBG.LEVEL` will be printed.

##### Example:
DBG.LEVEL=2
LOG.D2("This will be printed")
LOG.D3("But this will be not!")

## Using `Any.TAG`:
It is the `LOG.TAG` with the current class name.

##### Example:
```kotlin
class Foo {
  fun bar() {
  LOG.D(TAG, "test")
  }
}
```

Prints:
```bash
# tag: message
/anyplace/Foo: Test
```

## Using `Any.TAG_METHOD` (current class+method):
It is the `LOG.TAG` with the current class & method name.

##### Example:
```kotlin
class Foo() {
  fun bar() {
  LOG.D(TAG_METHOD, "test")
  }
}
```

Prints:
```bash
# tag: message
/anyplace/Foo/bar: Test
```

## Using `Any.METHOD` (current class+method):
It is the current method name.

##### Example:
```kotlin
class Foo {
  fun bar() {
  LOG.D(TAG, "$METHOD: test")
  }
}
```

Prints:
```bash
# tag: message
/anyplace/Foo: bar: Test
```

## Using `LOG.D("message")`
It uses `LOG.TAG` as a default tag

##### Example:
```kotlin
class Foo {
  fun bar() {
  LOG.D("test")
  }
}
```

Prints:
```bash
# tag: message
/anyplace: bar: Test
```
## Quickly logging that current method has run
Use any of the LOG methods (V, D, I, W, E) without any arguments.
It uses `LOG.TAG` as a default tag

##### Example:
```kotlin
class Foo {
  fun bar() {
  LOG.D()
  }
}
```

Prints:
```bash
# tag: message
/anyplace/bar:
```