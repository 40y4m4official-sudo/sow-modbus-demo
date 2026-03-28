# Keep USB serial driver classes intact for release builds.
-keep class com.hoho.android.usbserial.** { *; }
-dontwarn com.hoho.android.usbserial.**

# Keep FileProvider metadata wiring intact.
-keep class androidx.core.content.FileProvider { *; }
