package com.example.spotifyclone.di

import android.content.Context
import com.bumptech.glide.Glide
import com.example.spotifyclone.SpotifyApplication
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.internal.managers.ApplicationComponentManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    fun provideGlideInstance (
        @ApplicationContext context : Context
    ) = Glide.with (context)
}