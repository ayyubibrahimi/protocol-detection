# Network Protocol Downgrade Detector

Work in Progress

The Network Protocol Downgrade Detector is an Android application developed using Android Studio and Kotlin. Its primary objective is to assist users in identifying the presence of an attack by state or non-state actors utilizing stingrays or cell-site simulators. 

## Features
- Check network status: The application continuously monitors the network status and logs any changes in the network protocol.

- Detect network protocol downgrade: If the network protocol is downgraded from 4G/5G to 2G/3G a warning message is logged indicating possible network interference.

- Logging: The application logs network status changes and warnings to provide visibility into network  events.

- Permissions handling: The application requests necessary permissions (READ_PHONE_STATE) to function properly and provides appropriate instructions to users if the permissions are not granted.

## Installation
- Clone or download the repository.
- Open the project in Android Studio.

## Usage
- Launch the Network Status Checker application on your Android device using the SDK manager
- Grant the required permissions when prompted.
- The status text view will display the current network status (Not checking network status by default).
- To start checking the network status, click the "Start" button.
- The status text view will update to display "Checking network status" and change the background color.
- The application will periodically (every 10 seconds) check the network status and log any changes.
- If a network protocol downgrade is detected a warning message will be logged.
- To stop checking the network status, click the "Stop" button.
- The status text view will update to display "Not checking network status" and revert the background color.
- The application will stop monitoring the network status.

