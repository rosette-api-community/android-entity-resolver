# Entity Resolver & Entity-Level Sentiment Analyzer (for Android)
**Hackathon project to demonstrate using RosAPI in an Android application.**

##Installation

1. Check out this repo to your machine.
2. Import the repo as a Gradle project in Android Studio (or IntelliJ).
3. Build and run from Android Studio by using the Android Emulator or an Android-powered device.

##How to Use

After EntityExtractor application has been launched, on the top right corner set User Settings by entering a valid Rosette API key (if you don't have one, you can get one [here](https://developer.rosette.com/signup)) and if you wish to run against another server you can specify it under the Rosette Alternate URL section. In the main screen you can enter any text and hit 'Extract' to retrieve entities. If any entities were found they will show as clickable buttons which will direct you to their corresponding Wikipedia page and the buttons themselves are color coded by the entity level sentiment.