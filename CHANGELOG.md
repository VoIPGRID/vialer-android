# Changelog
This document is intended to be a place to list changes in technical
detail, ignoring the limits imposed on release notes by Google when
uploading to the play store. As with everything related to Vialer, this
document will be open for anybody to read who wishes to. However, this
is not targeted at end-users but rather at people who work on the
project to some extent and are very familiar with the entirety of the
project.

Not every version will need expanding on, if you cannot find changes
listed here for a version then please refer to the official release
notes on Google Play.

## [6.4.0] - 2019-09
### Onboarding

Onboarding has been completely overhauled from a back-end perspective,
instead of using a complicated chain of callbacks between fragments it
uses the new ViewPager 2 library to guide the user through a number of
steps (which are implemented as fragments). The critical difference
between this approach and the previous approach is that each "step" is
completely responsible for all the processing it needs to do.
Previously, each fragment would send data back to an activity which
would dispatch and handle HTTP requests and all other processing,
meaning all the logic for every step is muddled into a single class.

Adding steps to onboarding has become significantly easier as we now
have a literal list of steps that can be found in the OnboardingActivity
that the onboarder will progress through, this did not exist before and
would require each fragment to progress to the correct place. 

The main area of complexity is the fact that some steps are optional,
for example, the step to enter a 2FA code obviously should only show up
if the user has 2FA enabled. There are two passes to check whether a
step should be included, initially when all the steps are added (before
we know anything about the VoIPGRID user) and just before it is
displayed. This is implemented via the shouldThisStepBeSkipped() that
can be added on each step, this defaults to FALSE if it is not included.

From a user point of view, we have also added all the required
permissions to the onboarding process, this presents the user with
reasoning for why we need this permission and the ability to skip it.
This means that while the onboarding is marginally longer, we can have
the app in a completely ready state for the user without having to worry
about permissions at other points.

### Contact Searching (T9)

The previous implementation of contact searching (this is so a user can
search for contacts in the Dialer via a T9 search) involved a lengthy
import process when the user first logged in, even for less than 1000
contacts this could take minutes to complete. We would also need to keep
Vialer alive to regularly monitor any contacts changes so we could
update this database.

It became clear pretty quickly this could be simplified significantly,
this searching is now performed as a regex query on-the-fly against the
actual call records database. This is highly performant even against
10,000 contacts and has reduced the complexity of the app significantly,
the net result of this changed was removing 1600 lines of code and
removed multiple database tables that no longer need to be maintained or
considered.

### Call Records

There have been a few different implementations of call records, before
6.2 there was a complex caching mechanism in-place but this caching did
not allow us to query records or anything more complex, it was simply so
call records could be viewed offline. In 6.2 this was changed so call
records were displayed directly from the API, meaning they couldn't be
viewed offline but it was also possible to provide "endless scrolling"
and other features.

The next stage has been to import call records into a database, this
database is easily queryable which means we can actually display a
subset of data (such as missed calls) without having to query a large
chunk of data from the api.

The way this has been implemented is that upon first log-in the app will
make a large number of requests to the api to import all historic call
records, this will essentially mean going from the current month all the
way back to the start of 2015 and importing everything it finds. When a
month has been imported, this will be flagged and requests won't be made
again. This process is run on log-in as well as periodically to make
sure our local database has been properly synced.

The call records displayed to the user is a live view of the database,
so if they select missed calls they can actually view their entire
history of missed calls rather than the previous implementation which
would only show a very limited amount and would sometimes cause very
unexpected things due to the "hacky" nature of the implementation.

The user can still refresh call records, this will make a query to the
api for all records between now and the last call in the database which
should result in a minor speed increase.

Behind the scenes it has also been simplified significantly as there now
only exists a single call record fragment rather than having multiple
fragments for each type of data being displayed, when a different tab is
selected or a toggle is changed that fragment will just be told to
update its data.

### Android 10

Android 10 support has continued to improve with what can be considered
a final implementation of incoming call flow.

- If the user is in Vialer already, they will see the incoming call
  screen.
- If the user is using their phone (i.e. it is not locked) they will see
  a notification, they are able to answer/hang-up the call directly from
  the notification. If they click on the notification it will take them
  to the incoming call screen.
- If the user has locked their phone, they will see the incoming call
  screen.

### Changed
- The error message displayed when there is an issue with call records
  (no internet/permissions) has been made much more readable and
  visually pleasant. Under the hood the confusing system for displaying
  these error messages was completely removed and instead a much clearer
  and readable api was created.
  
- Several improvements have been made to the XML layout files to make
  them cleaner and easier to work with. The actual design presented to
  the user should remain unchanged.

### Fixed
 - There was an issue where the sound played when the third party ended
   the call sounded "corrupted". The cause of this was because we were
   playing the busy tone and then immediately resetting the sounds back
   to how they are outside the call which caused a bit of distortion. To
   circumvent this we are simply sleeping for the same length as the
   tone so we will no longer begin tearing things down until the tone
   has finished played.
  
-   When you connected to a bluetooth device other than a headset, a
    notification would remain saying Vialer was running. This was
    because we were ignoring any keys that didn't look like a headset,
    but not shutting down the service when doing so.
    
-  When a Vialer call was in progress and a GSM call was received, it
   would show/hide the icon in the notification bar multiple times. This
   was because we were checking for in-progress Vialer calls when the
   push notification came in, while we were checking for in-progress GSM
   calls after we had already booted up SIP (much further in the
   process). Both of these checks will now be performed immediately as
   the push notification comes in.