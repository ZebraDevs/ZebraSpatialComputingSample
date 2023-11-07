package com.zebra.spatialcomputingsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import com.zebra.ezpicklib.spatialnavigation.barcode.ZBarcode
import com.zebra.spatialcomputingsample.backend.DatabaseRepository
import com.zebra.spatialcomputingsample.backend.Product
import com.zebra.spatialcomputingsample.databinding.FragmentWorkflowBinding
import java.util.EnumSet
import kotlin.math.pow
import kotlin.math.sqrt


class WorkflowFragment(val workflow: String) : Fragment() {

    private val TAG: String? = WorkflowFragment::class.simpleName
    private lateinit var binding: FragmentWorkflowBinding
    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private lateinit var scene: Scene
    private lateinit var databaseRepositoryInstance: DatabaseRepository
    private lateinit var sharedCameraSession: Session
    private var sectionNode: Node? = null
    private var sectionNodeData: String? = ""
    private var productNodeArrayList = arrayListOf<Product>()
    private var childNodes = arrayListOf<Node>()
    private var retrievedProductArrayList: ArrayList<Product>? = null
    private var foundPlane: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWorkflowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arSceneView = arFragment.arSceneView
        scene = arSceneView.scene
        sharedCameraSession = Session(context, EnumSet.of(Session.Feature.SHARED_CAMERA))
        setupPlaneFinding()

        scene.addOnUpdateListener(this::onSceneUpdate)
        sectionNode = null

        // Instantiate ZBarcode object that receives barcode updates.
        registerBarcodeReceiver()

        // Instantiate backend respository to save section and product information.
        databaseRepositoryInstance = context?.let { DatabaseRepository(it) }!!
        if (workflow.equals(getString(R.string.restore_planogram), ignoreCase = false)) {
            retrievedProductArrayList = databaseRepositoryInstance.readPlanogramFile()
        }
        binding.saveLocations.setOnClickListener {
            // Clear any existing data in the database
            databaseRepositoryInstance.deleteAllProducts()
            productNodeArrayList.forEach {
                databaseRepositoryInstance.insertProduct(it)
            }
            productNodeArrayList.clear()
            clearScene()
            setInfoMessage(getString(R.string.instruction_2))
            binding.saveLocations.visibility = INVISIBLE
            databaseRepositoryInstance.writePlanogramFile(sectionNodeData!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterBarcodeReceiver()
    }

    private fun setInfoMessage(message: String) {
        binding.instructions.text = message
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun registerBarcodeReceiver() {
        context?.let { ZBarcode.init(it) }
        ZBarcode.pBarcodeObservers.put(this) { scanOutput ->
            if (sectionNode == null) {
                val decodedData = scanOutput.first
                //Creation of a barcode banner at the intersection of the vertical plane
                //found by ARCore and the Zebra barcode scanner line of sight ie, the barcode position
                //in the real world.
                sectionNode = arSceneView.createBarcodeBanner(
                    requireContext(),
                    "Section: " + decodedData.takeLast(1),
                    true
                )
                if (sectionNode != null) {
                    sectionNodeData = decodedData
                    if (workflow.equals(
                            getString(R.string.restore_planogram),
                            ignoreCase = false
                        )
                    ) {
                        retrievedProductArrayList?.forEach {
                            sectionNode?.createChildNodeAtOffset(
                                requireContext(),
                                Vector3(it.xOffset, it.yOffset, 0f),
                                it.upc
                            )
                        }
                    } else {
                        setInfoMessage(getString(R.string.instruction_3))
                    }
                } else {
                    showToast(getString(R.string.instruction_4))
                }
            } else {
                val decodedData = scanOutput.first
                val barcodeNode = arSceneView.createBarcodeBanner(requireContext(), decodedData)
                if (barcodeNode != null) {
                    var xOffset = sqrt(
                        (barcodeNode.worldPosition.x - sectionNode!!.worldPosition.x).pow(2) + (barcodeNode.worldPosition.z - sectionNode!!.worldPosition.z).pow(
                            2
                        )
                    )
                    val xOffsetDiff = sectionNode!!.worldPosition.x - barcodeNode.worldPosition.x
                    if (xOffsetDiff >= 0) {
                        //Product barcode scan is found to the left Section QR Code. Hence negate the xOffset distance
                        xOffset *= -1.0f
                    }
                    val yOffset = barcodeNode.worldPosition.y - sectionNode!!.worldPosition.y
                    productNodeArrayList.add(
                        Product(
                            sectionKey = sectionNodeData!!,
                            locationId = 1,
                            upc = decodedData,
                            itemDescription = "itemDescription",
                            category = "category",
                            price = 399.99F,
                            quantityOnHand = 1,
                            xOffset = xOffset,
                            yOffset = yOffset
                        )
                    )
                    childNodes.add(barcodeNode)
                } else {
                    showToast(getString(R.string.instruction_4))
                }
                setInfoMessage("Save Realogram When Completed")
                binding.saveLocations.visibility = View.VISIBLE
            }
        }
    }

    private fun unregisterBarcodeReceiver() {
        // Remove self as observer when going out of scope:
        ZBarcode.pBarcodeObservers.remove(this)
    }

    private fun onSceneUpdate(updatedTime: FrameTime) {
        val tracking = arSceneView.getTrackingStatus()
        if (tracking.first?.equals(TrackingState.TRACKING) == true) {
            if (arSceneView.nearestVerticalPlaneHit() != null) {
                foundPlane = true
                setInfoMessage(getString(R.string.instruction_2))
            }
        }
        if (tracking.first?.equals(TrackingState.PAUSED) == true) {
            foundPlane = false
        }
        if (tracking.first?.equals(TrackingState.STOPPED) == true) {
            foundPlane = false
        }
    }

    private fun clearScene() {
        childNodes.forEach { node ->
            node.setParent(null)
        }
        childNodes.clear()
        sectionNode?.setParent(null)
        sectionNode = null
    }

    private fun setupPlaneFinding() {

        //Create the config
        val arConfig = Config(sharedCameraSession)

        //Pause the session | Not sure if this is required for modifying plane detection. I was using this for something else, try & modify at your end
        sharedCameraSession.pause()

        // Modify the plane finding mode
        arConfig.planeFindingMode = Config.PlaneFindingMode.VERTICAL
        arConfig.focusMode = Config.FocusMode.AUTO

        //Reinstate the session
        sharedCameraSession.resume()

        arConfig.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

        //Reconfigure the session
        sharedCameraSession.configure(arConfig)

        //Setup the session with ARSceneView | Very important
        arSceneView.setupSession(sharedCameraSession)
    }

}