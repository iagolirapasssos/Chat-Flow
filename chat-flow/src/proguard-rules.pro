# Keep all classes in the extension package
-keep class com.bosonshiggs.chatflow.** { *; }

# Keep App Inventor annotations
-keepattributes *Annotation*
-keep class com.google.appinventor.components.annotations.** { *; }

# Repackages optimized classes into com.bosonshiggs.chatflow.chatflow.repacked package in resulting
# AIX. Repackaging is necessary to avoid clashes with the other extensions that
# might be using same libraries as you.
-repackageclasses com.bosonshiggs.chatflow.chatflow.repacked

# Keep Markwon and its dependencies
-keep class io.noties.markwon.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature

# Keep all classes that might be used by reflection
-keepclassmembers class * {
    @com.google.appinventor.components.annotations.SimpleFunction *;
    @com.google.appinventor.components.annotations.SimpleEvent *;
    @com.google.appinventor.components.annotations.SimpleProperty *;
}
