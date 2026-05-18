You are a senior Android engineer. Generate a complete, production-ready Android project following strict architectural and coding guidelines.

## 🏗️ Architecture
Use **NVI (Model-View-Intent)** architecture with clear separation of:
- **Intent** — user actions (sealed class per feature)
- **State** — immutable UI state (data class per feature)
- **ViewModel** — state reducer, consumes UseCases, emits StateFlow

## 🧱 Tech Stack
- UI: Jetpack Compose
- Dependency Injection: Hilt
- Database: Room
- Networking: Retrofit + OkHttp
- Logging: Timber

---

## 📦 Project Structure

Feature-based packaging. Each feature is an independent vertical slice:
```
feature_name/
├── presentation/   ← Composables, ViewModel, UiState, Intent
├── domain/         ← UseCases, Repository interfaces
└── data/           ← Repository impls, DataSources, DTOs, Mappers
```

Shared cross-feature code lives in `shared/`:
```
shared/
├── ui/
│   ├── theme/
│   │   ├── AppTheme.kt
│   │   ├── LocalAppColorScheme.kt
│   │   └── LocalAppDimens.kt
│   ├── tokens/
│   │   ├── brand/
│   │   │   ├── BrandTokens.kt            ← interface
│   │   │   ├── DefaultBrandTokens.kt
│   │   │   └── OceanBrandTokens.kt       ← second palette example
│   │   ├── AppColorScheme.kt
│   │   ├── AppDimens.kt
│   │   ├── AppShapes.kt
│   │   ├── AppTypography.kt
│   │   └── AppIcons.kt
│   ├── components/                       ← App* composables
│   └── utils/
│       └── UiText.kt
├── domain/
├── data/
├── utils/
└── navigation/
```

**Shared rules:**
- Reusable logic MUST go into `shared/` — no duplication across features
- `shared/` must NOT depend on any feature
- Features CAN depend on `shared/`

---

## ⚙️ Coding Rules (STRICT)

### Layer rules
- **Presentation:** Composables, ViewModel, UiState, Intent — no business logic
- **Domain:** UseCases and Repository interfaces only — NO Android imports
- **Data:** Repository implementations, DataSources, DTOs, Mappers — no UI

### UseCase rules
- One UseCase per repository function — named `Verb + Entity + UseCase` (e.g. `GetUserUseCase`)
- ViewModels ONLY call UseCases — never repositories directly
- UseCases MUST be `suspend` or return `Flow`
- Both UseCase and Repository MUST have interface + implementation in separate files

### File separation (strict)
Every concept lives in its own file: UiState, ViewModel, Intent, UseCase, Repository, Mapper

### Mappers
- DTO → Domain: one mapper file
- Domain → UI model: one mapper file
- No mapping logic in repositories or composables

### Dependency Injection
- `@Binds` for interface → implementation binding
- `@Provides` only when a constructor cannot be annotated
- Inject `CoroutineDispatcher` via Hilt — no hardcoded `Dispatchers.*` in UseCases
- No manual instantiation anywhere

### Code quality
- SOLID principles throughout
- No raw exceptions above the data layer — always map to `AppError`
- Use Timber for all logging — `android.util.Log` is forbidden
- `runBlocking` is forbidden outside tests

---

## 📋 UiState Contract (MANDATORY)

Every feature `UiState` MUST follow this exact structure:

```kotlin
// feature/example/presentation/ExampleUiState.kt
data class ExampleUiState(
    val isLoading: Boolean = false,
    val error: UiText? = null,       // null = no error
    val data: ExampleData? = null,   // null = not yet loaded
)
```

**Field rules:**

`isLoading` — `true` during initial fetch and explicit refresh. `false` as soon as data or error arrives. NEVER nullable.

`error` — `UiText` on failure, `null` on success or user dismiss. `data` and `error` may coexist — composable shows stale data + error banner simultaneously.

`data` — `null` until first successful load. Retained on refresh failure (stale data pattern).

**Standard ViewModel transitions:**
```kotlin
// Start load
_state.update { it.copy(isLoading = true, error = null) }

_state.update { current ->
    when (val result = useCase.execute()) {
        is Result.Success -> current.copy(isLoading = false, data = result.data, error = null)
        is Result.Error   -> current.copy(isLoading = false, error = result.error.toUiText())
        // data intentionally retained on error — preserve stale content
    }
}
```

