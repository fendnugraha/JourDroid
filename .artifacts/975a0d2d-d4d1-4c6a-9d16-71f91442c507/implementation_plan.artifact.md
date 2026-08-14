# Fix "No parameter with name 'ignoreCase' found" compilation error

The project fails to compile because `UserData.role` is defined as `Any?`. When it is assigned to `userRole`, the type of `userRole` becomes `Any`. The `Any.equals()` method does not have an `ignoreCase` parameter, which is why the compiler throws an error when attempting to use it.

## User Review Required

> [!NOTE]
> I will convert the `role` property to a `String` using `.toString()` before performing equality checks. This ensures the Kotlin `String.equals(other: String?, ignoreCase: Boolean)` extension function is used.

## Proposed Changes

### JourDroid App

#### [MODIFY] [MainAppContainer.kt](file:///E:/Android%20Project/app/src/main/java/com/example/jourdroid/ui/app/MainAppContainer.kt)

- Update `userRole` assignment to convert `user.role` to a `String` safely.

#### [MODIFY] [ProfileScreen.kt](file:///E:/Android%20Project/app/src/main/java/com/example/jourdroid/ui/app/profile/ProfileScreen.kt)

- Update `userRole` assignment to ensure it is a `String`, preventing potential type mismatches in `Text` components.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the compilation error is resolved.

### Manual Verification
- None required as this is a compile-time fix.
