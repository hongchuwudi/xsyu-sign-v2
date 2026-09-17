# 西安石油大学校园系统接口分析（实测）

> 实测时间：2026-09-13 ~ 2026-09-14
> 分析对象：统一身份认证系统（ids.xsyu.edu.cn）、学工系统"我在校园"（gwxg.xsyu.edu.cn）
> 方式：抓取登录页 HTML/JS 源码 + 大量 curl 实测请求

---

## 一、CAS 统一身份认证系统（ids.xsyu.edu.cn）

### 1. 登录页结构

```
GET https://ids.xsyu.edu.cn/authserver/login?service=<URL编码的目标地址>
```

响应：`200 OK`（约 47KB HTML），Set-Cookie: `SESSION=<uuid>; Path=/authserver/; HttpOnly`

页面内包含 **4 个表单**：

| 表单 | 用途 | loginType |
|------|------|-----------|
| fm1 | 密码登录 | 1 |
| fm2 | 短信登录 | 2 |
| credential（fm3） | 刷脸登录 | 4 |
| （tab3） | 二维码登录 | 无表单，轮询 analogLogin |

关键页面变量（内联 JS）：

```js
var contextPath = "/authserver";
var ajaxLoginUrl = "/authserver/analogLogin";
var frontLoginType = 1;              // 默认密码登录
var qrCodeUrl = "/authserver/generateQRCode?loginLT=<49字符uuid+时间戳>";
```

页面引用的 JS：

| 文件 | 作用 |
|------|------|
| `/authserver/js/cas.js` | CAS 通用逻辑（检测浏览器类型等） |
| `/authserver/themes/sudy_xsyu/js/login.js` | 登录页主题逻辑（表单提交、短信/刷脸/二维码流程、RSA 公钥） |
| `/authserver/js/security.js` | RSA 加密库（JSEncrypt 风格，RSAUtils） |

### 2. 密码登录（loginType=1）

**表单字段**（POST `/authserver/login?service=...`）：

| 字段 | 说明 |
|------|------|
| username | 学号/工号 |
| password | 密码（hidden 输入框，提交前被 RSA 密文覆盖） |
| execution | 页面隐藏字段（每次页面加载重新生成，一次性） |
| _eventId | 固定 `submit` |
| loginType | 固定 `1` |
| rememberMe | `true` |
| encrypted | hidden 字段 value=`true`，标记密码为加密格式 |
| authcode | 验证码（仅风控触发时存在） |

**RSA 加密细节**（login.js 中硬编码）：

```js
// 触发条件：页面存在 id="encrypted" 元素 且 密码长度 != 256（未被加密过）
RSAUtils.setMaxDigits(131);
var key = RSAUtils.getKeyPair(
    "010001",   // 指数 e = 65537
    '',
    "008aed7e057fe8f14c73550b0e6467b023616ddc8fa91846d2613cdb7f7621e3cada4cd5d812d627af6b87727ade4e26d26208b7326815941492b2204c3167ab2d53df1e3a2c9153bdb7c8c2e968df97a5e7e01cc410f92c4c2c2fba529b3ee988ebc1fca99ff5119e036d732c368acf8beba01aa2fdafa45b21e4de4928d0d403"  // 1024位模数
);
var result = RSAUtils.encryptedString(key, password);  // 输出 256 字符 hex
```

> **实测**：直接提交**明文密码**（不带 encrypted 参数）也被服务器接受——返回的是"账号或密码错误"而非格式错误，说明服务器端同时兼容明文与 RSA 密文。

**响应**：
- 成功 → `302`，Location 为 `service?ticket=ST-xxx`
- 失败 → `200`，`<span id="msg1">` 中为错误信息，页面同时包含**新的 execution 值**

### 3. 验证码机制

**触发条件（实测）**：同一 IP/会话约 **3 次失败登录**后激活；触发后新建会话的登录页直接带验证码（IP 级风控标记）。

**激活标记**：页面 HTML 中出现 `authcode` 字符串（激活时 2 次；未激活时 0 次——未激活页面的验证码 div 在 HTML 注释里且 input 没有 id/name 属性）。