**Forbidden UiState patterns:**
```kotlin
// ❌ Raw String for error
data class BadState(val errorMessage: String? = null)

// ❌ Nullable Boolean for loading
data class BadState(val isLoading: Boolean? = null)

// ❌ Sealed loading/success/error — prevents stale data + error coexistence
sealed class BadState { object Loading; data class Success(...); data class Error(...) }
```

---

## 🔤 UiText (MANDATORY)

ViewModels NEVER call `stringResource()` or `context.getString()`. They emit `UiText`. Composables resolve it.

```kotlin
// shared/ui/utils/UiText.kt
sealed class UiText {
    data class StringResource(
        @StringRes val id: Int,
        val args: Array<Any> = emptyArray(),
    ) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return id == other.id && args.contentEquals(other.args)
        }
        override fun hashCode() = 31 * id + args.contentHashCode()
    }

    data class DynamicString(val value: String) : UiText()
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.StringResource -> context.getString(id, *args)
    is UiText.DynamicString  -> value
}
```

**Rules:**
- `StringResource` for all static labels, error messages, and status text
- `DynamicString` ONLY for server-sourced or user-inputted text that cannot be known at compile time
- All `UiState.error` fields use `UiText`
- All `UiEvent` snackbar/toast payloads use `UiText`
- Resolve via `LocalContext.current` in composables only

---

## ⚠️ Error Handling (MANDATORY)

```kotlin
// shared/domain/error/AppError.kt
sealed class AppError {
    data object NoInternetConnection : AppError()
    data object RequestTimeout       : AppError()
    data class  HttpError(val code: Int) : AppError()
    data object ServerError          : AppError()   // 5xx
    data object Unauthorized         : AppError()   // 401
    data object SessionExpired       : AppError()
    data object NotFound             : AppError()   // 404 or empty body
    data class  DatabaseError(val cause: Throwable) : AppError()
    data class  ValidationError(val field: String, val message: UiText) : AppError()
    data class  Unknown(val cause: Throwable) : AppError()
}

// shared/ui/utils/AppErrorExt.kt
fun AppError.toUiText(): UiText = when (this) {
    is AppError.NoInternetConnection -> UiText.StringResource(R.string.error_no_internet)
    is AppError.RequestTimeout       -> UiText.StringResource(R.string.error_timeout)
    is AppError.Unauthorized         -> UiText.StringResource(R.string.error_unauthorized)
    is AppError.NotFound             -> UiText.StringResource(R.string.error_not_found)
    is AppError.HttpError            -> UiText.StringResource(R.string.error_http, arrayOf(code))
    else                             -> UiText.StringResource(R.string.error_unknown)
}
```

**Result wrapper:**
```kotlin
// shared/domain/result/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
}
```

**Rules:**
- All UseCases return `Result<T>` or `Flow<Result<T>>` — never raw `T?`
- Data layer catches ALL exceptions and maps them to `AppError` subtypes
- `Unknown` is the fallback — never the default
- `IOException` / `SocketTimeoutException` → `NoInternetConnection` / `RequestTimeout`
- `HttpException` → `HttpError`, `Unauthorized`, `ServerError` by code
- No raw `try/catch` above the data layer

---

## 📡 Networking

### Retrofit
- DTOs in data layer only — mapped to domain models via Mappers
- Use `@Serializable` data classes for DTOs (kotlinx.serialization)

### Network resilience (MANDATORY)

**Connectivity:**
- `NetworkMonitor` interface in `shared/data/` — injected via Hilt
- Repository checks connectivity before every request
- Returns `AppError.NoInternetConnection` immediately if offline

**OkHttp timeouts:**
- `connectTimeout`: 15s
- `readTimeout`: 30s
- `writeTimeout`: 15s

**Retry policy:**
- Retry transient errors only: timeout, 503, IO failures — max 3 attempts with exponential backoff
- Never retry 4xx — client errors are not transient
- Implement in OkHttp Interceptor — NEVER in ViewModel or UseCase

**Response validation:**
- Empty 200 body where one is required → `AppError.NotFound`
- JSON parse failure → `AppError.Unknown`
- Never silently swallow any response failure

---

## 🧭 Navigation (MANDATORY)

**Library:** Navigation Compose with typed routes.

**Structure:**
- Each feature defines its graph as an extension on `NavGraphBuilder`
- All graphs registered in `shared/navigation/`
- Features NEVER reference each other's routes directly

