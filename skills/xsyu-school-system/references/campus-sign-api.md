# Campus identity and sign APIs

Campus base host: `https://gwxg.xsyu.edu.cn`.

## Current project sign flow

The project primarily uses the newer `/sign/mobile/` endpoints:

| Purpose | Method and path | Main inputs |
| --- | --- | --- |
| Recent sign list | `GET /sign/mobile/receive/getMySignLogs` | `page`, `size` |
| Sign detail | `GET /sign/mobile/receive/getSignLog` | `signId`, `schoolId` |
| Area sign | `POST /sign/mobile/receive/doSignByArea` | activity ID, sign ID, school ID, position/area data |

Requests require a valid JWSESSION header. The external platform commonly uses `code:0` for success, while XSYU Sign wraps results into its own `Result` convention. Do not mix the two code systems.

Before changing request fields, inspect:

- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/webClient/BaseSignService.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/service/impl/SignServiceImpl.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/pojo/entity/SignItem.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/pojo/DTO/SignDTO.java`

The older H5 application used `/h5/mobile/basicinfo/sign/getAllSign` and `/h5/mobile/basicinfo/sign/sign`. Treat those as historical compatibility references, not preferred endpoints.

## Identity endpoint

`POST /basicinfo/mobile/my/index` with `{}` returns the current school identity and is also used to verify that an SMS/QR session belongs to the requested student number. Important fields observed include `username`, `number`, `name`, `phone`, college, major, classes, dorm, school IDs, type, and year.

## Discovered endpoint families

These names were extracted from school H5 JavaScript and are reconnaissance, not guaranteed stable contracts.

### Student sign receiving

```text
/sign/mobile/receive/
doSignByArea doSignByHelp doSignByQrcode getMySignLogs getQrcode
getSignLog getSignLogByQrCode getSignLogs getSignPeoples getSignQrcode
getSignStatistics updateState
```

### Sign publishing and teaching

```text
/sign/mobile/publish/
createSign createSignTask closeLeaderSign closeSignTask deleteSign deleteSignTask
endSign openSignTask updateSign updateSignTask updateState
getAreaList getClassesList getClassesSelect getDeviceList getGroupList getQrcode
getSelectValue getSignClasses getSignPeoples getSignRelatedStatistics getSignState
getSignStatistics getSignsByMine getSignsByRelated getSignsTask getStudentAuth
getTempList getUserEmail remind remindByRelated
```

### Other sign families

```text
/sign/mobile/device/     bindDevice getMyDevices removeDevice
/sign/mobile/manage/     getClassesList getCollegeList getRole getSelectValue getSignStatistics getSigns remind
/sign/mobile/signExcel/  reportDaysForRelated reportForManage reportForPublish reportForRelated
```

### Basic information families

```text
/basicinfo/mobile/home/       index createPassword getDataApps getHomeApps getIsFinishMsg getIsTeacher getJSSdkSign queryBannerList unicode
/basicinfo/mobile/my/         index app changePassword getBind getCode getSchoolNeedFills updateAuthcode updateExt updatePhone updateStudent updateTeacher
/basicinfo/mobile/login/      username changePassword getCode
/basicinfo/mobile/register/   authcode getClassesData getCode
/basicinfo/mobile/selectUser/ getGroups getPosts getStudents getTeachers getTeams getUsersByKeyword
/basicinfo/mobile/department/ createCollege createMajor deleteCollege deleteMajor getColleges getDeptments getMajors updateCollege updateMajor
/basicinfo/mobile/appMarket/  addUser deleteAppUser getAppUsers getApps getUser update
```

Additional observed families cover graduate/student/teacher data, mobile user types, address books, jobs, news, notices, joining activities, and imitation login.

## External-system discipline

- Reconfirm current request and response shapes before implementing a newly discovered endpoint.
- Avoid bulk crawling or destructive teacher/publisher calls without explicit user authorization.
- Preserve raw school error code/message for diagnosis, but redact session credentials and personal data from logs.
- If the school returns code 103, renew JWSESSION at most through the project’s bounded refresh path; do not create retry loops around a failed login.