激活时的验证码区域：

```html
<input id="authcode" name="authcode" tabindex="3" type="text"
       class="auth_input paw_input" placeholder="验证码"/>
<img class="validatecode_image" src="/authserver/captcha.jpg"
     onclick="refreshCaptcha(this);"/>
```

- 验证码图片：`GET /authserver/captcha.jpg`（带 SESSION cookie），**60x20 JPEG，4 位数字**，带噪点
- 刷新：`/authserver/captcha.jpg?tt=<随机数>`
- 有效期：≥ 30 秒（实测 24 秒内提交有效）

> **关键实测结论**：验证码提交字段名是 **`authcode`**，服务器**完全忽略 `captcha=` 参数**——用 `captcha=<正确码>` 提交永远返回"验证码信息无效"，用 `authcode=<正确码>` 则通过校验进入下一步（实测响应从"验证码信息无效"变为"账号被锁定"，证明验证码校验已通过）。

验证码通过后才检查账号密码。

### 4. 风控机制（实测阈值）

| 失败次数 | 现象 |
|----------|------|
| 0 ~ 2 | 正常提示"账号或密码错误" |
| ~3 | 验证码激活（IP + 会话级，后续登录必须输验证码） |
| ~5-6 | **账号锁定**：`您的账号被锁定，请联系管理员。`（锁定期间即使验证码正确也无法登录，需等待解锁） |

### 5. 短信登录（loginType=2）

**流程**：

```
① POST /authserver/smsLogin/sendSms
   Headers: X-Requested-With: XMLHttpRequest, Content-Type: application/x-www-form-urlencoded
   Body:    request_username=<手机号>
   成功:    {"success":true}          失败: {"success":false,"errormsg":"错误信息"}
   冷却:    60 秒（前端 smsFreque 倒计时，服务器侧同样限制）

② 用户输入短信验证码后（不直接提交表单）：
   GET /authserver/maccountNoLoginValid/smsValid?phone=<手机号>&smsCode=<验证码>
   响应: {"code":1, "isExitMultipleAccount":bool,
          "data":[{"code":"学号","name":"姓名","category":"身份(如 教职工)",
                    "photo":"base64 jpeg","isDefault":bool}], "msg":"..."}

③ 分支：
   - code != 1              → 校验失败（msg 为错误原因，如验证码错误/过期）
   - code==1 且 单账号       → CAS 会话已认证 → 重载登录页 → 302 + ticket
   - code==1 且 多账号       → 弹窗选择账号（有默认账号则自动选默认）：
       GET /authserver/maccountNoLoginValid/accountValid?loginName=<学号>&isDefault=<bool>
       响应: {"code":0,"msg":"失败原因"} 为失败；code != 0 为成功
       成功后 location.reload() → 重载登录页 → 302 + ticket
   - 多账号但列表为空         → 弹窗要求手动输入账号（同样走 accountValid）
```

### 6. 扫码登录

**流程**：

```
① 登录页内联变量 qrCodeUrl = /authserver/generateQRCode?loginLT=<49字符>
② GET <qrCodeUrl>（带 SESSION cookie）→ 200x200 PNG 二维码
③ 用户用手机校园 APP 扫码并确认（APP 端已登录态）
④ 轮询 POST /authserver/analogLogin（空 body，X-Requested-With: XMLHttpRequest）
   每 2 秒一次（前端 setInterval(loginRequest, 2000)）
   未扫码:   HTTP 200，响应体为空
   已确认:   响应体 "success"（另有 "userlimit" 等状态）
⑤ 收到 success 后重载登录页（location.reload()）→ 会话已认证 → 302 + ticket
```

### 7. 刷脸登录（loginType=4）

- 调摄像头（getUserMedia），前端人脸检测
- `POST /cas/faceValid/checkHumanImg` 上传/校验人脸帧
- 提交表单字段：`faceData`（人脸数据）、`face_username`、`loginType=4`
- cookie `sudy_face` 记录失败次数，失败 3 次后需重新选择登录方式

### 8. ticket 换取 JWSESSION（登录成功链）

