# Publishing Woof Battle on Google Play

## Readiness checklist

| | Item | Status |
|---|---|---|
| ✅ | Signed App Bundle (`./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`), version **1.2 (3)** | ready |
| ✅ | Target API 37 (Play's minimum is met), 64-bit native code included | ready |
| ✅ | **16 KB memory page support** (required for Android 15+ targets with native code): the only native library, `libandroidx.graphics.path.so`, is 16 KB aligned in all 4 ABIs | checked |
| ✅ | Native debug symbols: requested (`debugSymbolLevel = SYMBOL_TABLE`), but the only native library is a prebuilt AndroidX one that ships stripped, so Play may show a harmless "no debug symbols" warning | ok |
| ✅ | No permissions (no INTERNET, no AD_ID), no ads, no analytics, no data collected | ready |
| ✅ | Privacy explained **inside the app** (parent area → Privacy, 4 languages) | ready |
| ✅ | Store texts EN / FR / ES / NL + "What's new" for 1.2 | `fastlane/metadata/android/<locale>/` |
| ✅ | Icon 512×512, feature graphic 1024×500, 5 phone, 2 seven-inch, 3 ten-inch screenshots | `fastlane/metadata/android/en-US/images/` |
| ✅ | Privacy policy web page (4 languages) | `docs/privacy-policy.html` |
| ⬜ | **You:** your contact e-mail and developer name in `docs/privacy-policy.html` | |
| ⬜ | **You:** the privacy policy online at a public URL (step 6) | |
| ⬜ | **You:** back up the upload key (`keystore/` + `keystore.properties`, not in git) | |
| ⬜ | **You:** confirm the package name `com.woofbattle.app` (permanent after the first upload) | |
| ⬜ | **You:** Play Console developer account, closed test, review | steps 1–5 |

Graphics are rendered from the app's own code: `./gradlew :app:testDebugUnitTest -Pscreenshots` writes
fresh copies to `app/build/store/play/` (copy them over the files in `fastlane/.../images/`).

## 0. Permanent decisions — before the first upload
- **Package name** `com.woofbattle.app` (`applicationId` in `app/build.gradle.kts`). It can never change
  after publishing. Change it now if you want your own, e.g. `com.yourname.woofbattle`.
- **Upload key** `keystore/woofbattle-upload.jks` + `keystore.properties` (password inside). Keep copies
  in a password manager or private cloud drive. With Play App Signing a lost upload key can be reset
  through Play support, but that takes days.
  Upload key SHA-256: `76:74:66:0A:06:8F:6A:E2:B9:42:05:5D:B4:53:B0:8E:4D:A6:F7:99:A9:DE:8F:61:E7:D1:31:E1:B7:4E:E0:54`

## 1. Developer account — You
1. Sign up at <https://play.google.com/console> (one-time USD 25), verify your identity.
2. **New personal accounts** must run a **closed test with at least 12 testers for 14 days** before
   production. Line up friends/family with Android phones.

## 2. Create the app
*Create app* → name `Woof Battle: Dog Card Game` · default language English (United States) ·
**Game** · **Free** · accept the declarations.

## 3. App content (Policy → App content)
| Section | Answer |
|---|---|
| Privacy policy | Public URL of `docs/privacy-policy.html` (step 6) |
| Ads | **No** |
| App access | **All functionality is available without special access** |
| Advertising ID | **No**, the app doesn't use it (no AD_ID permission) |
| Content rating | Category *Game*; answer **No** to violence, fear, sex, language, drugs, gambling, user interaction, location sharing, purchases → expect **PEGI 3 / ESRB Everyone / USK 0** |
| Target audience | **5 and under** and **6–8** → Families Policy applies; the app complies (no ads, no data, no external links, parent gate) |
| Data safety | **No data collected**, **no data shared** |
| Government / financial / health / news | **No** |

## 4. Store listing (Grow → Store presence → Main store listing)
- Copy `title.txt`, `short_description.txt`, `full_description.txt` from `fastlane/metadata/android/en-US/`.
- *Manage translations* → add French, Spanish, Dutch from the `fr-FR`, `es-ES`, `nl-NL` folders.
- Graphics from `fastlane/metadata/android/en-US/images/` (icon, feature graphic, phone, 7-inch, 10-inch).
- Category **Game → Card** (or Educational). Tags: Card, Casual, Kids, Offline, Animals.
- Contact e-mail (required, shown publicly).

Tip: the `fastlane/` folder follows the *fastlane supply* layout, so once you have a Play API service
account, `fastlane supply --aab app/build/outputs/bundle/release/app-release.aab` uploads the listing,
graphics, release notes and bundle in one go.

## 5. Release
1. *Testing → Closed testing* (or Internal testing) → *Create new release*; keep **Play App Signing** on.
2. Upload `app-release.aab`; release name `1.2 (3)`; release notes from `changelogs/3.txt` per language.
3. Add testers, roll out, install from the tester link, play a full game on a real phone.
4. After the test period: *Production → Create release* → promote the same build → countries → submit.
   First reviews of kids' apps can take several days.

## 6. Hosting the privacy policy
The repository is private. Either:
- make it public and enable **GitHub Pages** (Settings → Pages → *Deploy from a branch* → `main`,
  folder `/docs`). URL: `https://sancano1-dev.github.io/GameWaf/privacy-policy.html`, or
- upload `docs/privacy-policy.html` to any public site you control.

## 7. Every next update
1. Raise `versionCode` (always increasing) and `versionName` in `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. `./gradlew test bundleRelease` and upload the new `app-release.aab`.

## Notes
- **Two signatures, on purpose.** The APK on GitHub Releases is signed with the same key as v1.0/v1.1,
  so it installs as an update over them. The Play bundle is signed with the upload key and Google
  re-signs it for Play users. A GitHub copy and a Play copy can't replace each other: uninstall one
  before installing the other (otherwise Android says "App not installed").
- Code shrinking (R8) is off for release builds, so the store build runs exactly the code covered by
  the tests. If you turn it on later for a smaller download, test that build on a real phone first.
