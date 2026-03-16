# StackUsers – Mathew Thomas

An Android application that connects to the [StackExchange Users API](https://api.stackexchange.com/docs) to search for and display StackOverflow user profiles.

Built as a technical test for CandySpace, demonstrating production-grade Android architecture and engineering practices.

---

## Building & Running

**Requirements:** Android Studio Hedgehog (2023.1.1) or later, JDK 17 or above.

### 1. Make the Gradle wrapper executable (first build only)

The wrapper script may need execute permission before it can be run:

```bash
chmod +x gradlew
```

This is a one-time step and will not be needed again on subsequent runs.

### 2. Build, lint and test

```bash
./gradlew lint test assembleDebug
```

Runs the three tasks specified in the assessment brief:

- `lint` — static analysis
- `test` — all unit tests
- `assembleDebug` — produces the debug APK

The generated APK will be at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Build environment

| Tool | Version |
|---|---|
| Gradle | 9.40 |
| Android Gradle Plugin | 9.1.0 |
| Kotlin | 2.2.10 |
| compileSdk | 36 |
| minSdk | 21 (Android 5.0+) |
| targetSdk | 36 |
| Java | 17 |

---

## API Key

During development, the StackExchange API's anonymous quota of 300 requests/day was hit repeatedly while building and testing the app. To avoid this impacting the reviewer's experience, a registered StackExchange API key has been included in the app, which raises the quota to 10,000 requests/day.

**This is intentional for the purposes of this assessment only.**

In a production application, hardcoding an API key into source code would never be acceptable. The correct approach on Android is:

1. Store the key in `local.properties` at the project root — this file is excluded from version control by default via `.gitignore` and never committed to the repository
2. Read the key at build time in `app/build.gradle` and expose it via `BuildConfig`:

```groovy
defaultConfig {
    def localProps = new Properties()
    rootProject.file('local.properties').withInputStream { localProps.load(it) }
    buildConfigField "String", "STACK_API_KEY", "\"${localProps['stackexchange.api.key']}\""
}
```

3. Reference it in code via `BuildConfig.STACK_API_KEY` — never as a string literal

For CI/CD pipelines the key would be stored as an encrypted environment variable in the pipeline configuration (e.g. Bitrise Secrets, GitHub Actions Secrets) and injected at build time, never appearing in the codebase.

---

## Architecture

The project follows **Clean Architecture** with three distinct layers, enforcing a strict dependency rule: outer layers depend on inner layers, never the reverse.

```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│  Compose Screens · ViewModels · UiState │
├─────────────────────────────────────────┤
│            Domain Layer                 │
│  Use Cases · Domain Models · Repository │
│             Interface                   │
├─────────────────────────────────────────┤
│             Data Layer                  │
│  Repository Impl · Retrofit · DTOs      │
└─────────────────────────────────────────┘
```

### Key design decisions

**UiState sealed class** — each screen's ViewModel exposes a single `StateFlow<UiState<T>>` covering `Idle`, `Loading`, `Success`, `Empty`, and `Error`. This makes all possible states explicit and exhaustive, eliminating implicit null checks or scattered boolean flags across the UI.

**Use Cases as the source of business logic** — sorting, input validation, and result transformation live in use cases, not ViewModels. This keeps ViewModels thin (orchestration only) and keeps logic independently testable without any Android dependencies.

**Repository interface in domain, implementation in data** — the domain layer defines the contract; the data layer fulfils it. ViewModels and use cases never import anything from the data layer. This boundary makes it straightforward to swap the data source (e.g. add Room caching) without touching business logic.

**Alphabetical sort guaranteed client-side** — results are sorted in `SearchUsersUseCase` after the network response, not relied upon from the API. This ensures consistent ordering regardless of API behaviour.

**Debounced search** — the search query is a `StateFlow` that is debounced (400ms) before triggering a network request, avoiding unnecessary API calls on every keystroke. Manual submission via the search button bypasses the debounce for immediate feedback.

**Moshi considered; Gson chosen** — Gson is explicitly listed in the job spec stack. Moshi would be preferred in a greenfield project for its Kotlin-native null safety, and would be an easy migration given the clean DTO layer.

---

## Tech Stack

| Concern | Library | Rationale |
|---|---|---|
| UI | Jetpack Compose + Material3 | Modern declarative UI, min API 21 compatible |
| Navigation | Navigation Compose | Type-safe nav graph, single Activity |
| DI | Hilt | Explicitly in spec; reduces DI boilerplate vs manual Dagger |
| Networking | Retrofit + OkHttp | Industry standard; in spec |
| Serialisation | Gson | In spec |
| Image loading | Coil | Kotlin/coroutine-native; lighter than Glide for this use case |
| Async | Coroutines + StateFlow | In spec; idiomatic Kotlin |
| Unit testing | JUnit4 + Mockito-Kotlin | In spec |
| Flow testing | Turbine | Industry standard for testing StateFlow/SharedFlow |

---

## Testing

Unit tests cover all business logic and ViewModel state management:

| Test class | What it verifies |
|---|---|
| `SearchUsersUseCaseTest` | Blank query rejection, alphabetical sorting, error propagation |
| `GetUserDetailUseCaseTest` | Invalid ID rejection, delegation to repository, error propagation |
| `UserListViewModelTest` | All UiState transitions: Idle → Loading → Success / Empty / Error, search clear |
| `UserRepositoryImplTest` | DTO → domain model mapping, null badge handling, API exception wrapping |
| `BackoffInterceptorTest` | 429 retry behaviour, max retry exhaustion, exponential backoff timing, Retry-After header parsing, backoff field extraction from response body, non-429 errors not retried |

Run tests in isolation:

```bash
./gradlew test
```

---

## What I'd Add With More Time

- **Offline caching with Room** — cache search results and user detail locally, serving stale data with a freshness indicator while a refresh is in progress.
- **Pagination** — the StackExchange API supports `page` and `pagesize`; a `PagingSource` backed by Paging 3 would handle infinite scroll cleanly.
- **Move API key to local.properties** — as described above, removing the hardcoded key and replacing it with the `BuildConfig` pattern would be the first thing done before any production release.
- **UI tests with Compose testing APIs** — `composeTestRule` for asserting screen content in key flows (search result list, detail screen rendering).
- **Migrate kapt to KSP** — KSP is significantly faster than kapt and is the direction Hilt is heading; currently blocked on Hilt's stable KSP support.
- **Network connectivity awareness** — detect offline state and surface a dedicated no-connection UI rather than a generic error.
- **Accessibility audit** — content descriptions are in place; a full TalkBack walkthrough would verify focus order and announcement quality.
- **Image Placeholders** – Currently if the image fails to load or is slow, there's nothing shown. Given more time I would show a generic person icon or a coloured circle with the user's initial as a fallback.
- **UI Improvements** – Given more time I would improve the UI / work to designs, currently the UI is basic and could use some polish.
---

## API Notes

The StackExchange API returns gzip-compressed responses. OkHttp handles decompression automatically. All requests target `site=stackoverflow`.

Registered key quota: **10,000 requests/day**. Anonymous quota (without a key) is 300 requests/day.
