package com.zebra.ezpicklib.spatialnavigation.barcode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.annotation.Keep
import com.zebra.spatialcomputingsample.R
import java.util.IdentityHashMap
import kotlin.properties.Delegates

// Singleton object
@Keep
object ZBarcode {
    var mContext: Context? = null

    // Observers Delegate
    val pBarcodeObservers = IdentityHashMap<Any, (Triple<String, String, Boolean>) -> Unit>()

    // Observable Delegate - The delegate returns a callback function that executes when a given property is read or written.
    var mNewestBarcode: Triple<String, String, Boolean> by Delegates.observable(
        Triple("", "", false)
    ) { _, _, newValue ->
        pBarcodeObservers.values.forEach { it(newValue) }
    }
    private var isSoftScan: Boolean = false

    fun init(context: Context) {
        mContext = context
        createDWProfile()
    }

    private fun createDWProfile() {
        // MAIN BUNDLE PROPERTIES
        val bMain = Bundle()
        bMain.putString("PROFILE_NAME", mContext?.getString(R.string.datawedge_profile_name))
        bMain.putString("PROFILE_ENABLED", "true") // <- that will be enabled
        bMain.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST") // <- or created if necessary.

        // PLUGIN_CONFIG BUNDLE PROPERTIES
        val bConfig = Bundle()
        bConfig.putString("PLUGIN_NAME", "INTENT")
        bConfig.putString("RESET_CONFIG", "false")

        // PARAM_LIST BUNDLE PROPERTIES
        val bParams = Bundle()
        bParams.putString("scanner_selection", "auto")
        bParams.putString("scanner_input_enabled", "true")
        bParams.putString("intent_output_enabled", "true")
        bParams.putString(
            "intent_action",
            mContext?.getString(R.string.activity_intent_filter_action)
        )
        bParams.putString("intent_category", "android.intent.category.DEFAULT")
        bParams.putString("intent_delivery", "2")
        bParams.putString("keystroke_output_enabled", "false")

        // NEST THE BUNDLE "bParams" WITHIN THE BUNDLE "bConfig"
        bConfig.putBundle("PARAM_LIST", bParams)

        // THEN NEST THE "bConfig" BUNDLE WITHIN THE MAIN BUNDLE "bMain"
        bMain.putBundle("PLUGIN_CONFIG", bConfig)

        // CREATE APP_LIST BUNDLES (apps and/or activities to be associated with the Profile)
        val bundleApp1 = Bundle()
        bundleApp1.putString("PACKAGE_NAME", mContext?.packageName)
        bundleApp1.putStringArray("ACTIVITY_LIST", arrayOf("*"))

        // NEXT APP_LIST BUNDLE(S) INTO THE MAIN BUNDLE
        bMain.putParcelableArray("APP_LIST", arrayOf(bundleApp1))
        var i = Intent()
        i.action = "com.symbol.datawedge.api.ACTION"
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain)
        mContext?.sendBroadcast(i)

        // Enable Scanning Status Intent Notification
        val bNotification = Bundle()
        bNotification.putString("com.symbol.datawedge.api.APPLICATION_NAME", mContext?.packageName)
        bNotification.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS")
        i = Intent()
        i.action = "com.symbol.datawedge.api.ACTION"
        i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", bNotification)
        mContext?.sendBroadcast(i)
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(mContext?.getString(R.string.activity_intent_filter_action))
        // Enable this only if you want to catch all notifications from DataWedge
        // filter.addAction("com.symbol.datawedge.api.NOTIFICATION_ACTION");
        mContext?.registerReceiver(myBroadcastReceiver, filter)
    }

    fun softScan() {
        // define action and data strings
        isSoftScan = true
        val softScanTrigger = "com.symbol.datawedge.api.ACTION"
        val extraData = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"
        // create the intent
        val i = Intent()
        // set the action to perform
        i.action = softScanTrigger
        // add additional info
        i.putExtra(extraData, "START_SCANNING")
        // send the intent to DataWedge
        mContext?.sendBroadcast(i)
    }

    private val myBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val b = intent.extras
            if (action == context.getString(R.string.activity_intent_filter_action)) {
                //  Received a barcode scan
                try {
                    var decodedSource =
                        intent.getStringExtra(context.getString(R.string.datawedge_intent_key_source))
                    var decodedData =
                        intent.getStringExtra(context.getString(R.string.datawedge_intent_key_data))
                    var decodedLabelType =
                        intent.getStringExtra(context.getString(R.string.datawedge_intent_key_label_type))
                    if (null == decodedSource) {
                        decodedSource =
                            intent.getStringExtra(context.getString(R.string.datawedge_intent_key_source_legacy))
                        decodedData =
                            intent.getStringExtra(context.getString(R.string.datawedge_intent_key_data_legacy))
                        decodedLabelType =
                            intent.getStringExtra(context.getString(R.string.datawedge_intent_key_label_type_legacy))
                    }

                    if (decodedSource == "Camera") {
                        isSoftScan = true
                    }
                    // Set the new barcode value
                    mNewestBarcode = Triple(decodedData!!, decodedLabelType!!, isSoftScan)
                    isSoftScan = false
                } catch (e: Exception) {
                    //  Catch if the UI does not exist when we receive the broadcast...
                }
            }
        }
    }
}