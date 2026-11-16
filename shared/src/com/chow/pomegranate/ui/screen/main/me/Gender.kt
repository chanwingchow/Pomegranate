package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Face4
import androidx.compose.material.icons.twotone.FaceRetouchingNatural
import androidx.compose.ui.graphics.vector.ImageVector
import com.chow.pomegranate.Res
import com.chow.pomegranate.boy
import com.chow.pomegranate.girl
import org.jetbrains.compose.resources.StringResource

/**
 * 性别。
 *
 * @property label 性别名称
 * @property icon 图标
 */
enum class Gender(
    val label: StringResource,
    val icon: ImageVector,
) {
    /** 女生 */
    Girl(
        label = Res.string.girl,
        icon = Icons.TwoTone.Face4,
    ),

    /** 男生 */
    Boy(
        label = Res.string.boy,
        icon = Icons.TwoTone.FaceRetouchingNatural,
    ),
    ;

    /** 切换到另一个性别。 */
    fun toggle(): Gender = if (this == Girl) Boy else Girl

    companion object {
        /** 默认性别。 */
        val Default = Girl
    }
}