**Routes:**
```kotlin
// Typed route — never raw strings
@Serializable
data class UserDetailRoute(val userId: String)
```

**Navigation events:**
- ViewModel emits navigation as one-shot `SharedFlow<UiEvent>` — NEVER stored in `UiState`
- Composables collect via `LaunchedEffect` and call `NavController`
- Back navigation: ViewModel emits `UiEvent.NavigateUp` — composable calls `navController.navigateUp()`
- Composables NEVER call `popBackStack()` based on their own logic

**Deep links:** Declared at `NavGraph` level. URI patterns as constants in `shared/navigation/`.

---

## 🎨 Design Token System (MANDATORY)

Three strictly separated layers. Violating the boundary between layers is forbidden.

### Layer 1 — Brand Tokens (raw palette)

One file per palette. Adding a new palette = one new file, zero other changes.

```kotlin
// shared/ui/tokens/brand/BrandTokens.kt
interface BrandTokens {
    val primary50: Color;  val primary100: Color; val primary200: Color
    val primary400: Color; val primary600: Color; val primary800: Color
    val neutral50: Color;  val neutral100: Color; val neutral200: Color
    val neutral400: Color; val neutral600: Color; val neutral900: Color
    val success300: Color; val success400: Color
    val error300: Color;   val error400: Color
    val warning300: Color; val warning400: Color
}
```

```kotlin
// shared/ui/tokens/brand/DefaultBrandTokens.kt
object DefaultBrandTokens : BrandTokens {
    override val primary50  = Color(0xFFEEEDFE)
    override val primary100 = Color(0xFFCECBF6)
    override val primary200 = Color(0xFFAFA9EC)
    override val primary400 = Color(0xFF7F77DD)
    override val primary600 = Color(0xFF534AB7)
    override val primary800 = Color(0xFF3C3489)
    override val neutral50  = Color(0xFFF9F9FB)
    override val neutral100 = Color(0xFFF0EFF3)
    override val neutral200 = Color(0xFFD3D1C7)
    override val neutral400 = Color(0xFF888780)
    override val neutral600 = Color(0xFF5F5E5A)
    override val neutral900 = Color(0xFF1A1A2E)
    override val success300 = Color(0xFF5DCAA5); override val success400 = Color(0xFF1D9E75)
    override val error300   = Color(0xFFF09595); override val error400   = Color(0xFFE24B4A)
    override val warning300 = Color(0xFFFAC775); override val warning400 = Color(0xFFEF9F27)
}
```

**RULE:** No composable, ViewModel, UseCase, or Repository imports a `BrandTokens` object directly. Only `buildColorScheme` consumes brand tokens.

### Layer 2 — Semantic Tokens (role mapping)

All dark/light logic lives here and ONLY here.

