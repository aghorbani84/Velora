## Summary of Changes

- **Work Package / Issue:** 
- **Architectural Layer(s) Touched:** (`ui` / `orchestrator` / `domain` / `data`)

## Mandatory Quality Gate Checklist (`CONTRIBUTING.md`)

- [ ] `./gradlew detekt` passes with **0 issues**.
- [ ] `./gradlew :app:testDebugUnitTest` passes across all unit & Robolectric suites (`DspBiquadMathTest`, `DownloadValidatorSecurityTest`, `ExampleUnitTest`, `ExampleRobolectricTest`).
- [ ] `./gradlew assembleDebug` compiles cleanly without warnings.
- [ ] **No Silent Failures:** Exceptions are surfaced via `PlaybackStatus.ERROR` or `VeloraMediaEvent.ErrorRaised`.
- [ ] **No Fake Functionality:** Every new UI element is wired to a real domain/data implementation and includes `Modifier.testTag("snake_case_tag")`.
- [ ] **Bilingual i18n (EN + FA):** All user-facing strings exist in both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-fa/strings.xml` with RTL layout compatibility.
- [ ] **Security & Secrets:** No `.jks`, `.keystore`, `.base64`, `local.properties`, or API keys are included in the diff.
- [ ] `CHANGELOG.md` has been updated under the current version section.
