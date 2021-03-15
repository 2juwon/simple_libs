package devdan.libs.base.utils

import java.io.File

object FileUtil {
    fun delete(path: String) {
        val file = File(path)
        if(file.exists()) {
            file.delete()
        }
    }
}