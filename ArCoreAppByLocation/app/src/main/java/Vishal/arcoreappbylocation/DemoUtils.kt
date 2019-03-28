package Vishal.arcoreappbylocation


import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper

/** Static utility methods to simplify creating multiple demo activities.  */
object DemoUtils {
    private val TAG = "SceneformDemoUtils"
    /**
     * Creates and shows a Toast containing an error message. If there was an exception passed in it
     * will be appended to the toast. The error will also be written to the Log
     */
    fun displayError(
        context: Context, errorMsg: String, problem: Throwable?
    ) {
        val tag = context.javaClass.simpleName
        val toastText: String
        if (problem != null && problem.message != null) {
            Log.e(tag, errorMsg, problem)
            toastText = errorMsg + ": " + problem.message
        } else if (problem != null) {
            Log.e(tag, errorMsg, problem)
            toastText = errorMsg
        } else {
            Log.e(tag, errorMsg)
            toastText = errorMsg
        }

        Handler(Looper.getMainLooper())
            .post {
                val toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
    }

    /**
     * Creates an ARCore session. This checks for the CAMERA permission, and if granted, checks the
     * state of the ARCore installation. If there is a problem an exception is thrown. Care must be
     * taken to update the installRequested flag as needed to avoid an infinite checking loop. It
     * should be set to true if null is returned from this method, and called again when the
     * application is resumed.
     *
     * @param activity - the activity currently active.
     * @param installRequested - the indicator for ARCore that when checking the state of ARCore, if
     * an installation was already requested. This is true if this method previously returned
     * null. and the camera permission has been granted.
     */
    @Throws(UnavailableException::class)
    fun createArSession(activity: Activity, installRequested: Boolean): Session? {
        var session: Session? = null
        // if we have the camera permission, create the session
        if (ARLocationPermissionHelper.hasPermission(activity)) {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return null
                ArCoreApk.InstallStatus.INSTALLED -> {
                }
            }
            session = Session(activity)
            // IMPORTANT!!!  ArSceneView needs to use the non-blocking update mode.
            val config = Config(session)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            session.configure(config)
        }
        return session
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    /*public static void requestPermission(Activity activity, int requestCode) {
    ActivityCompat.requestPermissions(
        activity, new String[] {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
  }

  */
    /** Check to see we have the necessary permissions for this app.  *//*
  public static boolean hasPermission(Activity activity) {
    return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED;
  }
  */
    /** Check to see if we need to show the rationale for this permission.  *//*
  public static boolean shouldShowRequestPermissionRationale(Activity activity) {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.CAMERA);
  }

  */
    /** Launch Application Setting to grant permission.  *//*
  public static void launchPermissionSettings(Activity activity) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
    activity.startActivity(intent);
  }*/

    fun handleSessionException(
        activity: Activity, sessionException: UnavailableException
    ) {

        val message: String
        if (sessionException is UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
        } else if (sessionException is UnavailableApkTooOldException) {
            message = "Please update ARCore"
        } else if (sessionException is UnavailableSdkTooOldException) {
            message = "Please update this app"
        } else if (sessionException is UnavailableDeviceNotCompatibleException) {
            message = "This device does not support AR"
        } else {
            message = "Failed to create AR session"
            Log.e(TAG, "Exception: $sessionException")
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }
}