```kotlin
// shared/ui/tokens/AppColorScheme.kt
data class AppColorScheme(
    val backgroundPrimary: Color;   val backgroundSecondary: Color
    val backgroundCard: Color;      val backgroundOverlay: Color
    val contentPrimary: Color;      val contentSecondary: Color;    val contentDisabled: Color
    val actionPrimary: Color;       val actionPrimaryHover: Color
    val actionContent: Color;       val actionSecondary: Color;     val actionSecondaryContent: Color
    val borderDefault: Color;       val borderFocused: Color;       val borderStrong: Color
    val statusSuccess: Color;       val statusSuccessSubtle: Color
    val statusError: Color;         val statusErrorSubtle: Color
    val statusWarning: Color;       val statusWarningSubtle: Color
)

fun buildColorScheme(brand: BrandTokens, isDark: Boolean): AppColorScheme =
    if (isDark) darkScheme(brand) else lightScheme(brand)

private fun lightScheme(b: BrandTokens) = AppColorScheme(
    backgroundPrimary      = b.neutral50,
    backgroundSecondary    = b.neutral100,
    backgroundCard         = Color.White,
    backgroundOverlay      = Color.Black.copy(alpha = 0.4f),
    contentPrimary         = b.neutral900,
    contentSecondary       = b.neutral600,
    contentDisabled        = b.neutral400,
    actionPrimary          = b.primary400,
    actionPrimaryHover     = b.primary600,
    actionContent          = Color.White,
    actionSecondary        = b.primary50,
    actionSecondaryContent = b.primary600,
    borderDefault          = b.neutral200,
    borderFocused          = b.primary400,
    borderStrong           = b.neutral600,
    statusSuccess          = b.success400,
    statusSuccessSubtle    = b.success400.copy(alpha = 0.12f),
    statusError            = b.error400,
    statusErrorSubtle      = b.error400.copy(alpha = 0.12f),
    statusWarning          = b.warning400,
    statusWarningSubtle    = b.warning400.copy(alpha = 0.12f),
)

private fun darkScheme(b: BrandTokens) = AppColorScheme(
    backgroundPrimary      = b.neutral900,
    backgroundSecondary    = b.neutral600.copy(alpha = 0.25f),
    backgroundCard         = b.neutral100.copy(alpha = 0.08f),
    backgroundOverlay      = Color.Black.copy(alpha = 0.6f),
    contentPrimary         = b.neutral50,
    contentSecondary       = b.neutral200,
    contentDisabled        = b.neutral400,
    actionPrimary          = b.primary400,
    actionPrimaryHover     = b.primary200,
    actionContent          = Color.White,
    actionSecondary        = b.primary800,
    actionSecondaryContent = b.primary200,
    borderDefault          = b.neutral600.copy(alpha = 0.4f),
    borderFocused          = b.primary400,
    borderStrong           = b.neutral400,
    statusSuccess          = b.success300,
    statusSuccessSubtle    = b.success300.copy(alpha = 0.15f),
    statusError            = b.error300,
    statusErrorSubtle      = b.error300.copy(alpha = 0.15f),
    statusWarning          = b.warning300,
    statusWarningSubtle    = b.warning300.copy(alpha = 0.15f),
)
```

**RULE:** `buildColorScheme` is the only function that reads `isDark`. `isSystemInDarkTheme()` is called once in `MainActivity` and passed down. Forbidden everywhere else.

### Layer 3 — Component Tokens (scoped per component)

Each shared component defines its own colors data class sourced from `AppColorScheme`.

```kotlin
// shared/ui/components/button/AppButtonColors.kt
data class AppButtonColors(
    val containerColor: Color;         val contentColor: Color
    val containerHoverColor: Color;    val disabledContainerColor: Color
    val disabledContentColor: Color
)

fun AppColorScheme.primaryButtonColors() = AppButtonColors(
    containerColor         = actionPrimary,
    contentColor           = actionContent,
    containerHoverColor    = actionPrimaryHover,
    disabledContainerColor = actionPrimary.copy(alpha = 0.38f),
    disabledContentColor   = actionContent.copy(alpha = 0.38f),
)

fun AppColorScheme.secondaryButtonColors() = AppButtonColors(
    containerColor         = actionSecondary,
    contentColor           = actionSecondaryContent,
    containerHoverColor    = actionSecondary,
    disabledContainerColor = actionSecondary.copy(alpha = 0.38f),
    disabledContentColor   = actionSecondaryContent.copy(alpha = 0.38f),
)

// shared/ui/components/card/AppCardColors.kt
data class AppCardColors(val containerColor: Color, val borderColor: Color, val contentColor: Color)

fun AppColorScheme.defaultCardColors() = AppCardColors(
    containerColor = backgroundCard,
    borderColor    = borderDefault,
    contentColor   = contentPrimary,
)
```

### Theme wiring

```kotlin
// shared/ui/theme/AppThemeVariant.kt
enum class AppThemeVariant {
    Default, Ocean, Sunset;
    fun toBrandTokens(): BrandTokens = when (this) {
        Default -> DefaultBrandTokens
        Ocean   -> OceanBrandTokens
        Sunset  -> SunsetBrandTokens
    }
}

// shared/ui/theme/LocalAppColorScheme.kt
val LocalAppColorScheme = staticCompositionLocalOf<AppColorScheme> {
    error("Wrap root with AppTheme")
}
val LocalAppDimens = staticCompositionLocalOf<AppDimens> {
    error("Wrap root with AppTheme")
}

// shared/ui/theme/AppTheme.kt
@Composable
fun AppTheme(
    variant: AppThemeVariant = AppThemeVariant.Default,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(variant, isDark) {
        buildColorScheme(variant.toBrandTokens(), isDark)
    }
    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalAppDimens      provides AppDimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme.toMaterial3ColorScheme(),
            typography  = AppTypography,
            shapes      = AppShapes,
            content     = content,
        )
    }
}

fun AppColorScheme.toMaterial3ColorScheme(): ColorScheme {
    val dark = backgroundPrimary.luminance() < 0.5f
    val factory = if (dark) ::darkColorScheme else ::lightColorScheme
    return factory(
        primary      = actionPrimary,
        onPrimary    = actionContent,
        background   = backgroundPrimary,
        surface      = backgroundCard,
        onBackground = contentPrimary,
        onSurface    = contentPrimary,
        error        = statusError,
    )
}
```

