package com.jsuereth.sbtsite

import sbt._
import Keys._


object SiteKeys {
  val makeSite = TaskKey[File]("make-site", "Generates a static website for a project.")
  // Helper to point at mappings for the site.
  val siteMappings = mappings in makeSite
  val siteDirectory = target in makeSite
  val siteSources = sources in makeSite
  val siteSourceDirectory = sourceDirectory in makeSite
}


object SitePlugin extends Plugin {
  object site {
    // TODO - Site key references here  
    import SiteKeys._
    val settings = Seq(
      siteMappings := Seq[(File,String)](),    
      addMappingsToSiteDir(mappings in packageDoc in Compile, "latest/api"),
      siteDirectory <<= target(_ / "site"),
      siteSourceDirectory <<= sourceDirectory(_ / "site"),
      // TODO - Just include everything?
      sourceFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf",
      siteMappings <++= (sourceFilter in makeSite, siteSourceDirectory) map { (incs, dir ) =>
        dir ** incs x relativeTo(dir)
      },
      makeSite <<= (siteDirectory, siteMappings) map { (dir, maps) =>
        // TODO - Lazier way to do this.
        for((file, target) <- maps) {
          val tfile = dir / target
          if(file.isDirectory) IO.createDirectory(tfile)
          else IO.copyFile(file,tfile)
        }
        dir      
      }
    )
    /** Convenience functions to add a task of mappings to a site under a nested directory. */
    def addMappingsToSiteDir(mappings: ScopedTask[Seq[(File,String)]], nestedDirectory: String) =
      siteMappings <++= mappings map { m =>
        for((f, d) <- m) yield (f, nestedDirectory + "/" + d)
      }
  }
  // Note: We include helpers so other plugins can 'plug in' to this one without requiring users to use/configure the site plugin.
  override val settings = Seq(
    SiteKeys.siteMappings <<= SiteKeys.siteMappings ?? Seq[(File,String)]()
  )
}
