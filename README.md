# Yolow

Yolow is an Android native app for use with mbientlab metamotion R+ sensors. It is used for research purposes to determine the effectiveness of using wearable haptics to teach yoga to people with low vision. 
The name derives from Yo(ga for) Low (vision). 

## Installation 
The app can be installed on android devices running API 19 or higher by downloading the .apk.

## Operation
When the app starts, turn on Bluetooth and give the app permission to connect to Bluetooth devices.
Add devices by selecting "scan devices" at the bottom left of the screen. When you select a device, it will appear on the screen as a box. When the box turns blue, that means the sensor has been successfully connected.

Create haptic patterns on the Preset tab. You can either create a pattern using the text boxes, or upload a CSV file, as detailed below.

## CSV Files
Custom haptic patterns can be created and uploaded using .csv files. The expected format of the .csv file is:

```
on,off,intensity
0.5,0.1,100
0.4,0.2,100
0.3,0.3,100
0.2,0.4,100
0.5,0.1,100
```
The first row is included for readability and is ignored. The first column (on) is the amount of time the sensor spends vibrating, and the second column (off) is the amount of time (in seconds) the sensor rests until the next vibration.
The third column, intensity, is a value between 0-100 that will show the power of the haptic, with 100 being full power and 0 being no power. If intensity is not included, it will default to 100.

##Data Export
After recording a session, the data files will be available in the device's internal storage at:
```
Internal Storage / Android / data / com.mbeintlab.metawear.tutorial.multimw / files / yolow_[timestamp of the recording session]
```

## Contribution 
This app was created by Taylor Thackaberry (Virginia Tech), with advisors Sang Won Lee (Virginia Tech) and Sol Lim (University of Arizona). 

This app builds off of [Metawear's Multisensor Tutorial app](https://github.com/mbientlab/MetaWear-Tutorial-Android/tree/master/multimw)
