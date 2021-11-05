# Structure

TODO:

#### Github Repositories:
<details>
 <summary>Github Repositories</summary>

Anyplace is divided into 3 repositories, one `main`, that includes two library `submodules:
```
<Main Anyplace Repo>
- clients
- clients/core: Core Library <git submodule>
- clients/android-new/lib-android: Android Library <git submodule>
```
</details>

#### Android Library Code (anyplace-android):

<details>
 <summary>TODO library structure</summary>

</details>


# Code Architecture

<details>
 <summary>TODO architecture</summary>

</details>

# Classes and Conventions
<details>
 <summary>Classes and Conventions</summary>

### Models and ModelHelpers
`Models` are part of the `lib-core`, and they are [data classes](https://kotlinlang.org/docs/data-classes.html).
They must hold only data and no logic (code) whatsoever.
This is important as they are serialized/deserialized often:
- from/to Network (retrofit2)
- from/to DB (Room)

Path:
`/clients/core/lib/src/main/java/cy/ac/ucy/cs/anyplace/lib/models/`

[ModelHelpers](src/main/java/cy/ac/ucy/cs/anyplace/lib/android/data/modelhelpers) accept a `Model` as a parameter and can provide any needed functionality.
The convention is to use `modelH` for the `ModelHelper` variable.

Examples:
- [SpaceHelper](src/main/java/cy/ac/ucy/cs/anyplace/lib/android/data/modelhelpers/SpaceHelper.kt)

</details>