package com.university.marketplace

import android.app.Application
import com.university.marketplace.di.AppContainer
import com.university.marketplace.di.DefaultAppContainer

class MarketplaceApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}

