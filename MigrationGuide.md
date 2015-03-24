Migrating from Scrooge 3.x to 3.18.0
=======================================

## Sbt 0.13.5 and above

Autoplugins are available only for sbt 0.13.5 and above.


## Projects using  `build.sbt`

```scala

import com.twitter.scrooge.ScroogeSBT

lazy val app = project.in(file("app"))
  .settings(ScroogeSBT.newSettings: _*)
  .settings(
    // more settings here
  )

```

It now should look like

```scala

lazy val app = project.in(file("app"))
  .settings(
    // more settings here
  )

```

This is now done automatically, by virtue of the autoplugin mechanism for all JvmPlugin projects.

If you use a setting, this is in scope automatically as well so

```scala

import com.twitter.scrooge.ScroogeSBT

lazy val app = project.in(file("app"))
  .settings(ScroogeSBT.newSettings: _*)
  .settings(
    ScroogeSBT.scroogeThriftSourceFolder in Compile <<= baseDirectory {
      base => base / "src/main/resources"
    }
  )

```

becomes

```scala

lazy val app = project.in(file("app"))
  .settings(
    scroogeThriftSourceFolder in Compile <<= baseDirectory {
      base => base / "src/main/resources"
    }
  )

```

## Using `Build.scala`

The big change here as well as not needing to inject settings is that the location of the keys has changed. They are now under an object called autoImport.

```scala

import sbt._
import Keys._

import com.twitter.scrooge.ScroogeSBT._

object build extends Build {

  lazy val app = project.in(file("app"))
    .settings(newSettings: _*)
    .settings(
      scroogeThriftSourceFolder in Compile <<= baseDirectory {
        base => base / "src/main/resources"
      }
    )
}
```

becomes

```scala

import sbt._
import Keys._

import com.twitter.scrooge.ScroogeSBT._
import com.twitter.scrooge.ScroogeSBT.autoImport._

object build extends Build {

  lazy val app = project.in(file("app"))
    .settings(
      scroogeThriftSourceFolder in Compile <<= baseDirectory {
        base => base / "src/main/resources"
      }
    )
}

```

That is to say: adjust the imports, and drop the settings injection.
