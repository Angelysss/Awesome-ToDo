package com.awesometodo.app

import android.app.Application
import com.awesometodo.app.data.AppDatabase
import com.awesometodo.app.data.AppRepository
import com.awesometodo.app.data.SettingsRepository

class AwesomeTodoApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { AppRepository(database) }
    val settingsRepository by lazy { SettingsRepository(this) }
}
