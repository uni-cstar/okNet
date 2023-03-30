**如果对你有帮助，给个'star'鼓励一下**,第一次认真开源，谢谢。
[英文介绍](https://github.com/uni-cstar/oknet/blob/master/README.md)
## Introduction

这也许是目前(2023/03)Retrofit（OkHttp）支持多BaseUrl以及在运行时改变BaseUrl实现最简单的库；

同时也许也是支持全局Header配置实现最为简单、最为全面的库；

与Retrofit的@url方法不冲突，即如果@url指定的是其他baseurl，与全局配置的域名毫无关系，那么全局配置的baseurl和header不会对接口造成任何影响。

大家在开发过程中也许遇到过需要在运行期间切换baseurl或者需要支持多域名的情况，在此之前我基本是用的官方的动态 host的方案，需要其他域名的时候，都是通过retrofit的@url注解指定请求的地址，后来接触了[JessYanCoding大神](https://github.com/JessYanCoding)开源的[RetrofitUrlManager](https://github.com/JessYanCoding/RetrofitUrlManager)库，大神也发布了两篇文章详细的介绍了该库实现的依据，但是我一直在思考如何解决为了解决这个问题而不得不引入的`pathSegment`概念和三种模式(普通模式、高级模式、超级模式)，毕竟大神写这个库已经是好几年前，所以我一直在想一个更好的解决办法，能够忽略这些概念，让使用变得更加简单，这也是我写这个库的根本原因。

附大神文章：
[解决Retrofit多BaseUrl及运行时动态改变BaseUrl(一)](https://www.jianshu.com/p/2919bdb8d09a)

[解决Retrofit多BaseUrl及运行时动态改变BaseUrl(二)](https://www.jianshu.com/p/35a8959c2f86)
## 1. Setup

添加依赖
```
implementation 'io.github.uni-cstar:oknet:0.0.3'
```

## 2. Usage

#### 2.1 初始化功能
kotlin
```
OkHttpClient.Builder()
...
.addOkDomain(baseUrl)//初始化功能，baseurl为初始的主域名地址
...
.build()

```

java
```
OkHttpClient.Builder builder = new OkHttpClient.Builder();
OkDomain.useOkDomain(builder,baseUrl);
...
```

#### 2.2 在运行过程中修改主域名地址
`OkDomain.setMainDomain(newBaseUrl)`


#### 2.3 为主域名添加全局Header

`OkDomain.addMainHeader(key,value)`

#### 2.4 主域名移除全局Header
`OkDomain.removeMainHeader(key)`

**以上便实现了主域名（主BaseUrl）在动态运行过程中的切换以及全局Header的配置管理**

##  3. 高级用法
接下来配置其他的域名以及对应的全局Header管理，这些操作与前面主域名操作相似。

### 3.1 多域名支持


#### 3.1.1 添加一个域名
比如通过以下方法增加一个名字为`baidu`的域名配置，其baseUrl为`http://www.baidu.com/`(可以是任意的常见的域名，可以包含端口号，可以包含pathSegment，这一点与RetrofitUrlManager库不同)

```
OkDomain.setDomain("baidu","http://www.baidu.com/");
```

#### 3.1.2 使用域名
```
 public interface ApiService {
     @Headers({"Domain-Name: baidu"}) // 其中baidu就是前面添加的域名名字，其格式为"Domain-Name:{域名名字}"
     @GET("/v2/book/{id}")
     Observable<ResponseBody> getBook(@Path("id") int id);
     
     //也可以使用内置的常量来拼接域名，避免写错
     @Headers({OkDomain.DOMAIN_NAME_HEADER+"baidu"})
     @GET("/v2/book/{id}")
     Observable<ResponseBody> getBook(@Path("id") int id);
}
```
通过在接口定义上方增加域名的Header，即可让该请求使用对应域名的baseurl。

#### 3.1.3 更改域名的BaseUrl
通过前面配置的域名名字去修改配置的BaseUrl
```
OkDomain.setDomain("baidu","your new base url for the domain");
```

#### 3.1.4 为指定域名增加全局header

`OkDomain.addHeader("baidu",key,value)`

#### 3.1.5 为指定域名移除全局header
`OkDomain.removeHeader("baidu",key)`

### 3.2 Header的高级用法(Advanced usage of Header)
`OkDomain.addMainHeader(key,value)`和`OkDomain.addHeader(domainName,key,value)`均有一个重载的方法，其末尾接收一个`OnConflictStrategy`参数，该参数用于处理在ApiService定义的接口包含了与全局配置相同key的header时，该Header的处理策略。

- OnConflictStrategy.IGNORE 如果ApiService中包含了该Header，则全局配置中对应key的header不会添加到请求中
- OnConflictStrategy.REPLACE 全局配置中的key对应的header会替换ApiService中添加的header（如果存在则替换，不存在则添加）
- OnConflictStrategy.ADD 不管ApiService中是否有对应key的header，全局配置中的header都会添加到请求中（一个请求是允许包含多个相同key的header的）
- OnConflictStrategy.ABORT 如果全局配置中的Key对应的Header，在ApiService中有相同Key的Header，则会中止请求，并抛出异常（可能会导致程序崩溃，一般不怎么使用该策略）

eg.
```
//在主域名中配置了一个key = customKey，value = GlobalHeaderValue的header，该header使用IGONRE策略，即如果ApiService中也包含了key = customKey的header时，不会将全局配置的该header添加到请求的header中
OkDomain.addMainHeader("customKey", "GlobalHeaderValue", OnConflictStrategy.IGNORE)

//为‘baidu’这个域名配置了一个key = abort_key,value = GlobalHeaderValue的header,该header使用ABROT策略，即如果ApiService中包含了key = abort_key的header，将会导致抛出异常
OkDomain.addHeader("baidu","abort_key", "GlobalHeaderValue", OnConflictStrategy.ABORT)
```

### 4 额外说明
如果ApiService没有配置域名名字，则默认会使用主域名的配置进行处理；
如果ApiService使用了@url注解，则存在以下两种情况：
@url产生的请求的url与任何已经配置的域名（包括主域名）无关系，则已配置的域名和全局header对此请求不会有任何影响，相反如果@url生成的请求的url地址在配置中找到了域名，则会根据配置信息做对应处理。

### 5 其他Api
#### 5.1 Log
由于这个库是一个java库，没有依赖android sdk，所以日志使用的是system.out.print，可以通过如下方法关闭日志：
`OkDomain.debug = false` // or true,default is true
#### 5.2 启用/禁用组件
如果您需要关闭该库的所有功能，执行以下代码即可关闭/开启：
`OkDomain.enable = false` // or true,default is true


