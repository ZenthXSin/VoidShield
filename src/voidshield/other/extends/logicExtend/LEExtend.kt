package voidshield.other.extends.logicExtend

import mindustry.gen.Building
import mindustry.logic.LExecutor
import mindustry.logic.LVar
import kotlin.math.floor

/**
 * @author m1cxzfw3q
 */
object LEExtend {
    @JvmStatic
    fun safeToString(`var`: LVar?): String {
        // 处理 null 输入
        if (`var` == null) {
            return "null"
        }
        val obj = `var`.obj() ?: return if (!`var`.isobj) {
            floor(`var`.num()).toString()
        } else "null"

        return obj.toString()
    }

    @JvmStatic
    fun sanitize(value: String): String {
        if (value.isEmpty()) {
            return ""
        } else if (value.length == 1) {
            if (value[0] == '"' || value[0] == ';' || value[0] == ' ') {
                return "invalid"
            }
        } else {
            val res = StringBuilder(value.length)

            if (value[0] == '"' && value[value.length - 1] == '"') {

                res.append('"')

                for (i in 1 until value.length - 1) {
                    if (value[i] == '"') {
                        res.append('\'')
                    } else {
                        res.append(value[i])
                    }
                }

                res.append('"')
            } else {
                for (i in value.indices) {
                    val c = value[i]
                    res.append(
                        when (c) {
                            ';' -> 's'
                            '"' -> '\''
                            ' ' -> '_'
                            else -> c
                        }
                    )
                }
            }

            return res.toString()
        }

        return value
    }

    @JvmStatic
    fun appendLStmt(str: StringBuilder, name: String, vararg appends: String) {
        str.append(name)
        for (s in appends) {
            str.append(" ").append(s)
        }
    }
}
