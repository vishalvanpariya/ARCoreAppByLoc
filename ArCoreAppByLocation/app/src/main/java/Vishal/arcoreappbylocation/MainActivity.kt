package Vishal.arcoreappbylocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.android.synthetic.main.activity_main.*
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.rendering.LocationNode
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException


class MainActivity : AppCompatActivity() {


    private lateinit var andyRenderable: ModelRenderable
    private var locationScene: LocationScene? = null
    private var installRequested: Boolean = false
    private var hasFinishedLoading = false
    private var isseted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val andy = ModelRenderable.builder()
            .setSource(this, R.raw.model)
            .build()

        CompletableFuture.allOf(andy).handle<Any> { notUsed, throwable ->
                if (throwable != null) {
                    DemoUtils.displayError(this, "1 Unable to load renderables", throwable)
                    return@handle null
                }

                try {
                    andyRenderable = andy.get()
                    hasFinishedLoading = true
                    Toast.makeText(this,"model loaded",Toast.LENGTH_SHORT).show()
                } catch (ex: InterruptedException) {
                    DemoUtils.displayError(this, "2 Unable to load renderables", ex)
                } catch (ex: ExecutionException) {
                    DemoUtils.displayError(this, "3 Unable to load renderables", ex)
                }
                null
            }


        arSceneView
            .scene
            .setOnUpdateListener { frameTime ->
                if (!hasFinishedLoading){
                    return@setOnUpdateListener
                }
                if(locationScene == null){
                    locationScene = LocationScene(this, this, arSceneView)


                    //if need to use current location than use this
                    val location = getloc()
                    if (location == null){
                        Toast.makeText(applicationContext,"Loaction is null Please try again",Toast.LENGTH_LONG).show()
                        return@setOnUpdateListener
                    }
                    else if(location.longitude==null || location.latitude==null){
                        Toast.makeText(applicationContext,"Loaction is null Please try again",Toast.LENGTH_LONG).show()
                        return@setOnUpdateListener
                    }
                    else{
                        Toast.makeText(applicationContext,"${location.longitude}   ${location.latitude}",Toast.LENGTH_LONG).show()
                    }
                    val layoutLocationMarker = LocationMarker(
                        location!!.longitude,
                        location.latitude,
                        getAndy()
                    )

                    // If u has location than use this
                    /*
                    val layoutLocationMarker = LocationMarker(
                        72.93777731470215,
                        20.947453786000686,
                        getAndy()
                    )
                    */

                    layoutLocationMarker.setRenderEvent { LocationNode ->
                        if (!isseted) {
                            isseted = true
                            Toast.makeText(applicationContext, "Object is seted in plane", Toast.LENGTH_LONG).show()
                        }
                    }

                    locationScene!!.mLocationMarkers.add(layoutLocationMarker)
                }

                val frame = arSceneView.arFrame
                if (frame == null) {
                    Toast.makeText(applicationContext,"fram is null",Toast.LENGTH_LONG).show()
                    return@setOnUpdateListener
                }

                if (locationScene != null) {
                    locationScene!!.processFrame(frame)
                }
            }

        ARLocationPermissionHelper.requestPermission(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this)
            } else {
                Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    private fun getAndy(): Node? {
        val base = Node()
        base.renderable = andyRenderable
        val c = this
        base.setOnTapListener { v, event ->
            Toast.makeText(
                c, "Andy touched.", Toast.LENGTH_LONG
            )
                .show()
        }
        base.localPosition = Vector3(0f,0f,0f)
        return base
    }

    public override fun onPause() {
        super.onPause()

        if (locationScene != null) {
            locationScene!!.pause()
        }

        arSceneView.pause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        arSceneView.destroy()
    }



    override fun onResume() {
        super.onResume()

        if (locationScene != null) {
            locationScene!!.resume()
        }

        if (arSceneView.session == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = DemoUtils.createArSession(this, installRequested)
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this)
                    return
                } else {
                    arSceneView.setupSession(session)
                }
            } catch (e: UnavailableException) {
                DemoUtils.handleSessionException(this, e)
            }

        }

        try {
            arSceneView.resume()
        } catch (ex: CameraNotAvailableException) {
            DemoUtils.displayError(this, "Unable to get camera", ex)
            finish()
            return
        }


    }

    @SuppressLint("MissingPermission")
    fun getloc(): Location? {
        var location: Location? = null
        val locationManager: LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        locationManager.getBestProvider(criteria, true)
        location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true))
        return location
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window
                .decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
