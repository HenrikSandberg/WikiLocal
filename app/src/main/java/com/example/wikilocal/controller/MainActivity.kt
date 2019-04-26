package com.example.wikilocal.controller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wikilocal.R
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import com.example.wikilocal.model.DataRequests
import com.example.wikilocal.model.ViewPagerAdapter
import com.example.wikilocal.model.database.Article
import com.google.android.gms.location.*
import org.json.JSONObject
import safety.com.br.android_shake_detector.core.ShakeDetector
import safety.com.br.android_shake_detector.core.ShakeOptions
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
    private var articles: MutableList<JSONObject> = mutableListOf()

    private var shakeDetector: ShakeDetector? = null

    private lateinit var viewPager:ViewPager
    private var nearYouFragment: NearYouFragment? = null
    private var savedArticleFragment: SavedArticleFragment? = null


    /****************************************** LIFE CYCLE *************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createShakeDetection()

        //Elements on screen
        viewPager = findViewById(R.id.viewpager)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tabLayout = findViewById<TabLayout>(R.id.tabs)

        //Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        tabLayout.setupWithViewPager(viewPager)
        setupViewPager()
    }

    override fun onStart() {
        super.onStart()

        if (!shakeDetector?.isRunning!!){
            createShakeDetection()
        }

        requestQueue = Volley.newRequestQueue(this)

        locationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { noGpsAlert() }
        if (checkPermission(this)){ updateLocation() }

        requestQueue.addRequestFinishedListener<JSONObject> {
            if (it.tag == request.articlesTag()){
                articles = request.getArticles()
                updateList()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        requestQueue.stop()
        shakeDetector?.stopShakeDetector(baseContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        articles.clear()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        requestQueue.stop()
        shakeDetector?.destroy(baseContext)
        finish()
    }

    private fun createShakeDetection() {
        val options = ShakeOptions()
            .background(true)
            .interval(1000)
            .shakeCount(2)
            .sensibility(2.0f)
        shakeDetector = ShakeDetector(options).start(this) { getRandomArticle() }
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

        LocationServices
            .getSettingsClient(this)
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
                Manifest.permission.ACCESS_COARSE_LOCATION)
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
                requestArticles()
            }
        }
    }

    private fun setTitleText(){
        val city = Geocoder (
            this,
            Locale.getDefault()
        ).getFromLocation(latitude!!, longitude!!, 1)

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
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestPermissionLocation)
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
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    11)
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
                startActivityForResult(
                    Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
                    12)
                requestArticles()
            }
            .setNegativeButton("no") { dialog, _ ->
                dialog.cancel()
                finish()
            }
            .create()
            .show()
    }

    /***************************** REQUEST DATA FROM WIKIPEDIA *****************************/
    fun requestArticles() {
        if ((getSystemService(WIFI_SERVICE) as WifiManager).isWifiEnabled) {
            if (latitude != null && longitude != null){
                request = DataRequests(requestQueue)
                request.requestArticles(latitude!!, longitude!!)
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
    private fun getRandomArticle() {
        if (!isFinishing) { goTo(articles.shuffled().first()) }
    }
    override fun onNearYouFragmentInteraction(article: JSONObject) {
        if (!isFinishing) { goTo(article) }
    }

    override fun onSavedArticleFragmentInteraction(article: Article) {
        if (!isFinishing) { goToSaved(article) }
    }

    private fun goTo(article: JSONObject) {
        with ( Intent(this, ArticleActivity::class.java) ) {
            putExtra("title",
                if (article.has("displaytitle"))
                    article.getString("displaytitle")
                else  "No title available"
            )

            putExtra("description",
                when {
                    article.has("description") -> article.getString("description")
                    article.has("extract") -> article.getString("extract")
                    else -> "No description available"
                }
            )

            if (article.has("originalimage"))
                putExtra("image", article.getJSONObject("originalimage").getString("source") )

            startActivity(this)
        }
    }

    private fun goToSaved(article: Article) {
        with (Intent(this, ArticleActivity::class.java)) {
            putExtra("title", article.title)
            putExtra("description", article.description)
            putExtra("image",  article.image)
            putExtra("text",  article.text)
            startActivity(this)
        }
    }
}