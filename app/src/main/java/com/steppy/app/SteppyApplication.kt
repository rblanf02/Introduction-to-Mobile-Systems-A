package com.steppy.app

import android.app.Application
import com.steppy.app.di.AppContainer
import com.steppy.app.platform.work.StepSnapshotWorker

class SteppyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        StepSnapshotWorker.schedule(this)
    }
}
