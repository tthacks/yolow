# Yolow

Yolow is an Android native app for use with mbientlab metamotion R+ sensors. It is used for research purposes to determine the effectiveness of using wearable haptics to teach yoga to people with low vision. 
The name derives from Yo(ga for) Low (vision). 

## Installation 
The app can be installed on android devices. 

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

## Contribution 
This app was created by Taylor Thackaberry (Virginia Tech), with advisors Sang Won Lee (Virginia Tech) and Sol Lim (University of Arizona). 

This app builds off of [Metawear's Multisensor Tutorial app](https://github.com/mbientlab/MetaWear-Tutorial-Android/tree/master/multimw)
