package com.example.wikilocal.model

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject

class DataRequests(private val requestQueue: RequestQueue) {
    private val latLngTag = "jsonRequestTag"
    private val articleContentTag = "getArticleTag"
    private val contentTag = "contentTag"
    private var text = ""

    private lateinit var requestObject: StringRequest
    private var articles: MutableList<JSONObject> = mutableListOf()

    fun getArticles(): MutableList<JSONObject> { return articles }
    fun getLatLngTag():String { return latLngTag }
    fun articlesTag():String { return articleContentTag }
    fun getArticleTag():String { return contentTag; }
    fun getArticleContent():String {return text }

    fun requestArticles(latitude: Double, longitude: Double) {
        val radius = 5000
        val numberOfArticles = 20
        val url = ("https://no.wikipedia.org/w/api.php?" +
                "action=query&list=geosearch&" +
                "gscoord=$latitude%7C$longitude&" +
                "gsradius=$radius" +
                "&gslimit=$numberOfArticles&" +
                "format=json"
                )
        requestObject = StringRequest (
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                val articleJson = JSONObject(response)
                    .getJSONObject("query")
                    .getJSONArray("geosearch")
                requestArticles(articleJson)
            },
            Response.ErrorListener { println("Error: $it") }
        )
        requestObject.tag = latLngTag
        requestQueue.add(requestObject)
    }

    private fun requestArticles(json: JSONArray) {
        (0 until json.length()).forEach { position ->
            val url = ("https://no.wikipedia.org/api/rest_v1/page/summary/"
                    + json.getJSONObject(position).getString("title")
                    )
            articles.clear()
            val articleRequest = JsonObjectRequest (
                Request.Method.GET, url, null,
                Response.Listener { articles.add(it) },
                Response.ErrorListener { println("Error: $it") }
            )
            articleRequest.tag = articleContentTag
            requestQueue.add(articleRequest)
        }
    }

    fun requestArticle(title: String) {
        val url = (
                "https://no.wikipedia.org/api/" +
                "rest_v1/page/mobile-sections-lead/" +
                title.replace(' ', '_')
        )

        val articleRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener{ response ->
                text = response
                    .getJSONArray("sections")
                    .getJSONObject(0)
                    .getString("text")
                    .replace(Regex("<a.*?>"), "")
                    .replace(Regex("</a>"), "")
                    .replace(Regex("""\[.*?]"""), "")
                    .replace(Regex("<img.*?>"), "")
                    .replace(Regex("<figcaption>.*?</figcaption>"), "")
            },
            Response.ErrorListener { println("Error: $it") }
        )
        articleRequest.tag = contentTag
        requestQueue.add(articleRequest)
    }

}