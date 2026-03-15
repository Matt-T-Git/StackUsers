package com.stackusers

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class, annotated with @HiltAndroidApp to trigger Hilt's code gen
 * and set up dependency injection graph for the application
 */
@HiltAndroidApp
class StackUsersApplication : Application()