```kotlin
// app/MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeRepository: ThemePreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val variant by themeRepository.observeTheme()
                .collectAsStateWithLifecycle(AppThemeVariant.Default)
            val darkPref by themeRepository.observeDarkMode()
                .collectAsStateWithLifecycle(DarkModePreference.System)

            // isSystemInDarkTheme() is called HERE and only here
            val isDark = when (darkPref) {
                DarkModePreference.System -> isSystemInDarkTheme()
                DarkModePreference.Light  -> false
                DarkModePreference.Dark   -> true
            }
            AppTheme(variant = variant, isDark = isDark) { AppNavHost() }
        }
    }
}
```

### Theme preference persistence

```kotlin
// shared/domain/preferences/ThemePreferencesRepository.kt
interface ThemePreferencesRepository {
    fun observeTheme(): Flow<AppThemeVariant>
    fun observeDarkMode(): Flow<DarkModePreference>
    suspend fun setTheme(variant: AppThemeVariant)
    suspend fun setDarkMode(preference: DarkModePreference)
}
enum class DarkModePreference { System, Light, Dark }

// shared/data/preferences/ThemePreferencesRepositoryImpl.kt
class ThemePreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferencesRepository {
    private object Keys {
        val THEME     = stringPreferencesKey("theme_variant")
        val DARK_MODE = stringPreferencesKey("dark_mode")
    }
    override fun observeTheme() = dataStore.data
        .map { AppThemeVariant.valueOf(it[Keys.THEME] ?: AppThemeVariant.Default.name) }
        .catch { emit(AppThemeVariant.Default) }

    override fun observeDarkMode() = dataStore.data
        .map { DarkModePreference.valueOf(it[Keys.DARK_MODE] ?: DarkModePreference.System.name) }
        .catch { emit(DarkModePreference.System) }

    override suspend fun setTheme(variant: AppThemeVariant) =
        dataStore.edit { it[Keys.THEME] = variant.name }

    override suspend fun setDarkMode(preference: DarkModePreference) =
        dataStore.edit { it[Keys.DARK_MODE] = preference.name }
}
```

### Supporting token files

```kotlin
// shared/ui/tokens/AppDimens.kt
data class AppDimens(
    val spacing: Spacing = Spacing(),
    val elevation: Elevation = Elevation(),
) {
    data class Spacing(
        val xs: Dp = 4.dp, val sm: Dp = 8.dp,  val md: Dp = 16.dp,
        val lg: Dp = 24.dp, val xl: Dp = 32.dp, val xxl: Dp = 48.dp,
    )
    data class Elevation(val card: Dp = 2.dp, val dialog: Dp = 8.dp, val sheet: Dp = 16.dp)
}

// shared/ui/tokens/AppShapes.kt
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, badges
    small      = RoundedCornerShape(8.dp),   // inputs
    medium     = RoundedCornerShape(12.dp),  // cards, dialogs
    large      = RoundedCornerShape(16.dp),  // bottom sheets
    extraLarge = RoundedCornerShape(24.dp),  // full-bleed cards
)

// shared/ui/tokens/AppIcons.kt
object AppIcons {
    val ArrowBack     = Icons.AutoMirrored.Outlined.ArrowBack
    val Check         = Icons.Outlined.Check
    val Close         = Icons.Outlined.Close
    val Error         = Icons.Outlined.ErrorOutline
    val Home          = Icons.Outlined.Home
    val Search        = Icons.Outlined.Search
    val Settings      = Icons.Outlined.Settings
    val Visibility    = Icons.Outlined.Visibility
    val VisibilityOff = Icons.Outlined.VisibilityOff
    // Add new icons here only — never inline in a composable
}
```

### Adding a new palette (follow in order)

1. Create `shared/ui/tokens/brand/NewPaletteBrandTokens.kt` implementing `BrandTokens`
2. Add entry to `AppThemeVariant` enum
3. Add mapping in `AppThemeVariant.toBrandTokens()`
4. Add preview variants for the new palette to every shared component
5. Run snapshot tests

