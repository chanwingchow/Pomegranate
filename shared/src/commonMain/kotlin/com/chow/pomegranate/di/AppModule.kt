package com.chow.pomegranate.di

import com.chow.pomegranate.service.cet.api.CET
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.chow.pomegranate")
class AppModule {
    @Singleton
    fun cet(): CET = CET()
}