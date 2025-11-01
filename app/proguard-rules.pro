# MAL Image Downloader ProGuard Rules
# Author: osphvdhwj
# Date: 2025-11-01

# Keep all classes in our main package
-keep class com.osphvdhwj.malimage.** { *; }

# Keep data classes for JSON serialization
-keep class com.osphvdhwj.malimage.parser.** { *; }

# Keep ExifInterface classes
-keep class androidx.exifinterface.media.** { *; }

# Keep metadata extractor classes
-keep class com.drew.metadata.** { *; }
-keep class com.drew.imaging.** { *; }

# Keep Simple XML classes
-keep class org.simpleframework.xml.** { *; }
-keep class * extends org.simpleframework.xml.** { *; }

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Retrofit classes
-keep class retrofit2.** { *; }

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-keep class com.osphvdhwj.malimage.download.ImageDownloadWorker { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Android support classes
-keep class androidx.** { *; }

# Standard Android rules
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep setters in Views
-keepclassmembers public class * extends android.view.View {
   void set*(***); 
   *** get*();
}

# Keep Activity and Service classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}