No other files change — zero composables, zero semantic tokens, zero component tokens.

---

## 🧩 Shared Component Library (MANDATORY)

These components MUST be implemented in `shared/ui/components/` before any feature composable is written. Features NEVER build their own versions. No exceptions.

```
shared/ui/components/
├── button/     AppButton.kt, AppButtonColors.kt
├── card/       AppCard.kt, AppCardColors.kt
├── textfield/  AppTextField.kt, AppTextFieldColors.kt
├── topbar/     AppTopBar.kt
└── state/      AppLoadingIndicator.kt, AppErrorState.kt, AppEmptyState.kt
```

### AppButton

```kotlin
enum class AppButtonStyle { Primary, Secondary, Text }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val buttonColors = when (style) {
        AppButtonStyle.Primary   -> LocalAppColorScheme.current.primaryButtonColors()
        AppButtonStyle.Secondary -> LocalAppColorScheme.current.secondaryButtonColors()
        AppButtonStyle.Text      -> LocalAppColorScheme.current.textButtonColors()
    }
    Button(
        onClick  = onClick,
        enabled  = enabled && !isLoading,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = buttonColors.containerColor,
            contentColor           = buttonColors.contentColor,
            disabledContainerColor = buttonColors.disabledContainerColor,
            disabledContentColor   = buttonColors.disabledContentColor,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                color       = buttonColors.contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LocalAppDimens.current.spacing.xs))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

### AppTextField

```kotlin
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: UiText? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val colors  = LocalAppColorScheme.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            label           = { Text(label) },
            placeholder     = placeholder?.let { { Text(it) } },
            isError         = error != null,
            enabled         = enabled,
            readOnly        = readOnly,
            singleLine      = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon    = trailingIcon?.let {
                { IconButton(onClick = { onTrailingIconClick?.invoke() }) { Icon(it, null) } }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = colors.borderFocused,
                unfocusedBorderColor = colors.borderDefault,
                errorBorderColor     = colors.statusError,
                focusedLabelColor    = colors.actionPrimary,
                unfocusedLabelColor  = colors.contentSecondary,
                errorLabelColor      = colors.statusError,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        // Always reserves height — prevents layout jump when error appears
        Text(
            text     = error?.asString(context) ?: "",
            color    = if (error != null) colors.statusError else Color.Transparent,
            style    = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(
                start = LocalAppDimens.current.spacing.sm,
                top   = LocalAppDimens.current.spacing.xs,
            ),
        )
    }
}
```

### AppCard

```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardColors = LocalAppColorScheme.current.defaultCardColors()
    Surface(
        color    = cardColors.containerColor,
        border   = BorderStroke(0.5.dp, cardColors.borderColor),
        shape    = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(LocalAppDimens.current.spacing.md), content = content)
    }
}
```

### AppTopBar

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalAppColorScheme.current
    TopAppBar(
        title           = { Text(title, style = MaterialTheme.typography.titleLarge, color = colors.contentPrimary) },
        navigationIcon  = {
            onNavigateUp?.let {
                IconButton(onClick = it) {
                    Icon(AppIcons.ArrowBack, stringResource(R.string.cd_navigate_up), tint = colors.contentPrimary)
                }
            }
        },
        actions = actions,
        colors  = TopAppBarDefaults.topAppBarColors(containerColor = colors.backgroundPrimary),
        modifier = modifier,
    )
}
```

### AppLoadingIndicator

```kotlin
@Composable
fun AppLoadingIndicator(modifier: Modifier = Modifier, fullscreen: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = if (fullscreen) modifier.fillMaxSize()
                   else modifier.fillMaxWidth().padding(LocalAppDimens.current.spacing.xl),
    ) {
        CircularProgressIndicator(color = LocalAppColorScheme.current.actionPrimary)
    }
}
```

### AppErrorState

```kotlin
@Composable
fun AppErrorState(
    message: UiText,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val colors  = LocalAppColorScheme.current
    val dimens  = LocalAppDimens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(dimens.spacing.xl),
    ) {
        Icon(AppIcons.Error, contentDescription = null, tint = colors.statusError, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(dimens.spacing.md))
        Text(message.asString(context), color = colors.contentSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        onRetry?.let {
            Spacer(Modifier.height(dimens.spacing.lg))
            AppButton(stringResource(R.string.action_retry), it, style = AppButtonStyle.Secondary)
        }
    }
}
```

