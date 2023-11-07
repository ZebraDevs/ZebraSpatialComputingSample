package com.zebra.spatialcomputingsample.backend

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProduct(product: Product)

    @Query("SELECT * FROM Product")
    fun getAllProducts(): Array<Product>

    @Query("SELECT * FROM Product WHERE `sectionKey` = :sectionKey")
    fun getAllProductsInSection(sectionKey: String): Array<Product>

    @Query("SELECT * from Product WHERE `upc` = :upc")
    fun getProduct(upc: String): Product

    @Query("DELETE FROM Product")
    fun deleteAllProducts()

    @Delete
    fun deleteProduct(vararg product: Product)

    @Query("DELETE FROM Product WHERE `sectionKey` = :sectionKey")
    fun deleteAllProductsInSection(sectionKey: String)
}