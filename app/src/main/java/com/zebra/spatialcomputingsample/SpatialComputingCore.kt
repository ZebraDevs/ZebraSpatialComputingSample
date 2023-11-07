package com.zebra.spatialcomputingsample

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable


fun Anchor.position() = Vector3(pose.tx(), pose.ty(), pose.tz())

/**
 * Adds a banner to the node. The banner can contain an image and/or text with a specified font size.
 *
 * @param context The Android Context used to inflate the banner's view.
 * @param image The optional Bitmap image to display in the banner.
 * @param text The optional text to display in the banner.
 * @param fontSize The font size for the text in the banner. Default value is 0.
 */
fun Node.addBanner(
    context: Context,
    image: Bitmap? = null,
    text: String? = null,
    fontSize: Int = 0,
    isSectionBanner: Boolean = false
): Node {
    // Build a ViewRenderable using the provided context and layout resource.
    val layoutId = if (isSectionBanner) {
        R.layout.section_banner_view_layout
    } else {
        R.layout.product_banner_view_layout
    }
    ViewRenderable.builder().setView(context, layoutId).build()
        .thenAccept { renderable ->
            // Once the ViewRenderable is built, set the text and image (if provided) in the banner view.
            text?.apply {
                // If text is provided, set it to the TextView with id "banner_line_1" in the banner view.
                renderable.view.findViewById<TextView>(R.id.banner_line_1).apply {
                    setText(text)
                    textSize = fontSize.toFloat() // Set the text size to the provided font size.
                }
                // If an image is provided, set it to the ImageView with id "odp_image" in the banner view.
                image?.apply {
                    renderable.view.findViewById<ImageView>(R.id.odp_image).apply {
                        setImageBitmap(image)
                    }
                }
            }
            // Set the built ViewRenderable as the renderable for this Node.
            this.renderable = renderable
        }
    return this
}

//To create a barcode banner at the intersection of the vertical plane
//found by ARCore and the Zebra barcode scanner line of sight ie, the barcode position
//in the real world.
fun ArSceneView.createBarcodeBanner(
    context: Context,
    barcode: String,
    isSectionBanner: Boolean = false
): Node? {
    val camera = scene.camera
    val scannerDirection = camera.up
    val scannerPosition = camera.worldPosition.add(Vector3(-0.02f, 0f, 0f))
    val scannerRay = Ray(scannerPosition, scannerDirection)
    val node = scannerRay.hitResult(this)
        ?.minByOrNull { it.distance }
        ?.addNode(scene)
        //just show the last 5 digits of the barcode to avoid making it too wide
        ?.addBanner(context, text = barcode, fontSize = 8, isSectionBanner = isSectionBanner)
    return node
}

/**
 * Creates a child node at the specified offset from the current node, displaying the offset as a text banner.
 *
 * @param context The Context object to access resources and services.
 * @param offset The Vector3 representing the offset position of the child node relative to the current node.
 * @return A Node representing the child node with the offset displayed as a text banner.
 */
fun Node.createChildNodeAtOffset(context: Context, offset: Vector3, message: String): Node {

    // Create a child node with a banner displaying the offset text.
    val child = Node()
    child.addBanner(context, text = message, fontSize = 8)

    // Set the current node as the parent of the child node.
    child.setParent(this)

    // Set the local position of the child node to the specified offset.
    child.localPosition = offset

    // Return the child node.
    return child
}


/**
 * Rotates the Node by a specified angle around the vertical (Y) axis.
 * @param angle The angle in degrees by which to rotate the Node around the Y-axis.
 */
fun Node.rotate(angle: Float) {
    // Obtain the current local rotation of the Node.
    val currentRotation = localRotation

    // Create a new Quaternion representing the rotation around the Y-axis with the given angle.
    val rotationAroundY = Quaternion.axisAngle(Vector3.up(), angle)

    // Multiply the current local rotation with the new rotation around the Y-axis.
    // This effectively combines the existing rotation with the new rotation.
    // The result is the updated local rotation of the Node.
    localRotation = Quaternion.multiply(currentRotation, rotationAroundY)
}

