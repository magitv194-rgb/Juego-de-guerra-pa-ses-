package com.example.ww2strategy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ww2strategy.ui.GameApp
import com.example.ww2strategy.ui.WW2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WW2Theme {
                val vm: GameViewModel = viewModel()
                GameApp(vm)
            }
        }
    }
}
