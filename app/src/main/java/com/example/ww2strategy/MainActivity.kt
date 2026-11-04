package com.example.ww2strategy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ww2strategy.ui.BetaMenuScreen
import com.example.ww2strategy.ui.GameApp
import com.example.ww2strategy.ui.WW2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WW2Theme {
                var showGame by remember { mutableStateOf(false) }
                if (showGame) {
                    val vm: GameViewModel = viewModel()
                    GameApp(vm)
                } else {
                    BetaMenuScreen(onOpenGame = { showGame = true }, onCreateGame = { showGame = true })
                }
            }
        }
    }
}
