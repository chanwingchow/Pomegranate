# 小石榴官网

这个分支是小石榴的中文官网。应用源代码和安装包分别位于 [master 分支](https://github.com/chanwingchow/Pomegranate/tree/master) 和 [GitHub Releases](https://github.com/chanwingchow/Pomegranate/releases/latest)。

## 本地预览

在仓库根目录启动开发服务器。

```sh
bash ./gradlew :site:kobwebStart -t
```

打开 [localhost:8080](http://localhost:8080)。也可以在 `site` 目录使用 `kobweb run`。

## 静态导出

在 `site` 目录中运行 `kobweb export --layout static`。也可以在仓库根目录用 Gradle 导出。

```sh
bash ./gradlew :site:kobwebExport -PkobwebReuseServer=false -PkobwebEnv=DEV \
  -PkobwebRunLayout=FULLSTACK -PkobwebBuildTarget=RELEASE -PkobwebExportLayout=STATIC
```

输出目录为 `site/.kobweb/site`。若网站部署在子路径下，可在 `site/.kobweb/conf.yaml` 中设置 `site.basePath` 后重新导出。

上线时，在仓库的 Pages 设置中将发布来源选为 GitHub Actions。推送 `website` 分支后，工作流会读取 Pages 的实际路径，并发布静态产物。

页面从 GitHub 公开 API 读取最新发布包。设备识别在浏览器中完成，系统或架构无法确认时可以手动选择。API 请求失败时，下载区域会提供发布页链接。截图来自主分支的演示模式。

平台图标来自 [Font Awesome Free 6.5.2](https://github.com/FortAwesome/Font-Awesome/tree/6.5.2/svgs/brands)，由 Fonticons, Inc. 以 [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) 授权。
