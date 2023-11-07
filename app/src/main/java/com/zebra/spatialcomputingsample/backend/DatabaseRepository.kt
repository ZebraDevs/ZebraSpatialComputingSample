package com.zebra.spatialcomputingsample.backend

import android.content.Context
import android.util.JsonReader
import android.util.Log
import android.util.MalformedJsonException
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader


class DatabaseRepository(val mContext: Context) {
    private val TAG: String? = DatabaseRepository::class.simpleName
    private var mProductDao: ProductDao = DatabaseManager.getInstance(mContext)?.getProductDao()!!

    fun insertProduct(product: Product) {
        Log.i(TAG, "insertProduct invoked = " + product.toString())
        mProductDao.insertProduct(product = product)
    }

    fun getProduct(upc: String): Product {
        Log.i(TAG, "getProduct invoked for $upc")
        val product = mProductDao.getProduct(upc = upc)
        if (product != null) {
            return product
        } else {
            // Try prepend 0 at start and lookup again
            val product = mProductDao.getProduct(upc = "0" + upc)
            if (product != null) {
                return product
            }
            // There are chances we may receive 14 digit upc:
            if (upc.length == 14) {
                val product = mProductDao.getProduct(upc = upc.substring(2))
                if (product != null) {
                    return product
                }
            }
            return Product(upc = upc)
        }
    }

    fun deleteAllProducts() {
        Log.i(TAG, "deleteAllProducts invoked")
        mProductDao.deleteAllProducts()
    }

    fun getAllProducts(): Array<Product> {
        Log.i(TAG, "getAllProducts invoked")
        return mProductDao.getAllProducts()
    }

    fun writePlanogramFile(sectionName: String) {
        val productList = mProductDao.getAllProducts()

        val jsonarray = JSONArray()
        val jsonFile = JSONObject()

        productList.forEach {
            val jsonobject = JSONObject()
            jsonobject.put("category", it.category)
            jsonobject.put("locationId", it.locationId)
            jsonobject.put("upc", it.upc)
            jsonobject.put("itemDescription", it.itemDescription)
            jsonobject.put("price", it.price)
            jsonobject.put("quantityOnHand", it.quantityOnHand)
            jsonobject.put("xOffset", it.xOffset)
            jsonobject.put("yOffset", it.yOffset)
            jsonarray.put(jsonobject)
        }
        jsonFile.put(sectionName, jsonarray)
        jsonFile.put("sections", "1")
        writeToStorage("planogram.json", jsonFile.toString())
    }

    fun readPlanogramFile(): ArrayList<Product> {
        val sectionProductArrayList = arrayListOf<Product>()
        try {
            var file: File? = File(mContext.filesDir, "ExportProductsJson/planogram.json")
            var section = ""
            var price = 0f
            var category = ""
            var upc = ""
            var itemDescription = ""
            var locationId = 0L
            var xOffset = 0f
            var yOffset = 0f
            var quantityOnHand = 0
            try {
                JsonReader(InputStreamReader(FileInputStream(file))).use { reader ->
                    reader.isLenient = true
                    reader.beginObject()
                    while (reader.hasNext()) {
                        section = reader.nextName()
                        Log.d(TAG, "parser section $section")
                        if (section == "sections") reader.skipValue()
                        else {
                            val sectionName = "$section"
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "locationId" -> locationId = reader.nextDouble().toLong()
                                        "upc" -> upc = reader.nextString()
                                        "itemDescription" -> itemDescription = reader.nextString()
                                        "category" -> category = reader.nextString()
                                        "price" -> price = reader.nextDouble().toFloat()
                                        "quantityOnHand" -> quantityOnHand =
                                            reader.nextDouble().toInt()

                                        "xOffset" -> xOffset = reader.nextDouble().toFloat()
                                        "yOffset" -> yOffset = reader.nextDouble().toFloat()
                                        else -> reader.skipValue()
                                    }

                                }
                                reader.endObject()
                                val productR = Product(
                                    sectionKey = sectionName,
                                    locationId = locationId,
                                    upc = upc,
                                    itemDescription = itemDescription,
                                    category = category,
                                    price = price,
                                    quantityOnHand = quantityOnHand,
                                    xOffset = xOffset,
                                    yOffset = yOffset
                                )
                                Log.d(TAG, "room: $productR")
                                sectionProductArrayList.add(productR)

                            }
                            reader.endArray()
                        }
                    }
                    reader.endObject()
                }
            } catch (error: MalformedJsonException) {
                Log.e(TAG, "Parse error")
            }
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
        }
        return sectionProductArrayList
    }

    private fun writeToStorage(fileName: String, jsonContent: String) {
        val file = File(mContext.filesDir, "ExportProductsJson")
        if (!file.exists()) {
            file.mkdir()
        }
        try {
            var mFile = File(file, fileName)
            if(mFile.exists())
            {
                Log.e(TAG, "Exists")
                mFile.delete()
                mFile = File(file, fileName)
            }
            val writer = FileWriter(mFile)
            writer.append(jsonContent)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}