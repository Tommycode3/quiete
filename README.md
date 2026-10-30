# quiete

Quiete is an Android launcher with no icons, widgets or pages. The home screen is a
clock and a short list of app names in the bottom-left corner over the wallpaper; the
full app list with search and an A to Z rail is one upward drag away. Kotlin and
Jetpack Compose, no dependencies beyond AndroidX.

## Build

Needs JDK 17 and the Android SDK (API 35). Point `JAVA_HOME` and `ANDROID_HOME` at
your installs (or put `sdk.dir=...` in `local.properties`), then:

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity info.quiete.launcher/.MainActivity
```

Android 12+ (API 31) for cross-window blur, portrait only. It uses
`QUERY_ALL_PACKAGES` to list launchable apps.

## Layout and motion

The launcher is built around one number, `progress` ∈ [0, 1], an `Animatable<Float>`
owned by `LauncherRoot`. Home and drawer read it; only gestures and springs write it.
`MainActivity` collects it with `snapshotFlow` to blur the wallpaper behind the
drawer, using the platform's cross-window blur (`FLAG_BLUR_BEHIND`), since
`Modifier.blur` only blurs a composable's own content. Each write is a binder call, so
the radius is quantised to 8px steps, $r = 8\left\lfloor 64\,p / 8 \right\rfloor$: at
most 9 writes over a full drag instead of one per frame at 120 Hz.

```
data/          AppRepository (LauncherApps), fuzzy Search, Prefs
ui/theme/      Tokens.kt (colour, space, motion), Type.kt, Squircle.kt
ui/effect/     Panel (lit hairline ring), Glass, Grain
ui/home/       Clock, HomeScreen
ui/drawer/     DrawerScreen, Scrubber, SearchField
ui/            AppRow, ActionSheet, LauncherRoot (owns `progress`)
```

Everything that moves is a spring; only opacity uses a fixed curve. For a unit-mass
spring with stiffness $k$ and damping ratio $\zeta$, $\omega_n = \sqrt{k}$, the 2%
settling time is about $4/(\zeta\,\omega_n)$ and the overshoot is
$e^{-\pi\zeta/\sqrt{1-\zeta^2}}$. The tokens in `ui/theme/Tokens.kt` work out to:

| Token | $k$ | $\zeta$ | $\omega_n$ (rad/s) | $t_s$ | overshoot |
|---|---|---|---|---|---|
| `standard` | 620 | 0.92 | 24.9 | ≈ 175 ms | 0.06% |
| `sheet` (drawer) | 420 | 0.94 | 20.5 | ≈ 208 ms | 0.02% |
| `press` | 10 000 | 0.80 | 100 | ≈ 50 ms | 1.5% |

Springs keep the drawer interruptible: catch it mid-flight and it resolves from your
finger's velocity. On release, `settle(v)` opens the drawer if $v < -900$ px/s,
closes it if $v > 900$ px/s, and otherwise opens it if `progress` > 0.35, so a fast
flick commits even from 10% open.

Rows show a press by filling a squircle of light behind them, with scale reduced to
`PressScale = 0.985`. The A to Z rail (`ui/drawer/Scrubber.kt`) ticks the haptics on
every letter change, swells letters toward the finger so they stay legible under the
thumb, and sits at 20% ink at rest.

## The squircle

A rounded rectangle joins a straight edge to a circular arc, so curvature jumps from
$0$ to $1/r$ at the tangent point. `SquircleShape` replaces each corner with a quarter
superellipse:

$$
\left|\frac{x}{r}\right|^{n} + \left|\frac{y}{r}\right|^{n} = 1,
\qquad
(x, y) = r\left(\cos^{2/n} t,\ \sin^{2/n} t\right),\quad t \in \left[0, \tfrac{\pi}{2}\right]
$$

with $n = 4.5$ ($n = 2$ is a circle, $n \to \infty$ a square). Each corner is a
20-segment polyline; the error is well under a physical pixel. In `Float`,
$\cos(\pi/2) \approx -4.4\times10^{-8}$, and a negative base to a fractional power is
`NaN`, which silently breaks the path and makes anything clipped to it vanish, so both
terms are clamped at $0$ before `pow`. Radii are 22dp (panels), 13dp (rows inside
panels) and 9dp (chips). Panels also get a hairline ring that is bright at the top and
nearly black at the bottom (`ui/effect/Panel.kt`).

## Search ranking

`data/Search.kt` scores each label $\ell$ against the upper-cased query $q$, then
sorts by score, shorter label, and alphabetically:

$$
s(\ell, q) =
\begin{cases}
10^6 + \max(0,\ 100 - |\ell|) & \ell \text{ starts with } q \\
10^5 + \max(0,\ 100 - i) & q \text{ starts the word at index } i \\
10^3 + \sum_{j} \left(1 + 2\,\sigma_j\right) & q \text{ is a subsequence of } \ell \\
\varnothing & \text{otherwise}
\end{cases}
$$

where $\sigma_j$ is the length of the run of adjacent matched characters ending at the
$j$-th match. The tiers are orders of magnitude apart so a weaker tier never outranks
a stronger one, and within the subsequence tier clustered matches beat scattered ones
(`wsp` matches *WhatSaPp*).

## Status

v0.1.0, tested on an Android 15 (API 35) emulator as the default home: pinned apps
seed from the device's dialer, messages, browser and camera, the drawer opens and
blurs the wallpaper, `ca` ranks Camera, Calendar, Contacts, Voice Search, long-press
opens the action sheet, and HOME resets everything.

- Not tested on physical hardware. No automated tests.
- No icons in drawer rows (`AppRepository.icon` exists, nothing calls it).
- `Prefs.launchCount` is recorded but does not feed search ranking yet.
- No drag reordering of pinned apps, no work-profile apps, no landscape.

## License

MIT. See [LICENSE](LICENSE).
