<p align="center">
    <img src=https://img.qimuu.icu/typory/logo.gif width=188/>
</p>

<h1 align="center">Auto Clock In 自动打卡</h1>
<p align="center"><strong>职校家园自动打卡助手</strong></p>

<div align="center">
    <img alt="Maven" src="https://raster.shields.io/badge/Maven-3.8.1-red.svg"/>
   <img alt="SpringBoot" src="https://raster.shields.io/badge/SpringBoot-2.7+-green.svg"/>
  <a href="https://github.com/qimu666/autoclockin-backend" target="_blank"><img src='https://img.shields.io/github/forks/qimu666/autoclockin-backend' alt='GitHub forks' class="no-zoom"></a>
  <a href="https://github.com/qimu666/autoclockin-backend" target="_blank"><img src='https://img.shields.io/github/stars/qimu666/autoclockin-backend' alt='GitHub stars' class="no-zoom"></a>
</div>

### 告知 ⚠️

1. **使用此项目造成任何损失均由个人承担。**
3. **在开始之前请帮我点一下右上角的star。**
5. **禁止任何平台或个人将此项目用于盈利或违法！**
4. **此项目仅限学习交流，禁止用于任何商业或违法用途！**

### 导航 🧭

- **[Auto Clock In 自动打卡前端代码](https://github.com/qimu666/autoclockin-frontend)**
- **[Auto Clock In 自动打卡后端代码](https://github.com/qimu666/autoclockin-backend)**

### 主要功能 🙋

1. **自动打卡**
2. **异地打卡**
3. **一键补卡**
4. **ip池代理打卡**
7. **钉钉机器人通知**
5. **打卡失败自动重试**
6. **邮件通知打卡状态**
7. **新增职校家园同款位置api**
8. ....

### 注意事项 👽

1. **打卡时间避免高峰期**
2. **设备信息尽量真实**
3. **打卡地址越详细越好**

### 快速开始 🚀

#### 前端

环境要求：Node.js >= 16

安装依赖：

```bash
yarn or  npm install
```

启动：

```bash
yarn run dev or npm run start:dev
```

部署：

- 线上环境修改 `requestConfig.ts` 文件中域名为自己的域名，本地环境修改端口号即可

```ts
baseURL: process.env.NODE_ENV === 'production' ? "https://api.qimuu.icu/" : 'http://localhost:7529/',
```

- 打包构建代码

```bash
yarn build or npm run build
```

#### 后端

启动：

1. 执行sql目录下ddl.sql

2. 修改`todo`配置
   ![image-20231010145018691](https://img.qimuu.icu/typory/image-20231010145018691.png)

服务器部署：
在本地修改配置之后，使用maven打包jar、上传jar到服务器、执行表sql、运行jar
如果您正在使用这个项目并感觉良好，或者是想支持我继续开发，您可以通过如下`任意`方式支持我：

### 支持这个项目

1. Star并分享 [Auto Clock In 自动打卡](https://github.com/qimu666/autoclockin-backend) :rocket:
2. 通过以下二维码 一次性捐款，打赏作者一杯茶。:tea:

谢谢！ :heart:

|                           微信赞赏                            |                               支付宝                               |
|:---------------------------------------------------------:|:---------------------------------------------------------------:|
| <img src="doc/qrcode/wxzs.jpg" alt="Wechat QRcode" width=180/> |  <img src="doc/qrcode/zfb.jpg" alt="Alipay QRcode" width=180/>  |

### 有趣研究社

这里有着各种有趣的、好玩的、沙雕的创意和想法以在线小网站或者文章的形式表达出来，比如：

- [小霸王游戏机](https://game.xugaoyi.com)
- [爱国头像生成器](https://avatar.xugaoyi.com/)
- [到账语音生成器](https://zfb.xugaoyi.com/)

还有更多好玩的等你去探索吧~

### 贡献 🤝

如果您想为 **[Auto Clock In 自动打卡](https://github.com/qimu666/autoclockin-backend)**
做出贡献，请随时提交拉取请求。我们始终在寻找方法来改进项目，使其对像您这样的开发者更有用。

### 联系我们 📩

如果您对 **[Auto Clock In 自动打卡](https://github.com/qimu666/autoclockin-backend)**
平台有任何问题或建议，请随时联系我们:📩邮箱：2483482026@qq.com。

感谢您使用 **[Auto Clock In 自动打卡](https://github.com/qimu666/autoclockin-backend)**   ！ 😊

### 致谢

感谢给予支持的朋友，您的支持是我前进的动力 🎉
