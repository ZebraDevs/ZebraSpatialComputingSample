package com.zebra.SpatialComputingFragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import com.zebra.spatialcomputingsample.R
import com.zebra.spatialcomputingsample.addBanner
import com.zebra.spatialcomputingsample.addNode
import com.zebra.spatialcomputingsample.createChildNodeAtOffset
import com.zebra.spatialcomputingsample.databinding.FragmentSpatialComputingBinding
import com.zebra.spatialcomputingsample.databinding.InputNumbersDialogBinding
import com.zebra.spatialcomputingsample.getTrackingStatus
import com.zebra.spatialcomputingsample.nearestVerticalPlaneHit
import com.zebra.spatialcomputingsample.rotate


/**
 * A simple [Fragment] subclass.
 * Use the [SpatialComputingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SpatialComputingFragment : Fragment() {

    private val TAG: String? = SpatialComputingFragment::class.simpleName
    private var selectedNode: Node? = null
    private var childNodes = arrayListOf<Node>()
    private lateinit var arSceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private lateinit var scene: Scene
    private lateinit var binding: FragmentSpatialComputingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSpatialComputingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arSceneView = arFragment.arSceneView
        scene = arSceneView.scene
        scene.addOnUpdateListener(this::onSceneUpdate)
        // Add a banner when the user touches the display
        arSceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (selectedNode == null) {
                    arSceneView.arFrame?.hitTest(event)
                        ?.minByOrNull { it.distance }
                        ?.addNode(scene)
                        ?.also { selectedNode = it }
                        ?.addBanner(
                            requireContext(),
                            text = "Hello \n 2023",
                            fontSize = 20
                        )
                    binding.buttonCreateChild.visibility = VISIBLE
                    binding.buttonRotateBanner.visibility = VISIBLE
                    binding.buttonClear.visibility = VISIBLE
                    binding.instructions.visibility = INVISIBLE
                }
            }
            true//consume the touch
        }
        //To illustrate rotation of a node around any of three axes
        binding.buttonRotateBanner.setOnClickListener {
            // Rotate the 3D coordinate node 45 degrees around the Y-axis each time button pressed
            selectedNode?.rotate(45f)
        }

        binding.buttonCreateChild.setOnClickListener {
            showInputDialog()
        }
        binding.buttonClear.setOnClickListener {
            clearScene()
            binding.buttonCreateChild.visibility = INVISIBLE
            binding.buttonRotateBanner.visibility = INVISIBLE
            binding.buttonClear.visibility = INVISIBLE
            binding.instructions.visibility = VISIBLE
        }
    }

    private fun showInputDialog() {
        val dialogBinding = InputNumbersDialogBinding.inflate(layoutInflater)
        dialogBinding.apply {
            val inputDialog = AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .setCancelable(true)
                .create()
            submitButton.setOnClickListener {
                val offset = Vector3(
                    numberXEditText.toFloat(),
                    numberYEditText.toFloat(),
                    numberZEditText.toFloat()
                )
                selectedNode?.createChildNodeAtOffset(requireContext(), offset, offset.toString())
                    ?.let { it1 -> childNodes.add(it1) }
                inputDialog.dismiss()
            }
            inputDialog.show()
        }
    }

    private fun clearScene() {
        childNodes.forEach { node ->
            node.setParent(null)
        }
        childNodes.clear()
        selectedNode?.setParent(null)
        selectedNode = null
    }
    private fun onSceneUpdate(updatedTime: FrameTime) {
        val tracking = arSceneView.getTrackingStatus()
        if (tracking.first?.equals(TrackingState.TRACKING) == true) {
            arSceneView.nearestVerticalPlaneHit()
        }
        if (tracking.first?.equals(TrackingState.PAUSED) == true) {
        }
        if (tracking.first?.equals(TrackingState.STOPPED) == true) {
        }
    }
    //Extension function to convert EditText to float default to zero
    private fun EditText.toFloat() = text.toString().toFloatOrNull() ?: 0f
}