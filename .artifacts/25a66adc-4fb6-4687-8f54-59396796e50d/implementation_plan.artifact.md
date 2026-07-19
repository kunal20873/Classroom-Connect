# Implementation Plan - Fix Kotlin Compiler Crash in ClassDetailActivity

The build is failing with a `FileAnalysisException` (specifically a `java.lang.IllegalArgumentException: source must not be null`) while analysing `ClassDetailActivity.kt` at line 47. This is likely a bug in the Kotlin K2 compiler related to type inference or anonymous class generation for `registerForActivityResult`.

## Proposed Changes

### `app` module

#### [MODIFY] [ClassDetailActivity.kt](file:///C:/Users/kunal%20kumar/AndroidStudioProjects/ClassroomConnect/app/src/main/java/com/example/classroomconnect/ClassDetailActivity.kt)

- Explicitly specify the type of `pdfPickerLauncher` to `ActivityResultLauncher<String>`.
- Explicitly specify the type of the lambda parameter `uri` to `Uri?`.
- If the crash persists, use an explicit `ActivityResultCallback` object instead of a trailing lambda.

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
- Run `./gradlew :app:compileDebugKotlin` to verify that the compiler crash is resolved.

### Manual Verification
- Deploy the app and verify that the "Choose PDF" functionality in `ClassDetailActivity` still works correctly.
