package com.henriksineksamen.wikilocal.controller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.toolbox.Volley
import com.henriksineksamen.wikilocal.R
import com.henriksineksamen.wikilocal.model.database.Article
import com.henriksineksamen.wikilocal.model.database.ArticleModel
import com.squareup.picasso.Picasso
import android.view.ViewManager
import com.android.volley.RequestQueue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.henriksineksamen.wikilocal.model.DataRequests
import kotlinx.android.synthetic.main.activity_article.*
import org.json.JSONObject
class ArticleActivity : AppCompatActivity() {

    /****************************************** GLOBAL VARIABLES *************************************************/
    private var title = ""
    private var image: String? = null
    private var text = ""
    private var description = ""
    private lateinit var requestQueue: RequestQueue
    private lateinit var request: DataRequests
    private var requestTag = ""

    private var isSaved = false
    private var hasLooked = false
    private lateinit var articleModel: ArticleModel
    private lateinit var articleTextView: TextView

    /****************************************** LIFE CYCLE *************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        //Get content from intent
        title = intent.getStringExtra("title")
        image = intent.getStringExtra("image")
        description = intent.getStringExtra("description")

        //Set listeners
        val faveButton = add_favorite
        faveButton.setOnClickListener {
            updateFavoriteIcon()
            if (isSaved) addArticle() else removeSavedArticle()
        }

        //Access content
        requestQueue = Volley.newRequestQueue(this)
        articleModel = ViewModelProviders.of(this).get(ArticleModel::class.java)
        request = DataRequests(requestQueue)
        requestTag = request.getArticleTag()
        request.requestArticle(title)

        requestQueue.addRequestFinishedListener<JSONObject> {
            if (it.tag == requestTag){
                articleTextView.text = HtmlCompat.fromHtml(request.getArticleContent(), 0)
            }
        }

        //create add
        MobileAds.initialize(this, "ca-app-pub-9638675442193636~7851936375")
        val mAdView = adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        lookIfArticleIsSavedAlready()
        displayArticle()
    }

    override fun onStop() {
        super.onStop()
        requestQueue.cancelAll(requestTag)
        articleModel.allArticles.removeObservers(this)
        finish()
    }

    /****************************************** DATABASE CALLS *************************************************/
    private fun lookIfArticleIsSavedAlready() {
        articleModel.allArticles.observe(this, Observer { articles ->
            articles.forEach { article ->
                if (!isSaved && !hasLooked) {
                    if (article.title == title) {
                        updateFavoriteIcon()
                        hasLooked = true
                    }
                } else {
                    return@Observer
                }
            }
        })
    }

    private fun addArticle() {
        var shouldAddArticle = true
        val newArticle = Article(title, image ?: "", description, text)
        articleModel.allArticles.observe(this, Observer {articles->
            articles.forEach { article ->
                if (article.title == newArticle.title){
                    shouldAddArticle = false
                }
            }
            if (shouldAddArticle) {
                articleModel.insert(newArticle)
            }
        })
    }

    private fun removeSavedArticle() {
        articleModel.allArticles.observe(this, Observer { articleList ->
            articleList.forEach { article ->
                if (article.title == title){
                    articleModel.delete(article)
                    return@Observer
                }
            }
        })
    }

    /****************************************** DISPLAY CONTENT *************************************************/
    private fun updateFavoriteIcon() {
        isSaved = !isSaved
        val faveButton = add_favorite
        faveButton.setImageResource(
            when (isSaved) {
                true -> R.drawable.ic_favorite_40dp
                false -> R.drawable.ic_favorite_border_40dp
            }
        )
    }

    private fun displayArticle() {
        articleTextView = htmlToTextView

        if (intent.getStringExtra("text") != null) {
            text = intent.getStringExtra("text")
        } else {
            text = "<h2>There seems to be a problem with the site </h2>"
            request.requestArticle(title)
        }
        articleTextView.text = HtmlCompat.fromHtml(text, 0)

        title_text.text = title
        val imageView = top_image

        if (image != null) {
            Picasso.with(this)
                .load(image)
                .into(imageView)

        } else {
            val parent = imageView.parent as ViewManager
            parent.removeView(imageView)
        }
    }
}
