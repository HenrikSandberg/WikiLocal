package com.example.wikilocal.model.database
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
class ArticleModel(application: Application): AndroidViewModel(application){
    private val repository: ArticleRepository
    val allArticles: LiveData<List<Article>>

    private var parentJob = Job()
    private val coRoutineContext: CoroutineContext get() = parentJob + Dispatchers.Main
    private val scope = CoroutineScope(coRoutineContext)

    init {
        val articleDAO = AppDatabase.getDatabase(application.applicationContext).getArticleDAO()
        repository = ArticleRepository(articleDAO)
        allArticles = repository.allArticlesLive
    }

    fun insert(article: Article) = scope.launch(Dispatchers.IO) { repository.insert(article) }
    fun delete(article: Article) = scope.launch(Dispatchers.IO) { repository.delete(article) }
}