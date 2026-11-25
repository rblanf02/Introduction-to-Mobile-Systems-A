package com.steppy.app

import android.app.Application
import com.steppy.app.di.AppContainer

class SteppyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
