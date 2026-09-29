package com.chow.pomegranate.data.local

/**
 * 应用偏好键。
 */
object PreferenceKeys {
    /** 主题模式，取值为 `ThemeMode` 的下标。 */
    const val THEME_MODE = "theme_mode"

    /** 课表周次，第 0 周表示开学前一周。 */
    const val WEEK = "week"

    /** 性别，取值为 `Gender` 的下标。 */
    const val GENDER = "gender"

    /** 校区，取值为 `Campus` 的下标。 */
    const val CAMPUS = "campus"

    /** 主题色，取值为颜色的十六进制字符串。 */
    const val PRIMARY_COLOR = "primary_color"

    /** 最近打开的模块。 */
    const val LAST_MODULE = "last_module"

    /** 本机头像文件地址；这不是学校账户的远端头像。 */
    const val LOCAL_AVATAR_URI = "local_avatar_uri"
}