```
① CAS 登录成功 → 302 Location: <service>?ticket=ST-xxx
② GET <service>?ticket=ST-xxx → gwxg 后端向 CAS 校验 ticket
   → Set-Cookie: JWSESSION=<值>（域 gwxg.xsyu.edu.cn）
   → 302 跳转到系统首页 /h5/mobile/basicinfo/index
③ 后续所有学工系统接口用 JWSESSION 鉴权
```

- 无 ticket 访问 service → 302 重定向到 CAS 登录页（带 service 参数）
- 无效 ticket → `500 {"timestamp":...,"status":500,"error":"Internal Server Error","message":"","path":"/basicinfo/mobile/login/casLogin"}`

### 9. 错误信息速查（`<span id="msg1">`）

| 错误文本 | 含义 |
|----------|------|
| `账号或密码错误。` | 用户名或密码错误 |
| `验证码信息无效。` | 验证码错误/过期/未提交（authcode 字段） |
| `您的账号被锁定，请联系管理员。` | 账号被风控锁定 |

> **坑**：所有登录页 HTML（含注释掉的 tab 标签 `<h3>验证码登录</h3>`）都包含"验证码"字样，**不能**用 `contains("验证码")` 判断是否需要验证码；可靠标记是 `authcode` 字符串的出现。

### 10. 其他登录入口

| 入口 | 说明 |
|------|------|
| QQ 授权登录 | `toqqLogin()` 打开 QQ 授权窗口（qqAuthorizeURL） |
| 微博授权登录 | `toweiboLogin()`（wbAuthorizeURL，手机端跳转/PC 端弹窗） |
| WeLink 扫码登录 | `postMessage` 监听 `https://login.welink.huaweicloud.com` 消息 → `https://login.welink.huaweicloud.com/sso/oauth2/sns_authorize?client_id&redirect_uri&code=...` |
| 木鱼小程序扫码 | `muyuQrcode`，`muUrl + /cgi-bin/wxlogin/getssouuid.php` |

### 11. Cookie 与会话

| Cookie | 域 | 说明 |
|--------|-----|------|
| SESSION | ids.xsyu.edu.cn，Path=/authserver/，HttpOnly | CAS 会话；execution 与验证码绑定此会话 |
| JWSESSION | gwxg.xsyu.edu.cn | 学工系统会话凭证（登录成功后签发） |

- execution：页面每次加载生成新值，与 SESSION 绑定，提交失败后响应页会携带新 execution
- 请求头需带浏览器 UA；POST 登录需带 `Referer: https://ids.xsyu.edu.cn/authserver/login?service=...`

---

## 二、学工系统 gwxg.xsyu.edu.cn（我在校园）

### 1. CAS 登录回调

```
https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin
```

即 CAS 的 service 参数值。`?ticket=ST-xxx` 时校验 ticket 并签发 JWSESSION。

### 2. JWSESSION 使用方式

- **存储**：cookie + localStorage（h5 前端 `jw` SDK 双写）
- **API 鉴权**：HTTP 请求头 `jwsession: <值>`（大小写不敏感；项目实测用 `JWSESSION` 头同样有效）
- 未登录访问 API → `{"code":103,"message":"未登录,请重新登录"}`
- 有效期约一周（项目按周续签）

### 3. 用户信息接口（获取当前会话的学号/姓名）

```
POST https://gwxg.xsyu.edu.cn/basicinfo/mobile/my/index
Headers: JWSESSION: <值>, Content-Type: application/json
Body:    {}
```

成功响应（data 完整字段：area, avatar, classes, classesId, college, degree, dorm, dormId, email, gender, graduate, id, inSchool, isFullInfo, location, locationId, major, manage, name, nation, number, oldType, periods, phone, school, schoolId, studentDataRights, teacherDataRights, teacherId, teacherName, type, typeName, username, wxmpOpenId, year）：

```json
{"code":0, "data":{"username":"学号/工号", "number":"学号/工号", "name":"姓名", "phone":"手机号", ...}}
```

