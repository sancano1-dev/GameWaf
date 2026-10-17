# Publishing Woof Battle on Google Play

Everything the Play Console asks for is in this folder. Steps marked **You** need your Google
account; the rest is already prepared.

| What | Where |
|---|---|
| Signed App Bundle to upload | `app/build/outputs/bundle/release/app-release.aab` (build with `./gradlew bundleRelease`) |
| Store texts EN / FR / ES / NL | `store/listing/*.md` |
| App icon 512×512 | `store/graphics/icon_512.png` |
| Feature graphic 1024×500 | `store/graphics/feature_graphic_1024x500.png` |
| Phone screenshots 1080×1920 (5) | `store/graphics/phone_*.png` |
| 7-inch tablet screenshots 1920×1200 (2) | `store/graphics/tablet7_*.png` |
| 10-inch tablet screenshots 2560×1600 (3) | `store/graphics/tablet10_*.png` |
| Privacy policy text | `store/privacy-policy.md` |

The graphics are rendered from the app's own code: `./gradlew :app:testDebugUnitTest -Pscreenshots`
writes them to `app/build/store/`.

## 0. Before the first upload — decide these, they are permanent
- **Package name:** `com.woofbattle.app` (in `app/build.gradle.kts` → `applicationId`). Once an app is
  published with it, it can never change. If you prefer your own, e.g. `com.yourname.woofbattle`,
  change it **before** the first upload.
- **Upload key:** `keystore/woofbattle-upload.jks` + `keystore.properties` (password inside).
  They are **not in git**. **Back them up now** (password manager, private cloud drive). With Play
  App Signing (default) a lost upload key can be reset through Play support, but it takes days.
  Upload key SHA-256: `76:74:66:0A:06:8F:6A:E2:B9:42:05:5D:B4:53:B0:8E:4D:A6:F7:99:A9:DE:8F:61:E7:D1:31:E1:B7:4E:E0:54`

## 1. Developer account — **You**
1. Sign up at <https://play.google.com/console> (one-time USD 25 fee), verify your identity.
2. **New personal accounts** must run a **closed test with at least 12 testers for 14 days**
   before they can publish to production. Plan for this (friends/family with Android phones).

## 2. Create the app — **You**
Play Console → *Create app*:
- App name: `Woof Battle: Dog Card Game` · Default language: English (United States)
- App or game: **Game** · Free or paid: **Free**
- Accept the declarations (Developer Program Policies, US export laws).

## 3. App content (Policy → App content)
| Section | Answer |
|---|---|
| Privacy policy | A **public URL** to `store/privacy-policy.md` (see step 6). Fill in your contact e-mail and name first. |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** |
| Content rating | Questionnaire, category *Game*. Answer **No** to violence, fear, sexuality, language, drugs, gambling, user interaction, sharing location, purchases. Expected: **PEGI 3 / ESRB Everyone / USK 0** |
| Target audience | Ages **5 and under** and **6–8**. This puts the app under the **Families Policy** (the app complies: no ads, no data collection, no external links, parent gate). Answer **No** to "appeals to children only unintentionally". |
| Data safety | **No data collected**, **no data shared**. "Is all user data encrypted in transit?" → not applicable (no data). "Can users request deletion?" → no data collected. |
| Government / financial / health / news apps | **No** |
| Families: Designed for Families | Opt in if offered; the app meets the requirements |

## 4. Store listing (Grow → Store presence → Main store listing)
- Copy *App name*, *Short description*, *Full description* from `store/listing/en-US.md`.
- Add translations (*Manage translations* → add French, Spanish, Dutch) from the other three files.
- Graphics: upload the icon, the feature graphic, the 5 phone screenshots, the 7-inch and 10-inch
  tablet screenshots from `store/graphics/`.
- Category: **Game → Card** (or *Educational*). Tags: Card, Casual, Kids, Offline, Animals.
- Contact details: an e-mail is required (shown publicly). Website optional.

## 5. Release
1. *Testing → Internal testing* (or *Closed testing* for new accounts) → *Create new release*.
2. Keep **Play App Signing** on (Google re-signs the app; you keep the upload key).
3. Upload `app-release.aab`. Release name: `1.2 (3)`. Release notes, e.g.:
   `First release: the bigger dog wins! 8 manga dogs, play vs the computer or 2–4 kids on one phone.`
4. Add testers, roll out, install from the tester link, play a full game on a real phone.
5. When ready: *Production → Create release* → promote the same build → choose countries → submit for
   review (first reviews of kids' apps can take several days).

## 6. Hosting the privacy policy (needs a public URL)
The GitHub repository is private, so its files aren't public. Options:
- Make the repo public and enable **GitHub Pages** (Settings → Pages → branch `main`, folder `/`),
  then use `https://sancano1-dev.github.io/GameWaf/store/privacy-policy` — or
- Paste the text into a public page you control (Google Sites, a personal site, a public Gist).

## 7. Every next update
1. Raise `versionCode` (must always increase) and `versionName` in `app/build.gradle.kts`.
2. `./gradlew test bundleRelease`
3. Upload the new `app-release.aab` to a new release.

## Notes
- **Two signatures, on purpose.** The APK on GitHub Releases is signed with the same key as the
  earlier v1.0/v1.1 APKs, so it installs as an update over them. The `app-release.aab` for Google
  Play is signed with the upload key, and Google re-signs it for Play users. A copy installed from
  GitHub and one installed from Play can't replace each other: uninstall one before installing the
  other (otherwise Android says "App not installed").
- Code shrinking (R8) is off for release builds, so the store build runs exactly the code covered
  by the tests. If you turn it on later for a smaller download, test that build on a real phone.
