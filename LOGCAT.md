# TODO: update logging:

## Logging conventions:
- Use `TG` for tag name, and `MT` for a method's name
- see `TG` examples in different classes to figure out the value to assign
  - if there is a `companion object`, place `MT` in there
  - make it private (use on in relevant class)
- To asign value to `MT`use `::methodName.name` to get the name
- why?
  - because it is resolved at compile time (does not take runtime)
  - refactoring the method name changes this name automatically
- what about `overloaded` methods?
  - doesn't work unfortunately. just make a string of that method name
- you can use macros to easily create the `MT` variable
  - TODO:TUT: youtube video



# Logcat (LOG.kt)
Study the relevant sources in:
- [LOG.kt](lib-android/src/main/java/cy/ac/ucy/cs/anyplace/lib/android/utils/LOG.kt)
- [ActivityEXT.kt](lib-android/src/main/java/cy/ac/ucy/cs/anyplace/lib/android/extensions/ActivityEXT.kt)

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