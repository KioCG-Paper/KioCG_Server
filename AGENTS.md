# 项目：KioCG（Paper 服务端 fork）

这是一个基于 Paper 的 Minecraft 服务端 fork，用 paperweight-patcher 维护。 两个模块：kiocg-api（Paper API
fork）、kiocg-server（服务端 fork）。

补丁机制：paperweight 把上游 Paper 检出成 paper-server/、paper-api/，把反编译的 Minecraft 源码放进
kiocg-server/src/minecraft/，再套用本仓库的补丁。 **本仓库所有补丁文件都是自动生成的产物，不是手写的。**

# 版本相关

上游版本由 gradle.properties（paperRef、mcVersion）和构建脚本确定，是可查的事实。

- 需要确认 API/行为时，直接读这些配置或对应源码，不要凭印象报版本号。
- 确认版本之后，就可以按这个版本的 API/行为来写代码，不用为兼容其他版本做无谓的抽象或兜底。
- 项目会跟进上游，paperRef 可能被更新；如果你改了依赖上游行为的代码，说明清楚它依赖的是哪个版本的行为。

# 铁律

1. 只改源码，不改补丁。 可以编辑：paper-server/ **、paper-api/**、kiocg-server/src/minecraft/ **、
   kiocg-server/src/main/java/**、以及生成出来的两个 build.gradle.kts。 不要编辑：kiocg-server/paper-patches/
   **、kiocg-server/minecraft-patches/**、 kiocg-api/paper-patches/**、*.kts.patch。改了会被覆盖，还会造成补丁与源码不一致。
2. 以源码为准。检查代码时不要核对“源码和补丁是否一致”，也不要因为补丁里没有某段代码就回头改补丁。
3. 不要跑任何 Gradle 任务，包括 rebuildAllPatches、applyAllPatches、build、runServer、createPaperclipJar。 构建、运行、重建补丁、提交
   git 都由我在 IDEA 里处理。 尤其 applyAllPatches 会用旧补丁覆盖源码，可能直接丢掉你的改动。

# 目录速查

| 路径                                    | 内容                                                  |
|-----------------------------------------|-------------------------------------------------------|
| paper-server/src/main/java/**           | Paper 服务端源码（含 NMS 侧改动），小改动直接写在这里 |
| kiocg-server/src/minecraft/java/**      | 反编译的 Minecraft 源码，同上                         |
| kiocg-server/src/minecraft/resources/** | Minecraft 数据/资源（配方、战利品表等），同上         |
| paper-api/src/main/java/**              | Paper API 源码                                        |
| kiocg-server/src/main/java/com/kiocg/** | 独立功能的落点，不参与补丁，改动不经过上游源码        |
| DATAPACKS/                              | 独立的数据包成品，不参与构建，不要当源码改            |

# 代码怎么放（重要）

改动落在哪里看性质，不要一律外移到 com.kiocg：

- 小改动就写原处。改个数值/常量、加个判断、调整掉落/经验/配方/消息、加个开关、给现有方法补一段逻辑—— 直接改 paper-server/ 或
  src/minecraft/ 对应的那个文件即可。不要为了“少碰上游”而另外建类、 加事件、加回调、加静态工具方法绕一圈。
- 复杂且独立的功能才单独成模块，放 kiocg-server/src/main/java/com/kiocg/**。
  典型如：新命令、新机制、需要自己状态和生命周期的系统（任务、管理器、自定义世界生成、附魔实现等）。
  这时上游侧只留最薄的调用点（一行调用 + 注释标记），逻辑都在 com.kiocg 里。
- 判断标准：如果外移之后，原处只剩一两行转发、还得多加构造函数/静态入口/API 才能接通，
  就说明这个改动本该写回原处。反之，如果逻辑成套、会被多处复用、或需要在服务器启动/关闭时挂生命周期，就该独立出来。

# 代码约定

- 保持与上游的最小 diff：不要重排、重格式化、顺手清理无关代码。
- 功能标记只加在 Paper / Minecraft 的上游类里（paper-server/ **、paper-api/**、src/minecraft/**， 以及 build.gradle.kts
  这类上游文件）：单行 `// KioCG - <简述>`； 成块改动用 `// KioCG start - <简述>` / `// KioCG end - <简述>`。
- 自己在 com.kiocg 包下新增的类、以及写在这个包里的代码，属于本 fork 自有实现，不要加 KioCG 标记。
- 改完给出改动位置和思路即可，不需要编译验证或跑测试。

# 回答方式

- 说清改在哪个源码文件、为什么放这里（原处还是独立模块）。
- 不确定的架构问题先问，普通修复/重构不必逐条确认。
