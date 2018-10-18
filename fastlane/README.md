fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew cask install fastlane`

# Available Actions
## Android
### android test
```
fastlane android test
```
Runs all the tests
### android screenshot
```
fastlane android screenshot
```
Do a screenshot run
### android apk
```
fastlane android apk
```
Generate a signed APK
### android beta
```
fastlane android beta
```
Submit a new Beta version to the Google Play Store
### android release
```
fastlane android release
```
Submit a new Production version to the Google Play Store
### android alpha
```
fastlane android alpha
```
Submit a new Alpha version to the Google Play Store

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
