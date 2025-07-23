package ru.vsu.arembroidery.di

import org.koin.dsl.module
import ru.vsu.arembroidery.data.MatrixRepository

val dataModule = module {
    single { MatrixRepository() }
}