**注意：学号字段是 `username`（`number` 同值），不是 `code`**（`code` 只出现在短信多账号接口的账号列表里）。

用途：短信/扫码登录后确认会话归属的学号（防输错/存错人）。

### 4. 签到相关接口

**项目在用的（/sign/mobile/ 前缀）**：

| 接口 | 方法 | 参数 |
|------|------|------|
| `/sign/mobile/receive/getMySignLogs` | GET | page, size |
| `/sign/mobile/receive/getSignLog` | GET | signId, schoolId |
| `/sign/mobile/receive/doSignByArea` | POST | 区域签到参数（id/经纬度/areaJson） |

**h5 版本（/h5/mobile/basicinfo/ 前缀，旧脚本使用）**：

| 接口 | 方法 | 参数 |
|------|------|------|
| `/h5/mobile/basicinfo/sign/getAllSign` | GET | page, size |
| `/h5/mobile/basicinfo/sign/sign` | GET/POST | id, signId, schoolId |

### 5. 完整 API 目录（从 h5 前端 JS chunks 提取）

统一响应格式：`{"code":0成功,"data":...}`（未登录 103）。

**/sign/mobile/receive/（学生端签到）**：

```
doSignByArea  doSignByHelp  doSignByQrcode  getMySignLogs  getQrcode
getSignLog  getSignLogByQrCode  getSignLogs  getSignPeoples  getSignQrcode
getSignStatistics  updateState
```

**/sign/mobile/publish/（发布端/教师端）**：

```
createSign  createSignTask  closeLeaderSign  closeSignTask  deleteSign  deleteSignTask
endSign  openSignTask  updateSign  updateSignTask  updateState
getAreaList  getClassesList  getClassesSelect  getDeviceList  getGroupList  getQrcode
getSelectValue  getSignClasses  getSignPeoples  getSignRelatedStatistics  getSignState
getSignStatistics  getSignsByMine  getSignsByRelated  getSignsTask  getStudentAuth
getTempList  getUserEmail  remind  remindByRelated
```

