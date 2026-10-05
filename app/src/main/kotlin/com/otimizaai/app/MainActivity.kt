package com.otimizaai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.otimizaai.app.ui.AppNavigation
import com.otimizaai.app.ui.theme.OtimizaTheme
import dagger.hilt.android.AndroidEntryPoint

/** A única tela "física" do app. Todas as telas são desenhadas dentro dela com Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtimizaTheme {
                AppNavigation()
            }
        }
    }
}
