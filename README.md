# Anyplace Android Gradle library
This library is used to create the [Demo Client](./demo/)

# MVVM Arhictecture:
<img width="300"
    src="https://developer.android.com/topic/libraries/architecture/images/final-architecture.png" />

### Logcat
See [LOGCAT.md](./LOGCAT.md)

--- 

# Structure:

## data

#### data/datastore:
Application preferences using AndroidX/Jetpack `DataStore`.

# Libraries

#### Google Sign in: [Docs](https://developers.google.com/identity/sign-in/android/start-integrating?authuser=2)

In debug mode execute:
```bash
gradle signingReport
```

then copy SHA-1 hash and paste into the developer console.

---

# SMAS Queries
- Queries located at: cy/ac/ucy/cs/anyplace/lib/android/data/smas/db/smasQueries.kt
- Views located at: cy/ac/ucy/cs/anyplace/lib/android/data/smas/db/Views.kt