package com.zebra.spatialcomputingsample.backend

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "Product")
data class Product(
    @PrimaryKey @NonNull var upc: String = "N/A",
    var sectionKey : String = "N/A",
    var locationId: Long? = 0,
    var itemDescription: String = "N/A",
    var category: String = "N/A",
    var price: Float? = 0f,
    var quantityOnHand: Int? = 0,
    var xOffset: Float = 0.0F,
    var yOffset: Float = 0.0F
){
    @Ignore
    constructor() : this("0")
}
