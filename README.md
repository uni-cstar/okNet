**If it's helpful to you, give me a 'star' to encouragement**,This is the first time I seriously provide my open source，Thank u.

[中文说明](https://github.com/uni-cstar/oknet/blob/master/README_CN.md)

## Introduction

This may be currently (2023/03) Retrofit (OkHttp) supports multiple BaseUrl and changes BaseUrl at runtime to achieve the simplest library;

At the same time, it may also be the simplest and most comprehensive library that supports global Header configuration implementation;

It does not conflict with Retrofit's @url method, that is, if @url specifies another baseurl, which has nothing to do with the globally configured domain name, then the globally configured baseurl and header will not have any impact on the interface.

## 1. Setup

Add Dependencies:
```
implementation 'io.github.uni-cstar:oknet:0.0.3'
```

## 2. Usage

#### 2.1 Initialize
kotlin
```
OkHttpClient.Builder()
...
.addOkDomain(baseUrl)//Initialization function, baseurl is the initial main domain name address
...
.build()

```

java
```
OkHttpClient.Builder builder = new OkHttpClient.Builder();
OkDomain.useOkDomain(builder,baseUrl);
...
```

#### 2.2 Change BaseUrl
`OkDomain.setMainDomain(newBaseUrl)`


#### 2.3 Add Global Header

`OkDomain.addMainHeader(key,value)`

#### 2.4 Remove Global Header
`OkDomain.removeMainHeader(key)`

**The above realizes the switching of the main domain name (main BaseUrl) during dynamic operation and the configuration management of the global Header**

##  3. Advanced Usage
Next, configure other domain names and corresponding global Header management. These operations are similar to the previous main domain name operations.

### 3.1 Multi Domain
Multi-domain name support.

#### 3.1.1 Add Domain
eg. add a domain name configuration named `baidu` by the following method, and its baseUrl is `http://www.baidu.com/` (can be any common domain name, can include port number, can include pathSegment , which differs from the RetrofitUrlManager library)

```
OkDomain.setDomain("baidu","http://www.baidu.com/");
```

#### 3.1.2 Use Domain
```
 public interface ApiService {
     //baidu is the name of the domain name added , and its format is "Domain-Name:{domain name}"
     @Headers({"Domain-Name: baidu"}) 
     @GET("/v2/book/{id}")
     Observable<ResponseBody> getBook(@Path("id") int id);
     
     //You can also use built-in constants to splice domain names to avoid typos
     @Headers({OkDomain.DOMAIN_NAME_HEADER+"baidu"}) 
     @GET("/v2/book/{id}")
     Observable<ResponseBody> getBook(@Path("id") int id);
}
```
By adding the header of the domain name above the interface definition, the request can use the baseurl of the corresponding domain name.

#### 3.1.3 Change Domain BaseUrl
```
OkDomain.setDomain("baidu","your new base url for the domain");
```

#### 3.1.4 Add Domain Global Header

`OkDomain.addHeader("baidu",key,value)`

#### 3.1.5 Remove Domain Global Header
`OkDomain.removeHeader("baidu",key)`

### 3.2 Advanced usage of Header
Both `OkDomain.addMainHeader(key,value)` and `OkDomain.addHeader(domainName,key,value)` have an overloaded method, which receives an `OnConflictStrategy` parameter at the end, which is used to process the interface defined in ApiService When a header with the same key as the global configuration is included, the header's processing strategy.
- OnConflictStrategy.IGNORE If the header is included in the ApiService, the header corresponding to the key in the global configuration will not be added to the request (default policy)
- OnConflictStrategy.REPLACE The header corresponding to the key in the global configuration will replace the header added in ApiService(The header corresponding to the key in the global configuration will replace the header added in ApiService (if it exists, it will be replaced, if it does not exist, it will be added)
- OnConflictStrategy.ADD Regardless of whether there is a header corresponding to the key in ApiService, the header in the global configuration will be added to the request (the request of OkHttp is allowed to contain multiple headers with the same key)
- OnConflictStrategy.ABORT If the Header corresponding to the Key in the global configuration has a Header with the same Key in ApiService, the request will be aborted and an exception will be thrown (may cause the program to crash, and this strategy will not be used in general)

eg.
```
//A header with key = customKey, value = GlobalHeaderValue is configured in the main domain name. This header uses the IGONRE strategy, that is, if ApiService also contains a header with key = customKey, the globally configured header will not be added to the header of the request
OkDomain.addMainHeader("customKey", "GlobalHeaderValue", OnConflictStrategy.IGNORE)

//A header with key = abort_key, value = GlobalHeaderValue is configured for the domain name 'baidu', and the header uses the ABROT strategy, that is, if ApiService contains a header with key = abort_key, an exception will be thrown
OkDomain.addHeader("baidu","abort_key", "GlobalHeaderValue", OnConflictStrategy.ABORT)
```

### 4 额外说明
If the ApiService is not configured with a domain name, it will use the configuration of the main domain name for processing by default;
If the ApiService uses the @url annotation, the following two situations exist:
The url of the request generated by @url has nothing to do with any configured domain name (including the main domain name), then the configured domain name and global header will not have any impact on this request. On the contrary, if the url address of the request generated by @url is in the configuration If the domain name is found in , it will be processed according to the configuration information.

### 5 Other Api
#### 5.1 Log
Since this library is a java library and does not depend on the android sdk, the log uses system.out.print, and the log can be turned off by the following method:
`OkDomain.debug = false` // or true,default is true
#### 5.2 enable/disable 
If you need to turn off all functions of this library, execute the following code to disable/enable:
`OkDomain.enable = false` // or true,default is true