### AppEmptyState

```kotlin
@Composable
fun AppEmptyState(
    title: UiText,
    modifier: Modifier = Modifier,
    subtitle: UiText? = null,
    icon: ImageVector = AppIcons.EmptyBox,
    action: Pair<UiText, () -> Unit>? = null,
) {
    val context = LocalContext.current
    val colors  = LocalAppColorScheme.current
    val dimens  = LocalAppDimens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(dimens.spacing.xl),
    ) {
        Icon(icon, contentDescription = null, tint = colors.contentDisabled, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(dimens.spacing.md))
        Text(title.asString(context), color = colors.contentPrimary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        subtitle?.let {
            Spacer(Modifier.height(dimens.spacing.sm))
            Text(it.asString(context), color = colors.contentSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        action?.let { (label, onClick) ->
            Spacer(Modifier.height(dimens.spacing.lg))
            AppButton(label.asString(context), onClick)
        }
    }
}
```

### Shared component rules
- Shared components source colors from component tokens (Layer 3) — never raw `AppColorScheme`
- Every shared component MUST have previews in all four variants: Default·Light, Default·Dark, Ocean·Light, Ocean·Dark
- `AppButton` with `isLoading = true` shows a spinner and disables interaction — never replicate this at feature level
- `AppTextField` error row always reserves height — prevents layout jump on error appearance

### Standard screen scaffold

Every feature screen uses this structure:

```kotlin
@Composable
fun ExampleScreen(state: ExampleUiState, onIntent: (ExampleIntent) -> Unit) {
    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.example_title), onNavigateUp = { onIntent(ExampleIntent.NavigateUp) }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading && state.data == null ->
                    AppLoadingIndicator(fullscreen = true)
                state.error != null && state.data == null ->
                    AppErrorState(state.error, onRetry = { onIntent(ExampleIntent.Retry) })
                state.data != null ->
                    ExampleContent(state.data, onIntent)
            }
            // Refresh indicator overlays existing content
            if (state.isLoading && state.data != null) {
                AppLoadingIndicator(modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
```

---

## 🎨 UI Rules (STRICT)

- Each composable in its own file — never group multiple composables in one file
- Stateless UI — driven only by `UiState`, never by internal composable state
- Every composable MUST have all four `@Preview` variants: Default·Light, Default·Dark, Ocean·Light, Ocean·Dark
- Every preview wraps with `AppTheme { }` and uses mock data

**Token access (the only correct patterns):**
```kotlin
val colors = LocalAppColorScheme.current        // semantic colors
val dimens = LocalAppDimens.current             // spacing and elevation
MaterialTheme.shapes.medium                     // corner radius
AppIcons.Home                                   // icons
MaterialTheme.typography.bodyMedium             // text styles
```

**Forbidden — treat as build errors:**
```kotlin
Color(0xFF...)                          // hardcoded color
isSystemInDarkTheme()                   // in any composable
MaterialTheme.colorScheme.*             // in feature composables
Icons.Outlined.*                        // direct icon import
Modifier.padding(13.dp)                 // arbitrary spacing
RoundedCornerShape(10.dp)               // arbitrary corner radius
Button(...)                             // raw Material — use AppButton
OutlinedTextField(...)                  // raw Material — use AppTextField
```

---

## 🧪 Testing (MANDATORY)

**Coverage goal:** 100% line and method coverage.

**Must test:** Repositories, UseCases, Mappers, ViewModels, Shared components.

**ViewModel state transition tests** — assert each sequence explicitly:
- `initial → loading → success`
- `initial → loading → error` (data remains null)
- `success → refresh loading` (data retained) `→ success`
- `success → refresh loading → error` (data retained from previous success)

**Token system tests:**
- `buildColorScheme` tested for light and dark for every palette
- Assert roles that must differ between modes actually differ (`backgroundPrimary`, `contentPrimary`)
- Snapshot tests for all shared components across all four preview variants

**Shared component tests:**
- Snapshot/screenshot for each visual state: enabled, disabled, loading, error
- Semantics test verifying content descriptions and interactive roles

**UiText tests:**
- `StringResource.asString()` and `DynamicString.asString()` tested via a real `Context`
- `StringResource` equals/hashCode correctness with array args

