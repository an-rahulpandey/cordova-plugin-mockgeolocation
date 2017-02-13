# cordova-plugin-mockgeolocation
Detect Mocked GeoLocation on Android Devices

# Note
This Plugin Only works on Android. Currently the work in not yet finished, but this will get the most of the job done. The permission module is not properly implement, so please make sure you take care of the permissions. You can use this plugin [Cordova Diagnostic Plugin](github.com/dpa99c/cordova-diagnostic-plugin).

# How To
    //options can be null too
    var options = {
	'priority':'high', //can be one of these low, no, balanced and high
	'updateInterval': 1000, //to checke for new location for this time interval used in conjunction with maxnumberupdates.
	'maxNumberUpdates': 2, //maximum number of geolocation to fetch to detect if the location is fake. More the number the better the chances.
	'numTimesPermissionDeclined':2 //plugin will throw error message if the user declines the permission to get location.
    };

    window.plugins.mockgeolocation.check(options, function(s) {
                    if (s.mockLocationEnabled) {
                        console.log("Fake Location Detected");
                    } else {
                        console.log("Location is Not Faked");
                    }
                }, function(e) {
                    console.log("Plugin Error Occured",e);
                });
