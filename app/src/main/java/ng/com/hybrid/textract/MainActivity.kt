package ng.com.hybrid.textract

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

class MainActivity : AppCompatActivity() {

    internal lateinit var copy: Button
    internal lateinit var selecter: FloatingActionButton
    internal lateinit var imgpreview: ImageView
    internal lateinit var textresult: TextView
    internal var image_uri: Uri? = null

    internal lateinit var cameraPermission: Array<String>
    internal lateinit var storagePermission: Array<String>
    internal lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        copy = findViewById(R.id.copy)
        textresult = findViewById(R.id.textresult)
        imgpreview = findViewById(R.id.imgpreview)
        selecter = findViewById(R.id.fab)
        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)


        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        selecter.setOnClickListener { ShowImageDialog() }

        if (clipboardManager.hasPrimaryClip()) {

        }

        copy.setOnClickListener {
            val text = textresult.text.toString()

            if (text != "") {
                val clipData = ClipData.newPlainText("text", text)
                clipboardManager.primaryClip = clipData

                Toast.makeText(applicationContext, "Copied", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun ShowImageDialog() {

        val items = arrayOf("Camera ", "Gallery")
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Select Image")
        dialog.setItems(items) { dialog, which ->
            if (which == 0) {

                if (!checkCameraPermission()) {
                    requestCameraPermission()

                } else {
                    pickCamera()
                }
            }

            if (which == 1) {

                if (!checkStoragePermission()) {
                    requestStoragePermission()

                } else {
                    pickGallery()
                }
            }
        }
        dialog.create().show()
    }

    private fun pickGallery() {

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)

    }

    private fun pickCamera() {

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "NewPic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "NaijaCardLoader")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)
    }

    private fun requestStoragePermission() {

        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)

    }

    private fun checkStoragePermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)

    }

    private fun checkCameraPermission(): Boolean {

        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val result1 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        return result && result1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        when (requestCode) {

            CAMERA_REQUEST_CODE ->

                if (grantResults.size > 0) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera()
                    } else {
                        Toast.makeText(applicationContext, "permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            STORAGE_REQUEST_CODE ->

                if (grantResults.size > 0) {
                    val writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (writeStorageAccepted) {
                        pickGallery()
                    } else {
                        Toast.makeText(applicationContext, "permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                CropImage.activity(data!!.data)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this)
            }

            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(image_uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this)
            }

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                imgpreview.setImageURI(resultUri)

                val bitmapDrawable = imgpreview.drawable as BitmapDrawable
                val bitmap = bitmapDrawable.bitmap
                val recognizer = TextRecognizer.Builder(applicationContext).build()

                if (!recognizer.isOperational) {
                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                } else {
                    val frame = Frame.Builder().setBitmap(bitmap).build()
                    val items = recognizer.detect(frame)
                    val sb = StringBuilder()

                    for (i in 0 until items.size()) {
                        val myItem = items.valueAt(i)
                        sb.append(myItem.value)
                        sb.append("\n")

                    }
                    textresult.text = sb.toString()

                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                Toast.makeText(applicationContext, "" + error, Toast.LENGTH_SHORT).show()
            }

        }
    }

    companion object {

        private val CAMERA_REQUEST_CODE = 200
        private val STORAGE_REQUEST_CODE = 400
        private val IMAGE_PICK_GALLERY_CODE = 1000
        private val IMAGE_PICK_CAMERA_CODE = 1001
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}
