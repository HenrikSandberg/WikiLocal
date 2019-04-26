package com.example.wikilocal.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "article_table")
data class Article (
    @PrimaryKey @ColumnInfo (name = "title")
    val title:String,

    @ColumnInfo (name = "image")
    var image:String,

    @ColumnInfo (name = "description")
    var description:String,

    @ColumnInfo (name = "text")
    var text:String
)