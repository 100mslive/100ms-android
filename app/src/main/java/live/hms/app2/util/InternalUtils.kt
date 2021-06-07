package live.hms.app2.util

import live.hms.app2.BuildConfig
import live.hms.app2.ui.settings.SettingsFragment

fun getTokenEnvironmentFromInitEnvironment(environment: String) = when (environment) {
  SettingsFragment.ENV_QA -> "qa-in"
  SettingsFragment.ENV_PROD -> "prod-in"
  else -> "qa-in"
}

fun getTokenEndpoint(environment: String): String {
  val endpoint = BuildConfig.TOKEN_ENDPOINT
  return if (BuildConfig.INTERNAL) {
    endpoint.replace("{environment}", environment)
  } else endpoint
}
