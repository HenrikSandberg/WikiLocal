package com.henriksineksamen.wikilocal.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.henriksineksamen.wikilocal.R
import com.henriksineksamen.wikilocal.model.NearYouRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_near_you_list.view.*
import org.json.JSONObject

class NearYouFragment : Fragment(), SensorEventListener {
    private var listener: OnNearYouFragmentInteractionListener? = null
    private var articles: MutableList<JSONObject> = mutableListOf()

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeContainer: SwipeRefreshLayout

    //Sensor
    private lateinit var sensorManager:SensorManager
    private var openArticleWithShake = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_near_you_list, container, false)
        recyclerView = view.recycler

        swipeContainer = view.swipeContainer
        swipeContainer.setOnRefreshListener {
            (activity as MainActivity).requestArticles(null)
        }
        swipeContainer.setColorSchemeResources (
            R.color.color_primary,
            R.color.spin_first_color,
            R.color.spin_second_color,
            R.color.spin_third_color,
            R.color.spin_fourth_color
        )

        sensorManager = view.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensor.also {sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)}

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNearYouFragmentInteractionListener) listener = context
    }

    override fun onResume() {
        super.onResume()
        openArticleWithShake = true //Make sure you can shake again.
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        sensorManager.unregisterListener(this)
    }

    //Update the recyclerView
    fun updateList (articles: MutableList<JSONObject>) {
        if (articles != recyclerView.adapter) {
            with (recyclerView) {
                layoutManager = LinearLayoutManager(context)
                adapter = NearYouRecyclerViewAdapter(articles, listener)
            }
            this.articles = articles
            swipeContainer.isRefreshing = false
        }
    }

    // Reacts when shake event happens. If the force is greater the 2.7 it will return a random article
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && openArticleWithShake) {
            val xAccess = event.values[0] / SensorManager.GRAVITY_EARTH
            val yAccess = event.values[1] / SensorManager.GRAVITY_EARTH
            val zAccess = event.values[2] / SensorManager.GRAVITY_EARTH
            val force = Math.sqrt(xAccess.toDouble() * xAccess + yAccess * yAccess + zAccess * zAccess).toFloat()

            if (force > 2.7f) {
                openArticleWithShake = false
                val randomArticle = articles[(0..articles.size).shuffled().first()]
                (activity as MainActivity).onNearYouFragmentInteraction(randomArticle)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {} //Needed to be implemented ut have no use for it

    interface OnNearYouFragmentInteractionListener {
        fun onNearYouFragmentInteraction(article: JSONObject)
    }
}
