package com.otimizaai.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Ponto de partida do app. O Hilt monta aqui o banco, os repositórios e as contas. */
@HiltAndroidApp
class OtimizaApp : Application()
