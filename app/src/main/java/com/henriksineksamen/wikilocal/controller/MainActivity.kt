package com.henriksineksamen.wikilocal.controller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.henriksineksamen.wikilocal.R
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import com.henriksineksamen.wikilocal.model.DataRequests
import com.henriksineksamen.wikilocal.model.ViewPagerAdapter
import com.henriksineksamen.wikilocal.model.database.Article
import com.google.android.gms.location.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.DateFormat.getDateInstance
import java.util.*

class MainActivity : AppCompatActivity(),
    NearYouFragment.OnNearYouFragmentInteractionListener,
    SavedArticleFragment.OnListFragmentInteractionListener {

    /****************************************** GLOBAL VARIABLES *************************************************/
    //HTTP Requests
    private lateinit var requestQueue:RequestQueue
    private lateinit var request: DataRequests

    //Location
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var lookForLandmark = false
    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val requestPermissionLocation = 100

    //Image
    private val requestImage = 1
    private var currentPhotoPath: String? = null

    //Fragments
    private var nearYouFragment: NearYouFragment? = null
    private var savedArticleFragment: SavedArticleFragment? = null

    /****************************************** LIFE CYCLE *************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        tabs.setupWithViewPager(viewpager)
        setupViewPager()

        //Set Up location Manager
        locationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            alertUser("GPS is off, please turn it on", Settings.ACTION_LOCATION_SOURCE_SETTINGS, 11)
        }
        if (checkPermission(this)) { updateLocation() }

        //Camera button pressed
        photo_button.setOnClickListener { dispatchTakePictureIntent() }
    }

    override fun onStart() {
        super.onStart()
        //Activate listener for HTTP Requests
        requestQueue = Volley.newRequestQueue(this)
        request = DataRequests(requestQueue)
        requestQueue.addRequestFinishedListener<JSONObject> {//Update when location manager return
            if (it.tag == request.articlesTag()) {
                nearYouFragment?.updateList(request.getArticles())
            }
            if (it.tag == request.landmarkRequestTag() && lookForLandmark) { //When you click on the camera
                lookForLandmark = false
                onNearYouFragmentInteraction(request.getArticles().first())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        locationProvider.removeLocationUpdates(locationCallback)
        requestQueue.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == requestImage) {
            val bmOptions = BitmapFactory.Options().apply {
                BitmapFactory.decodeFile(currentPhotoPath, this)
            }
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
                recognizeImage(bitmap) //Activate the call to Firebase
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /****************************************** RECOGNIZE LANDMARK *************************************************/
    private fun recognizeImage(bitmap: Bitmap) {
        val options = FirebaseVisionCloudDetectorOptions.Builder()
            .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
            .setMaxResults(5)
            .build()
        val detector = FirebaseVision //Creates the landmark detector
            .getInstance()
            .getVisionCloudLandmarkDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        var bestConfidence: Float? = null
        var fireLat = 0.0
        var fireLon = 0.0

        Toast.makeText(applicationContext, "Looking for article", Toast.LENGTH_SHORT).show()
        with (detector.detectInImage(image)) {
            addOnSuccessListener { fireLandmarks ->
                if (fireLandmarks.size > 0) {
                    fireLandmarks.forEach { landmark ->
                        val confidence = landmark.confidence
                        if (bestConfidence == null || confidence >= bestConfidence!!) {
                            bestConfidence = confidence
                            landmark.locations.forEach { location ->
                                fireLat = location.latitude
                                fireLon = location.longitude
                            }
                        }
                    }
                    lookForLandmark = true
                    request.requestArticles(fireLat, fireLon, request.landmarkRequestTag())
                } else {
                    Toast.makeText(applicationContext, "Could not find landmark", Toast.LENGTH_SHORT).show()
                }
            }
            addOnFailureListener {
                println(it)
                Toast.makeText(applicationContext, "Could not find landmark", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /****************************************** PHOTO *************************************************/
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) { null }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(this, "com.mydomain.fileprovider", it )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, requestImage)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = getDateInstance()
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    /****************************************** LOCATION *************************************************/
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) { updateContent(result.lastLocation) }
    }

    private fun updateLocation() {
        with (locationRequest) {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 1000000L
            fastestInterval = 1000000L
        }

        LocationServices.getSettingsClient(this).checkLocationSettings(
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        )
        locationProvider = LocationServices.getFusedLocationProviderClient(this)

        if ( ActivityCompat.checkSelfPermission (this, Manifest.permission.ACCESS_FINE_LOCATION )
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission (this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        != PackageManager.PERMISSION_GRANTED) { return }

        locationProvider.requestLocationUpdates( locationRequest, locationCallback, Looper.myLooper() )
    }

    private fun updateContent( location: Location?) {
        if (location != null) {
            latitude = location.latitude
            longitude = location.longitude
            setTitleText()
            requestArticles(null)
        }
    }

    private fun setTitleText() {
        Geocoder (this, Locale.getDefault() )
            .getFromLocation(latitude!!, longitude!!, 1).first()
            .apply {
                city_text.text = when {
                    locality != null -> locality
                    adminArea != null -> adminArea
                    else -> "Did not get title"
            }
        }
    }

    /***************************** PERMISSIONS HANDLING *************************** */
    private fun checkPermission(context: Context): Boolean {
        return if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestPermissionLocation)
            false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionLocation && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocation()
        }
    }

    /***************************** HANDLE HARDWARE TURNED OFF *************************** */
    private fun alertUser(message: String, setting: String, requestCode: Int) {
        AlertDialog
            .Builder(this)
            .setMessage( message )
            .setCancelable(false)
            .setPositiveButton("yes") { _, _ ->
                startActivityForResult( Intent(setting), requestCode)
                requestArticles(null)
            }
            .setNegativeButton("no") { dialog, _ ->
                dialog.cancel()
                finish()
            }
            .create()
            .show()
    }

    /***************************** REQUEST DATA FROM WIKIPEDIA *****************************/
    fun requestArticles(tag: String?) {
        if ( (getSystemService(WIFI_SERVICE) as WifiManager).isWifiEnabled) {
            if (latitude != null && longitude != null) {
                request.requestArticles(latitude!!, longitude!!, tag)

            } else alertUser("GPS is off, please turn it on",
                Settings.ACTION_LOCATION_SOURCE_SETTINGS, 11)

        } else alertUser("Network is of, the app is useless without it. Please turn it back on",
            Settings.ACTION_NETWORK_OPERATOR_SETTINGS, 12 )
    }

    /********************************* PAGE CONTENT HANDLING ******************************/
    private fun setupViewPager() {
        if ( nearYouFragment == null ) {
            nearYouFragment = NearYouFragment()
        }

        if (savedArticleFragment == null) {
            savedArticleFragment = SavedArticleFragment()
        }

        with ( ViewPagerAdapter(supportFragmentManager)) {
            addFragment(nearYouFragment!!, "Nær deg")
            addFragment(savedArticleFragment!!, "Lagret")
            viewpager.adapter = this
        }
    }

    /************************************ GO TO ACTIVITY ***************************************/
    override fun onNearYouFragmentInteraction(article: JSONObject) {
        with ( Intent(this, ArticleActivity::class.java) ) {
            putExtra ("title",
                if (article.has("displaytitle")) article.getString("displaytitle")
                else "No title available"
            )
            putExtra ("description",
                when {
                    article.has("description") -> article.getString("description")
                    article.has("extract") -> article.getString("extract")
                    else -> "No description available"
                }
            )
            if (article.has("originalimage"))
                putExtra("image", article.getJSONObject("originalimage").getString("source"))
            startActivity(this)
        }
    }

    override fun onSavedArticleFragmentInteraction(article: Article) {
        with ( Intent(this, ArticleActivity::class.java) ) {
            putExtra("title", article.title)
            putExtra("description", article.description)
            putExtra("image",  article.image)
            putExtra("text",  article.text)
            startActivity(this)
        }
    }
}