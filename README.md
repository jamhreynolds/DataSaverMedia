# Data Saver Media — Android Pilot

This is the current Android pilot project for Data Saver Media. It is an early prototype, not the finished consumer app.

## Current pilot features
- Select a photo or video using Android's system picker.
- Photos are resized to a maximum 1600-pixel side and JPEG-encoded at quality 82.
- Videos are transcoded to H.264/AAC using Jetpack Media3 Transformer, with a 1.5 Mbps video bitrate.
- The selected original is left untouched.
- The app reports original and optimized file sizes, megabits saved, reduction percentage, and an estimated dollar value.
- Optimized copies can be shared through Android's share sheet.

## Build compatibility
- Android Gradle Plugin: 8.13.2
- Kotlin Android plugin: 2.2.20
- Gradle distribution: 8.13 (configured in `gradle/wrapper/gradle-wrapper.properties`)
- Compile/target SDK: 36
- Minimum SDK: 26
- Java: 17

**Important:** The Gradle wrapper configuration is included, but the generated `gradle-wrapper.jar` and launcher scripts are not included in this package. Android Studio can use a locally installed Gradle 8.13 distribution, and the GitHub Actions workflow below installs Gradle 8.13 directly. Do not let the project use Gradle 9.x with Android Gradle Plugin 8.13.2.

## Build in Android Studio
1. Open this project folder (`DataSaverMedia-GitHub-Upload`) in Android Studio.
2. In Android Studio, open **Settings → Build, Execution, Deployment → Build Tools → Gradle**.
3. Set the Gradle distribution/version to **Gradle 8.13** if Android Studio asks for a local Gradle installation or continues selecting Gradle 9.x.
4. Use JDK 17 for Gradle, then sync the project.
5. After a successful sync, choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Build with GitHub Actions
The workflow is at `.github/workflows/build-apk.yml`. It builds with Gradle 8.13 and uploads `app-debug.apk` as an Actions artifact.

When using GitHub's website to upload files, upload the project contents into the repository root so that `app/`, `build.gradle.kts`, and `settings.gradle.kts` are directly visible at the top level. GitHub's web uploader may not show hidden `.github` folders. If it skips that folder, create a new file in GitHub with this exact path:

`.github/workflows/build-apk.yml`

Then paste the workflow from this project into that file and commit it.

## Important limitations
- This is a prototype, not a finished system-wide upload interceptor.
- It does not intercept encrypted network traffic or automatically replace media in every app.
- It optimizes media selected inside this app, then hands the optimized copy to another app using Android's share sheet.
- Video bitrate is fixed for this pilot; quality and savings vary by source and device.
- Estimated dollar value is an allocated share of a plan price, not a guaranteed bill reduction. It only represents actual cash savings if the data reduction avoids overage charges or prepaid data purchases.
- The cumulative savings counter is session-only and resets when the app process is recreated.
- Actual transmitted bytes can differ because other apps and servers may further compress media or add protocol overhead.
