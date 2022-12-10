/* Copyright 2022 The Tensorflow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package airost.teamrocket.posturedetect

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Process
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import airost.teamrocket.posturedetect.camera.CameraSource
import airost.teamrocket.posturedetect.data.Device
import airost.teamrocket.posturedetect.data.Camera
import airost.teamrocket.posturedetect.ml.ModelType
import airost.teamrocket.posturedetect.ml.MoveNet
import airost.teamrocket.posturedetect.ml.PoseClassifier
import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBar
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    /** Device: CPU、GPU、NNAPI（AI accelerator） */
    private var device = Device.CPU

    /** Camera：FRONT、BACK (Back only, front camera bug)*/
    private var selectedCamera = Camera.BACK

    /** Counters for posture state */
    private var hunchbackCounter = 0
    private var crosslegCounter = 0
    private var straightCounter = 0
    private var missingCounter = 0

    /** Default posture */
    private var poseRegister = "standard"

    /** Debug message that shows in the app (TextView) */
    private lateinit var tvDebug: TextView

    /** Shows current sitting posture (ImageView) */
    private lateinit var ivStatus: ImageView
    private lateinit var tvFPS: TextView
    private lateinit var tvScore: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnCamera: Spinner

    private lateinit var dialog: BottomSheetDialog

    private var cameraSource: CameraSource? = null
    private var isClassifyPose = true

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private var changeCameraListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, view: View?, direction: Int, id: Long) {
            changeCamera(direction)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            // do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // keep screen on while app is running

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)

        /** Debug messages */
        tvDebug = findViewById(R.id.tvDebug)

        /** Shows current posture */
        ivStatus = findViewById(R.id.ivStatus)

        tvFPS = findViewById(R.id.tvFps)
        spnDevice = findViewById(R.id.spnDevice)
        spnCamera = findViewById(R.id.spnCamera)
        surfaceView = findViewById(R.id.surfaceView)



        initSpinner()
        if (!isCameraPermissionGranted()) {
            requestPermission()
        }

        // Calling the support action bar and setting it to custom
        this.supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM

        // Displaying the custom layout in the ActionBar
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setCustomView(R.layout.my_layout)



    }

    private fun showToast(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
    }

    // method to inflate the options menu when
    // the user opens the menu for the first time
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.option_1 -> {
                Toast.makeText(this,"Change to Pose Detection", Toast.LENGTH_SHORT).show()
                item.isChecked = !item.isChecked
                return true
            }
            R.id.option_2 -> {
                Toast.makeText(this,"Change to Yoga Detection", Toast.LENGTH_SHORT).show()
                item.isChecked = !item.isChecked
                return true
            }
            R.id.text -> {
                val dialogView = layoutInflater.inflate(R.layout.bottom_sheet, null)
                dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
                dialog.setContentView(dialogView)
                dialog.show()
                return true
            }else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCamera() {

        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, selectedCamera, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {

                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)

                            /** Analyse target's posture, shows messages */
                            if (poseLabels != null && personScore != null && personScore > 0.3) {
                                missingCounter = 0
                                val sortedLabels = poseLabels.sortedByDescending { it.second }
                                when (sortedLabels[0].first) {
                                    "forwardhead" -> {
                                        crosslegCounter = 0
                                        straightCounter = 0
                                        if (poseRegister == "forwardhead") {
                                            hunchbackCounter++
                                        }
                                        poseRegister = "forwardhead"

                                        /** Shows current posture (hunchback) */
                                        if (hunchbackCounter > 60) {

                                            ivStatus.setImageResource(R.drawable.hunchback_confirm)
                                        } else if (hunchbackCounter > 30) {
                                            ivStatus.setImageResource(R.drawable.hunchback_warning)
                                        }

                                        /** Debug messages */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $hunchbackCounter")
                                    }
                                    "crossleg" -> {
                                        hunchbackCounter = 0
                                        straightCounter = 0
                                        if (poseRegister == "crossleg") {
                                            crosslegCounter++
                                        }
                                        poseRegister = "crossleg"

                                        /** Shows current posture (crossleg) */
                                        if (crosslegCounter > 60) {

                                            ivStatus.setImageResource(R.drawable.crossleg_confirm)
                                        } else if (crosslegCounter > 30) {
                                            ivStatus.setImageResource(R.drawable.crossleg_warning)
                                        }

                                        /** Debug messages */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $crosslegCounter")
                                    }
                                    else -> {
                                        hunchbackCounter = 0
                                        crosslegCounter = 0
                                        if (poseRegister == "standard") {
                                            straightCounter++
                                        }
                                        poseRegister = "standard"

                                        /** Shows current posture (straight) */
                                        if (straightCounter > 30) {

                                            ivStatus.setImageResource(R.drawable.standard)
                                        }

                                        /** Debug messages */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $straightCounter")
                                    }
                                }


                            }
                            else {
                                missingCounter++
                                if (missingCounter > 30) {
                                    ivStatus.setImageResource(R.drawable.no_target)
                                }

                                /** Debug messages */
                                tvDebug.text = getString(R.string.tfe_pe_tv_debug, "missing $missingCounter")
                            }
                        }
                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(this) else null)
    }

    // Initialize spinners to let user select
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_camera_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnCamera.adapter = adapter
            spnCamera.onItemSelectedListener = changeCameraListener
        }
    }

    // Change device (accelerator) type when app is running
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    private fun changeCamera(direction: Int) {
        val targetCamera = when (direction) {
            0 -> Camera.BACK
            else -> Camera.FRONT
        }
        if (selectedCamera == targetCamera) return
        selectedCamera = targetCamera

        cameraSource?.close()
        cameraSource = null
        openCamera()
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // pass
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
