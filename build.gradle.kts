// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.detekt)
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
  source.setFrom(
    files(
      "app/src/main/java",
      "app/src/test/java"
    )
  )
}
