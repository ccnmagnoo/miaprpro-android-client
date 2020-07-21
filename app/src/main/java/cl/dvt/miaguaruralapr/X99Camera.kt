package cl.dvt.miaguaruralapr

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.FileNotFoundException
import java.io.IOException

class Camera(
    private val mDialogView:View
){
    private var imageUri: Uri? = null /* el archivo a suber requiere un image URI */
    private val captureCODE = 1001

}
