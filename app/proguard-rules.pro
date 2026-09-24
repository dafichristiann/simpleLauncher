# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
