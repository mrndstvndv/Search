package com.mrndstvndv.search.provider

import android.content.Context
import com.mrndstvndv.search.provider.apps.AppListProvider
import com.mrndstvndv.search.provider.calculator.CalculatorProvider
import com.mrndstvndv.search.provider.files.FileSearchProvider
import com.mrndstvndv.search.provider.files.FileSearchRepository
import com.mrndstvndv.search.provider.files.FileThumbnailRepository
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import com.mrndstvndv.search.provider.text.TextUtilitiesProvider
import com.mrndstvndv.search.provider.web.WebSearchProvider

class Providers(
    private val context: Context,
    private val settingsRepository: ProviderSettingsRepository,
    private val fileSearchRepository: FileSearchRepository,
    private val fileThumbnailRepository: FileThumbnailRepository
) {
    private val defaultAppIconSize by lazy { context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size) }

    fun get(): List<Provider> {
        return buildList {
            add(AppListProvider(context, defaultAppIconSize, settingsRepository))
            add(CalculatorProvider(context, settingsRepository))
            add(TextUtilitiesProvider(context, settingsRepository))
            add(FileSearchProvider(context, settingsRepository, fileSearchRepository, fileThumbnailRepository))
            add(WebSearchProvider(context, settingsRepository))
        }
    }
}
