# 0.0.7 
- 支持带进度的文件下载（具体查看OkNetUseCase相关类），安全处理回调

# 0.0.6
- 新增部分工具类（网络检测、tls证书校验等相关的工具类）
- 发布maven，支持远程依赖

# 0.0.5

- 新增OkNetClient,提供常规客户端Http（Retrofit+OkHttpClient）的使用场景
- 日志信息打印在android平台上运行时从System.out.println改为android.util.Log实现

# 0.0.4

- 优化Log信息的处理 日志打印改为内联方法，通过高阶函数提供打印的消息：避免非debug模式同样创建了字符串信息的问题
- 新增Okhttp证书校验相关的资料类
- 新增网络连通检测的工具类