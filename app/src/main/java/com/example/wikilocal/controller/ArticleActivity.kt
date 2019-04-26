package com.example.wikilocal.controller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.toolbox.Volley
import com.example.wikilocal.R
import com.example.wikilocal.model.database.Article
import com.example.wikilocal.model.database.ArticleModel
import com.squareup.picasso.Picasso
import android.view.ViewManager
import com.android.volley.RequestQueue
import com.example.wikilocal.model.DataRequests
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
        val faveButton = findViewById<ImageButton>(R.id.add_favorite)
        faveButton.setOnClickListener {
            if (!isSaved) addArticle() else removeSavedArticle()
            updateFavoriteIcon()
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

        lookIfArticleIsSavedAlready();
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
                if (article.title == title){
                    updateFavoriteIcon()
                    return@Observer
                }
            }
        })
    }

    private fun addArticle(){
        var shouldAddArticle = true
        val newArticle = Article(title, image ?: "", description, text)
        val articleList = articleModel.allArticles.value
        articleList?.forEach {article ->
            if (article.title == newArticle.title){
                shouldAddArticle = false
            }
        }
        if (shouldAddArticle) articleModel.insert(newArticle)
    }

    private fun removeSavedArticle(){
        articleModel.allArticles.observe(this, Observer { articleList ->
            articleList.forEach {article ->
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
        val faveButton = findViewById<ImageView>(R.id.add_favorite)
        faveButton.setImageResource(
            when (isSaved) {
                true -> R.drawable.ic_favorite_40dp
                false -> R.drawable.ic_favorite_border_40dp
            }
        )
    }

    private fun displayArticle() {
        articleTextView = findViewById(R.id.htmlToTextView)

        if (intent.getStringExtra("text") != null) {
            text = intent.getStringExtra("text")
        } else {
            text = "<h2>There seems to be a problem with the site </h2>"
            request.requestArticle(title)
        }
        articleTextView.text = HtmlCompat.fromHtml(text, 0)

        findViewById<TextView>(R.id.title_text).text = title
        val imageView = findViewById<ImageView>(R.id.top_image)

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
