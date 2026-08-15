# Implementation Plan - Fix App Icon Crash and Visuals

The app is force closing due to a resource conflict or invalid adaptive icon configuration. There is a naming collision between `@color/ic_launcher_background` (Black) and `@drawable/ic_launcher_background` (Green Vector), and the adaptive icon is currently referencing the wrong layers.

## Proposed Changes

### Adaptive Icon Configuration

#### [MODIFY] [ic_launcher.xml](file:///E:/Android Project/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
- Set `background` to `@color/ic_launcher_background` to ensure a solid black background.
- Set `foreground` to `@drawable/ic_launcher_foreground` to use the new `app_logo.png` with proper insets.

#### [MODIFY] [ic_launcher_round.xml](file:///E:/Android Project/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml)
- Match the changes in `ic_launcher.xml` for consistency.

### Resource Cleanup

#### [DELETE] [ic_launcher_background.xml](file:///E:/Android Project/app/src/main/res/drawable/ic_launcher_background.xml)
- Remove this file to resolve the naming conflict with the color resource. A solid black color is preferred for the new brand identity.

## Verification Plan

### Build & Run
- Run `gradle assembleDebug` to ensure no build errors.
- Deploy to the device and verify the app no longer force closes on launch or when viewed in the task switcher.

### Visual Check
- Confirm the app icon shows the new logo on a solid black background.
- Verify the logo is correctly centered and sized.
