# MyRuns

Android app that tracks your workouts (and your walks/runs!)

Project built in CMPT 362 at SFU.

## How to run

Before trying to run the app on an Android phone or emulator, you must 
edit the `local.properties` file. This file is in the gitignore. More 
specifically, this file needs to contain:

```
sdk.dir=/path/to/android/sdk/on/your/computer
MAPS_API_KEY=   
```

You can get an API key for Google Maps 
[here](https://developers.google.com/maps/documentation/android-sdk/get-api-key).

This API key is needed for functionalities within the app that use Google 
Maps.

## Features

Exercises can be recorded either manually or automatically. Manually recording an exercise means that you need to input the necessary information (name of the exercise, duration, calories burnt, etc.), but automatic mode can detect if you are 
standing, walking, running, or something else using machine learning (via a classifier model built using Weka) as well as will automatically input the duration, calories burnt, etc. The machine learning model works by taking in gyroscope and 
accelerometer data from your Android device to then classify what type of expercise you may be doing. These exercises are stored in the backend, which is local to the 
device. Furthermore, automatic mode uses the Google Maps API to track your location and create a trace on the map to where you were.

The backend of the app uses Room databases, which are based off of SQLite. Coroutines are used for the I/O on these databases to improve performance.

Exercise history can be viewed and individual exercises can be deleted from the history. When viewing exercises, you can see the information about the exercise as well as the map that traces where you were.

This app uses settings that are stored locally with SharedPreferences. It 
will remember these settings for as long as the app is installed. Some 
information stored here include units (miles vs. kilometers), name, email, 
and alike. Changing the units setting will change the units everywhere on 
the app.

## Potential improvements

A backend with a server or cloud services would be better (and more costly) so that the app can be installed/uninstall with all of the information still saved. Firebase is a cheap option for this.
