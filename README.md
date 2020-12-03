# Switch Simple Screenshot

**As of now, this application is just a proof-of-concept. It's not intended for daily use.**

This application permits you to download all the screenshot shared by your Nintendo Switch only scanning the first QR Code.
This require your Nintendo Switch updated to version >= 11.0 since the "Send to Smartphone" features has been added only on version 11.0.

## How does this work

When you share screenshots from your console the Nintendo Switch became an (Hotspot)[https://en.wikipedia.org/wiki/Hotspot_(Wi-Fi)] protected by a randomly generated password and start hosting a (Web Server)[https://en.wikipedia.org/wiki/Web_server].
When you scan the first QR Code your device get the Hostpot SSID Name and Password to connect.
When you scan the second QR Code your device open the web page hosted by your Nintendo Switch at address http://192.168.0.1/index.html.
This page is initally empty: with the page the Nintendo Switch serve a JavaScript file that does a GET Request and download a JSON file from the Switch from http://192.168.0.1/data.json.
This data.json file contains all the strings displayed on the web page, translated to your language and an array containing **all the filenames** of the downloadable content.
Later on, the JavaScript download all the images contained into the array from http://192.168.0.1/img/extremely_long_file_name_that_we_find_in_json_array.jpg(/mp4).

After scanning the first QRCOde to connect to the Switch network, this application automatically connect to http://192.168.0.1/data.json to get the filenames of all the photos (or the video name) 
that you want to download and download of all the images to the default DCIM folder of your device.

## Known Issues

This is my "first" Android application. It's not ready for daily use and it's just a proof of concept, there's a lot of issues that need to be handled.
- [ ] Devices with Android version <10.0 are not supported (I don't have any devices with lower version to test the app, so I focused on devices with Android version 10+ that use newer API to store files in memory and connect to custom network)
- [ ] You need to restart the application to do multiple download (If the QR Code is found, the QR Code scanner just stop forever)
- [ ] There are no check on the WiFi name (every Switch hotspot name should start with "switch-" followed by a kind of serial number, if not checked this can become a potential security issue. **DO NOT SCAN QR CODE THAT YOU DONT TRUST. SCAN ONLY YOUR NINTENDO SWITCH QR CODE.**)

**This software is not developed nor mantained by Nintendo. If you find some issues please open an issue here on GitHub.**
