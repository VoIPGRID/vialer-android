# Vialer Android

Vialer is a VoIP client which utilizes the VoIPGRID platform. Up until version 3.0 it's main purpose is to provide an easy interface for the "Two step Calling" function of the platform. This feature enables the user of the app to dial a phone number as if you were "at the office". The receiver sees the "office" number as the calling number (CLI). Using the app, the user can also:

- Adjust its availability
- See call statistics
- See and adjust dialplans

Starting from Version 3.0 the app also acts as a VoIP softphone, enabling the user to make and receive phone calls using SIP but only when connected to WiFi of 4G.

## Technical implementation

On an outgoing call the app registers itself with VoIPGRID's sip proxy and after the call has ended the registration is removed. We have deliberately chosen for this registering/deregistering process to avoid "dangling" registrations on the proxy when the phone goes out of proper internet coverage.

On an incoming call, the phone is notified through a silent push notification through firebase. When this notification is received the device will check the quality of the internet connection. If the connection is WiFi or 4G the app will register with the proxy. When registered the device will be called and the device will wake up with a pick-up screen.

To be able to sent the push notifications a piece of "middleware" software has been developed. A device registers at this middleware with a firebase token used to sent a notification to the device. This token will be used to wake the device when an incoming call needs to be placed.

## Status

This project is under active development and is actively maintained.

## Usage

### Requirements

- Android 4.2+
- Alternative Android-based OSs not supported (OxygenOS / HydrogenOS) 
- Preferred resolution of 720x1280 / 1080x1920 pixels (Not optimized for tablet)
- Strong internet connection for VoIP calls (Wi-Fi, or 4G)
- To be able to use the application you will need an account from one of the VoIPGRID partners
- You will need a active firebase configuration for the app and middleware to be able to receive incoming calls.

### Installation / Running

Open project in Android Studio or any similar IDE. Have the IDE run and build the project to get an APK file to install on your device. After installation the app can be run from the phone like any other app.

## Contributing

See the [CONTRIBUTING.md](CONTRIBUTING.md) file on how to contribute to this project.

## Contributors

See the [CONTRIBUTORS.md](CONTRIBUTORS.md) file for a list of contributors to the project.

## Roadmap

### Changelog

The changelog can be found in the [CHANGELOG.md](CHANGELOG.md) file.

### In progress

- Increasing the app's stability and reliability.
- Support for Bluetooth carkits.

### Future

- Calling over secure connections
- Redesign to be in line with the latest google design guidelines.

## Get in touch with a developer

If you want to report an issue see the [CONTRIBUTING.md](CONTRIBUTING.md) file for more info.
We will be happy to answer your other questions at opensource@wearespindle.com

## License

Vialer Android is made available under the GPL-3.0 license. See the [LICENSE.md](LICENSE.md) for more info.