**/sign/mobile/device/**：`bindDevice  getMyDevices  removeDevice`

**/sign/mobile/manage/**：`getClassesList  getCollegeList  getRole  getSelectValue  getSignStatistics  getSigns  remind`

**/sign/mobile/signExcel/**：`reportDaysForRelated  reportForManage  reportForPublish  reportForRelated`

**/basicinfo/mobile/home/**：`index  createPassword  getDataApps  getHomeApps  getIsFinishMsg  getIsTeacher  getJSSdkSign  queryBannerList  unicode`

**/basicinfo/mobile/my/**：`index  app  changePassword  getBind  getCode  getSchoolNeedFills  updateAuthcode  updateExt  updatePhone  updateStudent  updateTeacher`

**/basicinfo/mobile/login/**：`username  changePassword  getCode`

**/basicinfo/mobile/register/**：`authcode  getClassesData  getCode`

**/basicinfo/mobile/selectUser/**：`getGroups  getPosts  getStudents  getTeachers  getTeams  getUsersByKeyword`

**/basicinfo/mobile/department/**：`createCollege  createMajor  deleteCollege  deleteMajor  getColleges  getDeptments  getMajors  updateCollege  updateMajor`

**/basicinfo/mobile/appMarket/**：`addUser  deleteAppUser  getAppUsers  getApps  getUser  update`

**/basicinfo/mobile/data/**：`graduate/classes  graduate/getClassesUsers  graduate/getCollegeList  graduate/getYears  student/classes  student/college  student/getClassesUsers  student/getTypeStudents  teacher/college  teacher/getTeacherList  teacher/getTeachersExcludeNormals`

**/basicinfo/mobile/mobileManageUserType/**：`confirmStudentType  getClassesUsers  getStudentUserTypes  userTypeUpdateLog`

**/basicinfo/mobile/teacherAddressBook/**：`classes/*  group/*  post/searchTeacher  team/*`

**其他**：`/basicinfo/mobile/imitate/login`（模拟登录）、`/basicinfo/mobile/job/getJobViewList.json`、`/basicinfo/mobile/join/getData|join`、`/news/mobile/notice/list|listByHome|addCount`、`/notice/mobile/student/getAllStudentLeaveType`

### 6. h5 前端

- 产品名："我在校园"（wozaixiaoyuan），Vue 2 + Vant + axios
- 静态资源：`https://jwossxg.xsyu.edu.cn/h5/mobile/sign/0.3.6/`（签到）与 `.../basicinfo/4.8.3/`（基础信息）
- 公共 SDK：`/h5/mobile/common/jsapi/JWJSApi_*.js`（jw.request 封装，自动带 jwsession 头并处理未登录）

---

## 三、实测结论与踩坑记录（对集成最重要）

1. **验证码字段名**：`authcode`，不是 `captcha`。`captcha=` 参数被服务器静默忽略，永远报"验证码信息无效"。
2. **验证码激活检测**：以页面是否含 `authcode` 为准（`contains("验证码")` 会误判——注释里也有该词）。
3. **错误诊断顺序**：①"锁定"→账号锁定 ②含 `authcode`→需要验证码 ③`<span id="msg1">` 文本含"错误/不正确/不存在/无效"→账号或密码错误。
4. **风控阈值**：~3 次失败触发验证码（IP+会话级）；~5-6 次失败账号锁定。锁定期间任何方式都无法登录，**连续重试会延长风控**。
5. **密码明文可用**：服务器兼容明文与 RSA 密文提交（未携带 encrypted 参数时按明文处理）。
6. **execution 一次性**：每次 200 错误响应都会带新 execution，重试时必须用最新的。
7. **短信登录不做表单提交**：真实流程是 sendSms → smsValid（校验+建会话）→ [多账号时 accountValid] → 重载登录页拿 ticket，而不是提交 loginType=2 表单。
8. **扫码轮询响应**：`analogLogin` 空响应=等待，"success"=已确认；确认后需重载登录页拿 ticket。
9. **学号获取**：短信/扫码登录不返回学号，需用 JWSESSION 调 `/basicinfo/mobile/my/index` 的 `data.code` 获取并校验。
10. **验证码 TTL**：≥30 秒（图片获取后需尽快提交）；验证码图片绑定 SESSION cookie。
11. **`set-cookie` 是全小写响应头**（ids.xsyu.edu.cn 经 Envoy 代理）：解析响应头时**必须大小写不敏感**。Java `HttpURLConnection.getHeaderFields().get("Set-Cookie")` 是大小写敏感的，会取不到导致 cookie 静默丢失——实测 ids 服务器返回 `set-cookie`（小写），本项目曾因此导致 SESSION cookie 从未被捕获，短信登录全程无会话必然失败（学校报"认证信息无效"）。正确写法：遍历 `getHeaderFields()` 用 `equalsIgnoreCase("Set-Cookie")` 匹配。`getHeaderField("Set-Cookie")` 本身是大小写不敏感的（可用于单个取值）。
12. **短信登录各步骤必须在同一 SESSION 会话内**：sendSms 生成的验证码绑定当时会话，smsValid 必须携带同一 SESSION cookie，否则报"认证信息无效"。
13. **ticket → JWSESSION 重定向链较长且有"弹回"现象**：实测链路为 ticket →(302, Set-Cookie JSESSIONID)→ casLogin →(302, 弹回 CAS)→ CAS →(302, 新 ticket)→ casLogin?ticket →(302)→ casLogin →(302, Set-Cookie JWSESSION)→ h5首页，共 5-8 跳。**首次访问 casLogin 常被弹回 CAS 重新取票**，跟随重定向的深度上限不能太小（曾因上限 5 导致"重定向链过长"失败，现放宽到 12）。JWSESSION 在倒数第二跳（casLogin 无 ticket）下发，同时下发 WZXYSESSION（domain=wozaixiaoyuan.com）与同域 JWSESSION（domain=xsyu.edu.cn）。
14. **CAS 侧还有 CASTGC cookie**（rememberMe 票据）：拿到后可免密获取新 ticket（GET 登录页 service URL → 302 + ticket），可用于测试而无需重复登录。
