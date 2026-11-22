# Data Saver Media — Android Pilot (0.1)

An early Android Studio project for testing on-device photo and video optimization. App name: **Data Saver Media**.

## Current pilot features
- Select a photo or video using Android's system picker.
- Photo: decode, resize to a maximum 1600-pixel side, and JPEG encode at quality 82.
- Video: transcode to H.264/AAC with a 1.5 Mbps video bitrate using Jetpack Media3 Transformer.
- Preserve the selected original (the app only reads/copies it; it does not overwrite it).
- Show original and optimized sizes, megabits saved, reduction percentage, and estimated dollar value.
- Share the optimized copy through Android's share sheet.
- Dollar estimate uses the historical baseline entered in the discussion: $10 per 3 GB, with decimal GB (1 GB = 1,000,000,000 bytes).

## Important limitations
- This is a prototype, not a finished system-wide upload interceptor.
- It does not yet appear as a universal source in every app's attachment picker.
- It optimizes media selected inside this app, then hands the optimized copy to another app using Android's share sheet.
- Video bitrate is a fixed pilot setting; actual savings and quality vary by source video, codec, duration, and device encoder.
- Estimated dollar value is an allocated share of the data-plan price, not a guaranteed reduction in a fixed monthly bill.
- The prototype's total savings counter is session-only and resets when the app process is recreated.
- No network traffic is intercepted; the app measures file-size differences. Actual transmitted bytes can differ due to app/server compression and protocol overhead.

## Build
1. Install Android Studio (current stable) on a computer.
2. Open this folder as an existing project.
3. Let Gradle sync and download dependencies (internet required).
4. Connect the Motorola phone with USB debugging enabled, or enable wireless debugging.
5. Select the phone as the run target and press Run.

The project targets Android 16 (API 36), minimum Android 8.0 (API 26), and uses Jetpack Media3 Transformer 1.11.1.
