import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bedir.yanki.ui.TestScreen
import com.bedir.yanki.ui.permissions.PermissionHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // YankiTheme senin tema adın olmalı
            MaterialTheme {
                PermissionHandler {
                    // İzinler alındığında Test ekranını göster
                    TestScreen()
                }
            }
        }
    }
}