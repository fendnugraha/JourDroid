# Implementation Plan - POS Login Fix & Model Synchronization

Address the persistent 401 error and sync the Android data models with the provided Laravel `storeAndroid` implementation.

## User Review Required

> [!IMPORTANT]
> - **Input Sanitization**: I will force `.trim()` on email and password inputs to prevent common mobile autocomplete issues (trailing spaces).
> - **Header Cleanup**: I will remove the `Origin` and `Referer` headers I added previously, as they might be causing security mismatches with your server's CORS configuration.
> - **Model Flexibility**: I will make all fields in `UserData` and `LoginResponse` nullable to ensure the app doesn't crash when parsing the new, complex user object returned by your PHP code.

## Proposed Changes

### Network Layer

#### [MODIFY] [ApiClient.kt](file:///E:/Android%20Project/app/src/main/java/com/example/jourdroid/api/ApiClient.kt)
- Remove `Origin` and `Referer` headers.
- Keep `X-Requested-With: XMLHttpRequest` and `Accept: application/json`.

### Data Layer

#### [MODIFY] [LoginResponse.kt](file:///E:/Android%20Project/app/src/main/java/com/example/jourdroid/data/LoginResponse.kt)
- Make `UserData` fields nullable.
- Ensure `token_type` is captured.
- Update `UserData` to support the loaded relations (`warehouse`, `attendances`).

### UI Layer

#### [MODIFY] [LoginScreen.kt](file:///E:/Android%20Project/app/src/main/java/com/example/jourdroid/ui/auth/LoginScreen.kt)
- Trim email and password before calling the API.

## Verification Plan

### Manual Verification
- Attempt login and check Logcat.
- Verify if trimming the input resolves the 401 "Email atau password salah".
- Verify that a successful response is parsed without crashes even with the extra nested data (`primaryCash`, `warningActive`, etc.).
