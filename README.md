# 探花交友项目概述

## 一、项目的模块结构

> tanhua作为顶级父工程管理下面模块的通用依赖

- tanhua-admin ——后台管理系统对应的服务端
- tanhua-app-server ——App对应的服务端
- tanhua-autoconfig ——存放第三方技术的API工具类，使用工厂自动装配到spring容器中
- tanhua-commons ——存放一些公共的工具类，包括JWT，状态码定义
- tanhua-dubbo
  - tanhua-dubbo-db —— 存放与MySQL相关的表和api实现类，提供远程调用服务
  - tanhua-dubbo-interface —— 存放MySQL和MongoDB的api接口
  - tanhua-dubbo-mongo —— 存放MongoDB相关的表和api实现类等，提供远程调用服务
- tanhua-gateway —— 网关相关的模块，校验登录状态等
- tanhua-model —— 存放所有实体类，包括两个数据库表对应的实体类，以及接收DTO类、响应VO类
- tanhua-recommend —— 推荐系统的模块，获取对用户操作动态和小视频的行为信息，记录到相关推荐表中，为Spark推荐系统提供数据支撑





## 二、使用的技术栈

### （1）主要技术栈：

- **SpringBoot + MybitsPlus + MySQL +Redis + MongoDB + Dubbo + Nacos + RabbitMQ  + SpringCloud GateWay**



部分说明：

1、MySQL用于存储：

- App端的基础表：用户基础信息表，用户详细信息表，用户设置表，陌生人问题表；

- 后台管理系统：管理员表，用户操作记录表，用户操作分析表（用户快速查询用户操作记录的统计情况）

2、Redis用于存储验证码，用户对动态和评论的点赞情况，自己点击查看访客的上次时间，推荐动态Pid和视频的序列，用户喜欢与不喜欢的用户列表以及关注的用户列表
3、MongoDB 用于存储数据量相对会大的表：评论点赞Comment表，动态表，动态时间表，视频表，访客表，用户地理位置表，推荐用户表，朋友表，关注表

4、Dubbo 用户远程服务调用，提供者包括MySQL和MongoDB 的api层对象，消费者包括app-server端和后台管理admin端的service层

5、Nacos 既作为服务注册中心，又作为项目的配置中心，管理配置文件

6、RabbitMQ主要用于监听和接收客户的操作记录行为消息，以及发布动态之后调用阿里云的审核任务



### （2）其他技术

- 阿里云OSS对象存储 + 阿里云短信发送 + 环信即时通信系统 + 阿里云内容安全审核 + FastDFS分布式存储系统 + SpringCache



部分说明：

1、OSS用于存储用户的头像和用户发布的动态图片

2、阿里云内容安全审核用于审核用户发布的动态里的文案和图片是否合规

3、FastDFS分布式文件系统用于存储用户发布的小视频（如果使用oss，费用会比较高）

4、项目中使用SpringCache结合redis，对查询小视频的方法结果进行了缓存30秒的缓存



# 第一阶段（实现APP基础功能）

> 说明：请求url的第一个参数决定该请求处于哪个Controller中，例：/user/login  -->  UserController



### 1、用户1、用户登录

- 阿里云验证码短信发送： /user/login    （可以展示功能的号码：99号和106号用户）
- 用户输入验证码检验： /user/loginVerification
- 

### 2、用户完善具体信息

- 新建保存userInfo表：  /user/loginReginfo
- 上传头像： /user/loginReginfo/head
- 

### 3、查看或修改用户资料

- 查看用户资料：  /users  -->get
- 修改用户资料： /users   -->put
- 

### 4、统一token和异常处理

- 配置SpringMVC的拦截器来实现token检验
- 封装phone和userId为对象保存到ThreadLocal中
- 全局统一异常处理



### 5、通用设置功能

> 特殊：这个模块是对应的是 SettingsController，而非UsersController

- 查看简单通用设置： /users/settings  -->  get
- 修改或保存陌生人问题： /users/questions  -->  post
- 修改或保存通知设置： /users/notifications/setting  --> post
- 查询黑名单分页列表： /users/blacklist  -->  get
- 取消黑名单： /users/blacklist/:uid  --> delete





# 第二阶段（实现APP的娱乐功能）

### 1、用户推荐相关

- 今日佳人推荐： /tanhua/todayBest  --> get（需要从mongoDB中的推荐用户表中获取最高分用户，大数据推荐系统实现）
- 查看佳人信息： /tanhua/:id/personalInfo  -->  get

- 推荐好友：  /tanhua/recommendation  -->  get（需要从mongoDB中的推荐用户表中获取，大数据推荐系统实现）



### 2、圈子功能