/**
 * Adds a new Node to the Scene based on the provided HitResult.
 * @param scene The AR Scene to which the new Node will be added.
 * @return The newly created Node added to the Scene.
 */
fun HitResult.addNode(scene: Scene): Node {
    // Create a new Node.
    val node = Node()

    // Set the AR Scene (Scene) as the parent of the new Node.
    node.setParent(scene)

    // Create an anchor at the hit position.
    val anchor = createAnchor()

    // Place the new Node at the hit position by setting its worldPosition to the anchor's position.
    node.worldPosition = anchor.position()

    // Detach the anchor. We don't need to keep the anchor around, but it may be modified if necessary.
    anchor.detach()

    // Depending on the type of trackable (e.g., Horizontal or Vertical Plane), adjust the orientation of the Node.
    when (trackable.javaClass) {
        // If the hit is on a Vertical Plane, align the Node with the Plane's orientation.
        Plane::class.java -> {
            val plane = trackable as Plane
            if (plane.type == Plane.Type.VERTICAL) {
                // If the Plane is vertical, set the Node's look direction to the negation of the Plane's yAxis.
                node.setLookDirection(plane.centerPose.yAxis.toVector3().negated())
            } else {
                // If the Plane is not vertical, set the Node's look direction to the Scene camera's forward direction.
                // while keeping the Node vertical (hence the .yaw below)
                node.setLookDirection(scene.camera.forward.yaw())
            }
        }
    }
    // Return the newly created Node, which is now added to the Scene.
    return node
}

//projects a Vector3 to the ground plane (ie extracts the yaw from the direction)
fun Vector3.yaw() = Vector3(x, 0f, z)

//Extension functions to go between FloatArray used in ARCore APIs and Vector3 used in Sceneform
fun FloatArray.toVector3() = Vector3(this[0], this[1], this[2])
fun Vector3.toFloatArray() = floatArrayOf(x, y, z)

// Extension function to add two Vector3 objects
fun Vector3.add(vector: Vector3) = Vector3(x + vector.x, y + vector.y, z + vector.z)

//function bridges the Ray from Sceneform to HitResult in ArCore.  HitTestResults in Sceneform
//are not usable here since they yield only Node hits, not hits to a trackable from the ray.
fun Ray.hitResult(arSceneView: ArSceneView): MutableList<HitResult>? =
    arSceneView.arFrame?.hitTest(origin.toFloatArray(), 0, direction.toFloatArray(), 0)

//function return the tracking status and in case of tracking loss, reason for tracking loss
fun ArSceneView.getTrackingStatus(): Pair<TrackingState?, TrackingFailureReason?> {
    return Pair(
        this.arFrame?.camera?.trackingState,
        this.arFrame?.camera?.trackingFailureReason
    )
}

//function returns HitResult to the nearest vertical plane
fun ArSceneView.nearestVerticalPlaneHit(): HitResult? {
    var hitResult: HitResult? = null
    if (this.arFrame!!.camera.trackingState == TrackingState.TRACKING) {
        var minDistance: Float = Float.MAX_VALUE
        //Point from center of the screen
        for (hit in this.arFrame!!.hitTest(this.width * 0.5f, this.height * 0.5f)) {
            val trackable = hit.trackable
            try {
                if (trackable is Plane) {
                    if (trackable.type == Plane.Type.VERTICAL && trackable.isPoseInPolygon(hit.hitPose)
                    ) {
                        val distance = hit.distance
                        if (distance < minDistance) {
                            minDistance = distance
                            hitResult = hit
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("SpatialComputingCore", "Hit test failure")
            }
        }
    } else {
        Log.d("SpatialComputingCore", "AR Core is not tracking")
    }
    return hitResult
}