**Rules:** Every class has tests. Cover success, error, and edge cases. Fix broken tests before any PR — never leave failing tests.

---

## ⚙️ Gradle & Build (STRICT)

**Version catalog:** ALL dependencies in `libs.versions.toml`. No version literals in `build.gradle.kts`. Reference via catalog accessors only (e.g. `libs.retrofit`).

**Dependency ordering:** Alphabetical in all blocks across `libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`. Enforced on every add/remove.

**No unused dependencies.** Verify after generation, remove any that are unused, re-sort.

**Build types:**
- `debug`: `debuggable = true`, minification off, `applicationIdSuffix = ".debug"`
- `release`: `minifyEnabled = true`, `shrinkResources = true`, ProGuard rules required (never `-keep class * { *; }`), signing via environment variables

**Product flavors** (if multi-environment): `dev`, `staging`, `prod`. Base URLs and feature flags via `BuildConfig` per flavor. Never `if (BuildConfig.DEBUG)` in feature code — use an injected flag interface.

**Baseline profiles:** Generate for the main user journey at `app/src/main/baseline-prof.txt`. Regenerate after major UI or navigation changes.

---

## 🎯 Output Requirements

Generate the full project structure and one complete feature including:
- API, Room, Repository, UseCases, ViewModel (NVI), UI, Tests
- All shared components fully implemented (`AppButton`, `AppTextField`, `AppCard`, `AppTopBar`, `AppLoadingIndicator`, `AppErrorState`, `AppEmptyState`)
- `UiText` in `shared/ui/utils/`
- `AppError` and `Result<T>` in `shared/domain/`
- `AppTheme` wiring in `MainActivity`
- At least two `BrandTokens` palettes
- `ThemePreferencesRepository` implemented and wired

---

## 🚫 Hard Rules — treat every violation as a build error

- Do NOT duplicate shared logic
- Do NOT bypass UseCases in ViewModels
- Do NOT put Android imports in the domain layer
- Do NOT group multiple composables in one file
- Do NOT leave unused Gradle dependencies
- Do NOT leave Gradle dependencies unsorted
- Do NOT hardcode base URLs, API keys, or environment config
- Do NOT use raw string routes for navigation
- Do NOT store navigation events in `UiState`
- Do NOT use `Color()` literals in composables
- Do NOT call `isSystemInDarkTheme()` outside `MainActivity`
- Do NOT use `MaterialTheme.colorScheme` in feature composables
- Do NOT import `Icons.*` directly in feature composables
- Do NOT use arbitrary `dp` values — use `AppDimens` tokens
- Do NOT use arbitrary corner radius values — use `AppShapes` / `MaterialTheme.shapes`
- Do NOT use a raw `String` for error in `UiState` — always `UiText`
- Do NOT use `Boolean?` for loading state — always non-null `Boolean`
- Do NOT call `stringResource()` or `context.getString()` in a ViewModel
- Do NOT use `Button`, `OutlinedTextField`, `Card`, or `TopAppBar` directly in feature composables
- Do NOT implement loading, error, or empty states locally in a feature
- Do NOT use `android.util.Log` — use Timber
- Do NOT use `runBlocking` outside tests
- Do NOT hardcode any user-visible string in Kotlin — every string must live in `res/values/strings.xml` and be referenced via `R.string.*` or `stringResource()`
- Do NOT add a string to `values/strings.xml` without also adding it to every supported locale folder (`values-el/strings.xml`, etc.) — both files must stay in sync at all times
- Do NOT implement per-app language switching with context wrapping or manual locale hacks — use `AppCompatDelegate.setApplicationLocales()` exclusively, triggered from the presentation layer (NavGraph composable), never from a ViewModel or UseCase
- `MainActivity` MUST extend `AppCompatActivity` (not `ComponentActivity`) so that `AppCompatDelegate` can apply stored locale on all API levels via `attachBaseContext2()`

---

## ✅ Definition of Done (MANDATORY)

After **every** implementation session — no exceptions:

1. Run the build and confirm it passes:
```bash
./gradlew :app:assembleDebug
```

2. Stage the changed files (specific files only — never `git add -A`):
```bash
git add <specific files>
```

Do not run `git commit` — the developer commits manually after reviewing.
Do not report a task as complete until `BUILD SUCCESSFUL` and `git add` are confirmed. Never stage `.DS_Store`, `.env`, or generated build artifacts.