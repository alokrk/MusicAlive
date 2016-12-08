# MusicAlive

##F1f

- This is now a partial application. We extract some of the contextual information from the ContextMiddleware app.

- Location, Headphone status and Place has been moved to middleware.
- We keep Activity and Weather in this app for now since those snapshots objects are difficult to manipulate using callback interfaces.

- Location, places and headphone status are standard. We use code from the sample application provided for reference to create the middleware.

- We have also removed the Fences API code since it is no longer required due to the middleware implementation. 

- We, however keep the Snapshot code due to activity and weather object constraints. 
- This will also ensure the music player works even if we do not find the location properly. [Mulitple checks for headphone]
- If connection works, then we display a simple toast. Otherwise, we continue using the music player based on only activity and weather instead of location and place context.

###Working
- Context Extraction
--- Headphone status
-–- Activity
--- Location
--- Weather
- Music Playback
--- By Activity
--- Followed by weather
--- Skipped location due to integer output type of location from Google. No way to convert to meaningful type. Refer pType
- Skip to next track if current does not suit context
- Spotify will learn from user listening history so adaptation for user sourced to Spotify using APIs. By user permission, user playlists are part of context search.
- Playlist adapts as per user context. So we have both real-time context adaptation and feedback adaptation.

###Process
- Launch Middleware
- Launch Music App
- Plug in Headphone
- Default music might start at first. Could be because of Oauth login delay, location API conflict delay etc. Requesting TA to restart the app in case there is any crash on launch.
- As context changes, the playlist changes

###Future Scope
- Minor Bug fixes
- Move Snapshot APIs to the middleware. Thereby enabling full middleware control instead of partial as of now.

###Known Bugs
- Weather condition handling could be buggy based on return. Google page does not mention multiple condition scenario. Found out just before commit that multiple weather conditions can be returned. Modify logic to suit this requirement.

###Acknowledgement

Middleware code and AIDL linkage etc code used from:

https://github.com/pradeepmcl/SampleForegroundApplication

https://github.com/pradeepmcl/ContextMiddleware/

Reference Page:
https://developer.android.com/guide/components/aidl.html
