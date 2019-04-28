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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.henriksineksamen.wikilocal.R
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import com.henriksineksamen.wikilocal.model.DataRequests
import com.henriksineksamen.wikilocal.model.ViewPagerAdapter
import com.henriksineksamen.wikilocal.model.database.Article
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.DateFormat.getDateInstance
import java.util.*

class MainActivity : AppCompatActivity(),
    NearYouFragment.OnNearYouFragmentInteractionListener,
    SavedArticleFragment.OnListFragmentInteractionListener {

    /****************************************** GLOBAL VARIABLES *************************************************/
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val requestPermissionLocation = 100

    private lateinit var requestQueue: RequestQueue
    private lateinit var request: DataRequests

    private var latitude: Double? = null
    private var longitude:Double? = null
    private var lookForLandMark = false
    private var fireLat:Double = 0.0
    private var fireLon:Double = 0.0
    private var articles: MutableList<JSONObject> = mutableListOf()

    private val REQUEST_IMAGE_CAPTURE = 1
    private var currentPhotoPath: String? = null

    private lateinit var viewPager:ViewPager
    private var nearYouFragment: NearYouFragment? = null
    private var savedArticleFragment: SavedArticleFragment? = null


    /****************************************** LIFE CYCLE *************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Elements on screen
        viewPager = findViewById(R.id.viewpager)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tabLayout = findViewById<TabLayout>(R.id.tabs)

        //Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        tabLayout.setupWithViewPager(viewPager)
        setupViewPager()

        //Set Up location Manager
        locationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { noGpsAlert() }
        if (checkPermission(this)){ updateLocation() }

        //Camera button pressed
        findViewById<FloatingActionButton>(R.id.photo_button).setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    override fun onStart() {
        super.onStart()
        //Activate listener for HTTP Requests
        requestQueue = Volley.newRequestQueue(this)
        requestQueue.addRequestFinishedListener<JSONObject> {
            if (it.tag == request.articlesTag()) {
                articles = request.getArticles()
                updateList()
            }
            if (it.tag == request.landmarkRequestTag() && lookForLandMark) {
                lookForLandMark = false
                onNearYouFragmentInteraction(request.getArticles().first())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        requestQueue.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        articles.clear()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        requestQueue.stop()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ( requestCode == REQUEST_IMAGE_CAPTURE ) {
            val bmOptions = BitmapFactory.Options().apply {
                BitmapFactory.decodeFile(currentPhotoPath, this)
            }
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap -> recognizeImage(bitmap) }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /****************************************** RECOGNIZE LANDMARK *************************************************/
    private fun recognizeImage(bitmap: Bitmap) {
        val options = FirebaseVisionCloudDetectorOptions.Builder()
            .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
            .setMaxResults(5)
            .build()
        val detector = FirebaseVision.getInstance().getVisionCloudLandmarkDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        var bestConfidence:Float? = null

        Toast.makeText(applicationContext, "Looking for article", Toast.LENGTH_SHORT).show()
        with( detector.detectInImage(image) ) {
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
                    lookForLandMark = true
                    request = DataRequests(requestQueue)
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
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
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
        override fun onLocationResult(locationResult: LocationResult) {
            updateContent(locationResult.lastLocation)
        }
    }

    private fun updateLocation(){
        with (locationRequest){
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 1000000L
            fastestInterval = 1000000L
        }

        LocationServices.getSettingsClient(this)
            .checkLocationSettings(
                LocationSettingsRequest
                    .Builder()
                    .addLocationRequest(locationRequest)
                    .build()
            )
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission (this, Manifest.permission.ACCESS_FINE_LOCATION )
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission (
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private fun updateContent(location: Location?) {
        if (location != null) {
            latitude = location.latitude
            longitude = location.longitude

            if (latitude != null && longitude != null) {
                setTitleText()
                requestArticles(null)
            }
        }
    }

    private fun setTitleText(){
        val city = Geocoder (this, Locale.getDefault())
            .getFromLocation(latitude!!, longitude!!, 1)

        val text = when {
            city[0].locality != null -> city[0].locality
            city[0].adminArea != null -> city[0].adminArea
            else -> "Did not get title"
        }
        findViewById<TextView>(R.id.city_text).text = text
    }

    /***************************** PERMISSIONS HANDLING *************************** */
    private fun checkPermission(context: Context): Boolean {
        return if (context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestPermissionLocation)
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
    private fun noGpsAlert() {
        AlertDialog
            .Builder(this)
            .setMessage("GPS is off, please turn it on")
            .setCancelable(false)
            .setPositiveButton("yes") { _, _ ->
                startActivityForResult( Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11)
            }
            .setNegativeButton("no") { dialog, _ ->
                dialog.cancel()
                finish()
            }
            .create()
            .show()
    }

    private fun turnOnNetworking() {
        AlertDialog
            .Builder(this)
            .setMessage("Network is of, the app is useless without it. Please turn it back on")
            .setCancelable(false)
            .setPositiveButton("yes") { _, _ ->
                startActivityForResult( Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS), 12)
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
        if ((getSystemService(WIFI_SERVICE) as WifiManager).isWifiEnabled) {
            if (latitude != null && longitude != null){
                request = DataRequests(requestQueue)
                request.requestArticles(latitude!!, longitude!!, tag)
            } else {
                noGpsAlert()
            }
        } else {
            turnOnNetworking()
        }
    }

    /********************************* PAGE CONTENT HANDLING ******************************/
    private fun setupViewPager() {
        if (nearYouFragment == null) {
            nearYouFragment = NearYouFragment()
            nearYouFragment!!.updateList(articles)
        } else updateList()

        if (savedArticleFragment == null) {
            savedArticleFragment = SavedArticleFragment()
        }

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(nearYouFragment!!, "Nær deg")
        adapter.addFragment(savedArticleFragment!!, "Lagret")
        viewPager.adapter = adapter
    }

    private fun updateList() {
        if (nearYouFragment != null) {
            with (nearYouFragment!!) {
                updateList(articles)
                refersRecyclerView()
                removeUpdaterIcon()
            }
        } else setupViewPager()
    }

    /************************************ GO TO ACTIVITY ***************************************/
    override fun onNearYouFragmentInteraction(article: JSONObject) {
        if (!isFinishing) {
            with( Intent(this, ArticleActivity::class.java) ) {
                putExtra ("title",
                    if (article.has("displaytitle"))
                        article.getString("displaytitle")
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
    }

    override fun onSavedArticleFragmentInteraction(article: Article) {
        if (!isFinishing) {
            with (Intent(this, ArticleActivity::class.java)) {
                putExtra("title", article.title)
                putExtra("description", article.description)
                putExtra("image",  article.image)
                putExtra("text",  article.text)
                startActivity(this)
            }
        }
    }
}