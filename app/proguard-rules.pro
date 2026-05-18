# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keepnames @dagger.hilt.android.qualifiers.* class * { *; }

# Moshi codegen
-keep class **JsonAdapter { *; }
-keep,allowobfuscation class kotlin.Metadata
-keepclassmembers,allowobfuscation class * { @com.squareup.moshi.* <methods>; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }

# WorkManager
-keepclassmembers class * extends androidx.work.Worker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
-keepclassmembers class * extends androidx.work.CoroutineWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
