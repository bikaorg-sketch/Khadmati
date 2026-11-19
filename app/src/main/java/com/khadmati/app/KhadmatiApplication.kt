package com.khadmati.app

import android.app.Application
import com.khadmati.app.di.ServiceLocator

class KhadmatiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
