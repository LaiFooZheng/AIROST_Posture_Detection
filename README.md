# Posture Detection System

## AIROST Internship 2022 (Made by Team Rocket)

<image align ="right" src="document_images/phone_icon.jpeg" alt="Application Icon" width="15%">

Posture Detection System is an Android-based mobile application which can be run on an Android mobile platform. It can detect incorrect sitting or standing postures and display messages on the app screen, namely sitting or standing hunched over and sitting crossleg. 

Our project refers to an Android application demonstration by **[TensorFlow Lite](https://github.com/tensorflow/examples/tree/master/lite/examples/pose_estimation/android)** as a foundation to build on. Our AI that detects postures is trained using Tensorflow's estimation model, **[MoveNet](https://blog.tensorflow.org/2021/05/next-generation-pose-detection-with-movenet-and-tensorflowjs.html)** while the classification is done using a [notebook provided by Tensorflow Lite.](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/tutorials/pose_classification.ipynb) The model that we used in our application is MoveNet Thunder.

## Document Structure

```bash
├───android
│   ├───app
│   │   └───src
│   └───gradle
├───document_images
├───training
│   └───posture_data
│       ├───forwardhead
│       ├───crossleg
│       └───standard
```

`android/` contains all files relating to the application

`document_images/` contains screenshots and images for README.md

`training/` contains AI training sample data, screenshots and files of the training process using Google Colab `pose_classification.ipynb`.

## Installation

To install our application on your Android device, just download and install the APK package under **[Releases](https://github.com/LaiFooZheng/AIROST_Posture_Detection/releases/tag/v1.0.1)**. 
If you want to do some testing with our code, it is recommended to do it in Android Studio as our project is build on said platform.

## Using Android Studio

### Prerequisites

* If you don't have it already, install **[Android Studio](https://developer.android.com/studio)** 4.2 or above, following the instructions on the website.

* Android device and Android development environment with minimum API 23.

### Building

* Fork and `git clone` the repository in the directory you desire on your machine.

* Open Android Studio, and from the `Welcome` screen, select
`Open an existing Android Studio project`.

* From the `Open File or Project` window that appears, navigate to and select
the `/android` directory from wherever you
cloned the our GitHub repo. Click `OK`.

* If it asks you to do a `Gradle Sync`, click `OK`.

* If it asks you to use `Instant Run`, click `Proceed Without Instant Run`.

* Also, you need to have an Android device plugged in with developer options
 enabled at this point. See **[here](
 https://developer.android.com/studio/run/device)** for more details
 on setting up developer devices.

## Execution Demonstration

After installing the Application on your Android device, there will be a prompt asking permission for camera access when opening the app.

The application itself uses the device's camera to detect if there is a target or in this case, a person, to be found. If there is, it will start counting frames per second, when it detects all 30 frames of said person is in either Hunchback or Crossleg, it will display a warning comment above the screen, if the person persist for another 30 frames, it will display a red comment.

Basically the comment on the screen will update every 30 frames based on the detected posture.

<table width="100%">
 <tr>
  <td width="25%" style="line-height:0;"><img src="document_images\no_target_detected.jpeg"></td>
  <td width="25%" style="line-height:0;"><img src="document_images\no_target_hunchback.jpeg"></td>
  <td width="25%" style="line-height:0;"><img src="document_images\warning_hunchback.jpeg"></td>
  <td width="25%" style="line-height:0;"><img src="document_images\red_hunchback.jpeg"></td>
 </tr>
<table>

## Credits
AIROST Internship 2022

Team Rocket
* LAI FOO ZHENG
* MUHAMMAD ADAM BIN YAACOB
* TAN CHUN MING
