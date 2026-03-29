import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit
) {
    val permissionsGranted = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionsGranted.value = true
            onPermissionsGranted()
        } else {
            // TODO: Kullanıcıya neden izne ihtiyacımız olduğunu açıklayan bir diyalog gösterilebilir.
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(YankiPermissions.REQUIRED_PERMISSIONS)
    }
}