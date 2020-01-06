# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

-dontobfuscate

# For using GSON @Expose annotation
-keepattributes *Annotation*

-keepattributes SourceFile,LineNumberTable

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
#-keep class com.google.gson.examples.android.model.** { *; }
-keep class de.tgoll.projects.bzf.CatalogueFragment.SavedState { *; }
-keep class de.tgoll.projects.bzf.Trial { *; }

# Add the gson class
-keep public class com.google.gson

# Add any classes the interact with gson
-keep class de.tgoll.projects.bzf.CatalogueFragment { *; }
-keep class de.tgoll.projects.bzf.StatisticsFragment { *; }
-keep class de.tgoll.projects.bzf.StatisticsFragment.NoneValueFormatter { *; }
-keep class de.tgoll.projects.bzf.StatisticsFragment.PercentFormatter { *; }

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Add the path to the jar
#-libraryjars C:\Android\Projects\BZF\app\libs\gson-2.2.2.jar

##---------------End: proguard configuration for Gson  ----------