- 发布动态 ：/movements  --> post
- 查询自己动态列表： /movements/all  --> get
- 查询好友动态列表： /movements    -->  get
- 查询推荐动态列表： /movements/recommend   --> get（需要从redis中获取推荐的动态pid，大数据推荐系统实现）
- 查询单条动态详情：/movements/{:id}  -->  get
- 发表评论： /comments  -->  post
- 分页查询评论列表:   /comments   -->  get 
- 点赞动态：  /movements/:id/like  --> get
- 取消点赞动态： /movements/:id/dislike  --> get
- 喜欢动态 :    /movements/:id/love   -->  get
- 取消喜欢动态： /movements/:id/unlove --> get



### 3、消息模块

- 查询点赞自己动态的简要情况列表消息： /messages/likes  -->  get 
- 查询喜欢自己动态的简要情况列表消息：/messages/loves  -->  get
- 查询评论自己动态的简要情况列表消息:  /messages/comments  --> get



### 4、环信即时通讯功能搭建

- App端需要连接上环信，登录后app端会自动发送用户id来查询环信账户密码用来登录环信： /huanxin/user   -->  get
- 聊天是基于环信上的，为了显示出用户详情，需要实现根据环信id查询用户信息： /messages/userinfo  -->  get 



### 5、添加好友

- 点击聊一聊（添加好友），查询陌生人问题： /tanhua/strangerQuestions  -->  get
- 填写并提交陌生人问题答案（发送添加好友请求给对方，环信都需要处于登录状态）：  /tanhua/strangerQuestions  -->  post 
- 被申请添加好友的人点击确认添加对方为好友：  /messages/contacts   -->  post
- 查询自己的联系人列表，可按用户昵称关键字查询：  /messages/contacts  --> get 



### 6、首页交友功能

- 卡片推荐用户：  /tanhua/cards  -->  get
- 喜欢当前卡片的用户（右滑）:   /tanhua/:id/love  -->  get
- 不喜欢当前卡片用户（左滑）:  /tanhua/:id/unlove  -->  get
- 上传地理位置：  /baidu/location  -->  post
- 搜索附近用户： /tanhua/search  -->  get



### 7、访客记录

- 保存访客记录（用户点击查看某佳人的信息时就会被记录），将当前时间戳和当前日期都记录下来，两者功能不同，后者时为了同一天不重复将访客记录保存到数据库，前者时为了让查询者有已阅的效果 。对应路径： /tanhua/:id/personalInfo  -->  get
- 显示最近访问过我的记录（显示简单头像）：  /movements/visitors  -->  get
- 点击查询访客的详情列表： /users/friends/4   -->  get



### 8、小视频相关功能

- 发布视频： /smallVideos   -->  post
- 查询视频列表：  /smallVideos   -->  get
- 查看视频时点击关注用户：  /smallVideos/:uid/userFocus  -->  post
- 查看视频时取消关注用户：  /smallVideos/:uid/userUnFocus  -->  post
- 小视频中的点赞和取消点赞功能的实现与动态的类似.....





# 第三阶段（实现网关和后台管理系统功能）

### 1、SpringCloud GateWay搭建

- app-server端和admin端的请求都需要先经过GateWay过滤和转发
- AuthFilter ：网关统一检验Token



### 2、后台管理员登录

- 前端页面得到验证码图片： /system/users/verification  -->  get
- 检验验证码：   /system/users/login  -->  post
- 登录后获取当前管理员的基本信息：   /system/users/profile  --> post



### 3、后台查询用户的相关信息

- 查询首页要管理的用户列表： /manage/users  -->  get  
- 根据id查询用户详情：  /manage/users/{userId}  --> get
- 查询指定用户的所有视频列表： /manage/videos  --> get
- 查询动态列表，可以是一个人，也可以是所有人（请求传递的参数区分）： /manage/messages  --> get
- 查询单条动态： /manage/messages/:id  --> get



### 4、后台冻结用户功能

- 冻结指定用户： /manage/users/freeze  -->  post
- 解冻指定用户： /manage/users/unfreeze  -->  post



### 5、后台统计用户活跃情况

- LogListener ：使用RabbitMQ记录用户行为消息，app-server端记录发送，后台管理系统接收，然后写入tb_log表
- AnalysisTask ：后台管理系统为统计功能设置定时任务，将tb_log中当天登录，注册，活跃，留存用户数量查询并保存到tb_analysis表



### 6、后台审核动态内容

- AuditListener ：发布动态的同时，也要将动态id发送到RabbitMQ中，后台管理系统监听接收，然后调用阿里云内容安全接口审核；因此，查询个人或好友动态时，就要根据动态的state字段是否为1（审核通过）来筛选



### 7、提供用户行为数据给推荐系统

- RecommedMovementListener ：用户发表动态，或对他人的动态进行点赞评论等行为数据会记录到对应的得分表中，然后推荐系统会以得分表的数据来分析，得出推荐结果
- RecommendVideoListener ： 用户发布小视频，或对他人的小视频进行点赞评论等行为数据也同样会记录到对应的得分表中













