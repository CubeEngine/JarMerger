package service

import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

import play.api.Configuration

object Util {

    def md5(string: String): String = {
        val md5Hasher = MessageDigest.getInstance("md5")
        md5Hasher.digest(string.getBytes("UTF-8")).map("%02X" format _).mkString
    }

    def folderOrDefault(conf: Configuration, key: String, default: String): Path = {
        val path = conf.getString(key) match {
            case Some(p) => Paths.get(p)
            case None => Paths.get(default)
        }
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
        path
    }

    def tempDir(conf: Configuration) = folderOrDefault(conf, "temp-path", "temp")
}
