# Fix Kotlin Compiler Crash in ClassDetailActivity

The build is failing with a Kotlin compiler crash (`java.lang.IllegalArgumentException: source must not be null` in `FirIncompatibleClassExpressionChecker`) when analyzing `ClassDetailActivity.kt`. This is likely a K2 compiler bug triggered by the `registerForActivityResult` declaration.

## Proposed Changes

### [app]

#### [MODIFY] [ClassDetailActivity.kt](file:///C:/Users/kunal kumar/AndroidStudioProjects/ClassroomConnect/app/src/main/java/com/example/classroomconnect/ClassDetailActivity.kt)

- Add explicit type `ActivityResultLauncher<String>` to `pdfPickerLauncher`.
- Add explicit type `Uri?` to the lambda parameter in `registerForActivityResult`.
- Explicitly import `android.net.Uri` if needed (it is already imported).

```kotlin
    private val pdfPickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                pdfUri = uri
                binding.txtSelectedFile.text = getFileName(uri)
            }
        }
```

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the build completes successfully.
