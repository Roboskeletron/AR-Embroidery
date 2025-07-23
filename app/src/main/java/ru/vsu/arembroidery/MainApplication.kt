package ru.vsu.arembroidery

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import ru.vsu.arembroidery.di.appModule
import ru.vsu.arembroidery.di.dataModule


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(
                appModule,
                dataModule
            )
        }
    }
}
