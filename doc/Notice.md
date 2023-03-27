## 关于Retrofit和OkHttp版本的说明
**高版本只支持ApiLevel 21。**

**支持Android4.4的设备，两者能够使用的最高版本okhttp3_version = "3.14.4" retrofit_version = "2.7.1"。**

OkHttp发布日志：https://square.github.io/okhttp/changelogs/changelog_3x/
OkHttp支持Android5+说明：https://code.cash.app/okhttp-3-13-requires-android-5
即OkHttp3.13 从要求:
Android 2.3+ / API 9+ (released December 2010)
Java 7+ (released July 2011)
变更为:
Android 5.0+ / API 21+ (released November 2014)
Java 8+ (released March 2014)

OkHttp建议使用3.12.13及以后的版本，之前的版本底层存在socket关闭的竟态问题:
详见:https://github.com/square/okhttp/issues/6509
解决说明(官方在3.12.13版本修复):https://square.github.io/okhttp/changelogs/changelog_3x/#version-31213  
