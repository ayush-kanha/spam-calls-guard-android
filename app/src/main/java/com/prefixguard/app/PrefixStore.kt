package com.prefixguard.app

import android.content.Context

object PrefixStore {
    private const val PREFS = "prefix_guard_settings"
    private const val KEY_PREFIXES = "prefixes"

    fun get(context: Context): Set<String> = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_PREFIXES, emptySet())
        ?.toSet() ?: emptySet()

    fun save(context: Context, prefixes: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_PREFIXES, prefixes.toSet()).apply()
    }
}
