# CloudStream Skia

CloudStream Skia is a fork of [CloudStream](https://github.com/recloudstream/cloudstream)
with a redesigned interface. It follows upstream `master` and keeps its own changes small
and separate, so upstream updates can be merged continuously.

All credit for the app itself goes to the CloudStream contributors. Like upstream, this
fork is licensed under the GNU GPL v3.

## How the fork is laid out

| What | Where |
| --- | --- |
| Resource overrides (colors, dimens, styles, layouts, fonts) | `app/src/skia/res` |
| Fork-only Kotlin code (app) | `app/src/skia/java/com/lagradost/cloudstream3/skia` |
| Fork-only Kotlin code (Compose, shared module) | `shared/src/commonMain/kotlin/com/lagradost/cloudstream4/skia` |
| Launcher label | `app/src/skia/AndroidManifest.xml` |
| Build settings (application id, source sets) | the "CloudStream Skia" block at the end of `app/build.gradle.kts` |
| Upstream sync and release builds | `.github/workflows/skia.yml` |
| Drift check | `scripts/skia/drift-check.py` |

`app/src/skia` is added to both product flavors (`stable` and `prerelease`). Flavor source
sets take priority over `main`, so a resource in `app/src/skia/res` replaces the upstream
resource with the same name without editing the upstream file:

- `values` resources override **by name**. Override single colors, dimens and small styles;
  never copy large upstream styles such as `AppTheme`.
- Files (`layout`, `drawable`, `color`, `font`) override **by file name**. A layout override
  must keep every view id and view type the upstream Kotlin code binds, otherwise the screen
  fails to inflate.
- Do not add view ids to layouts whose binding upstream also applies to a TV variant
  (`fragment_home`, `fragment_search`, `fragment_library`, `activity_main`,
  `player_custom_layout`): the TV layout would miss the id and fail to bind. Add such views
  at runtime from fork code instead (see `SkiaSearchStatus`).

## Hooks in upstream files

Where upstream code has to call fork code, the change is a single line marked with
`// Skia hook`. List them with:

```sh
git grep -n "Skia hook"
```

Keep hooks to one line each. If a hook needs more logic, move the logic into the fork's
own files and call it from the hook.

## Following upstream

`.github/workflows/skia.yml` runs every day (and on demand):

1. merges `recloudstream/cloudstream` `master` into this fork's `master`;
2. opens an issue if the merge conflicts (resolve it locally, usually in a hook line);
3. runs the drift check and opens an issue when upstream changed a file this fork overrides;
4. builds the prerelease APK, signs it with the fork's key and publishes it as the
   `pre-release` GitHub release, which the in-app updater checks.

To sync by hand:

```sh
git fetch upstream
python scripts/skia/drift-check.py HEAD upstream/master   # what to port into overrides
git merge upstream/master
```

## Building locally

Requirements: JDK 17 and the Android SDK (platform 37). Then:

```sh
./gradlew assemblePrereleaseDebug
```

The debug build installs as `com.lagradost.cloudstream3.skia.debug`, next to the official
apps. Release builds use `com.lagradost.cloudstream3.skia`.

## Fonts

The interface font is [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans),
licensed under the SIL Open Font License 1.1 (`app/src/skia/LICENSE-PlusJakartaSans-OFL.txt`).
