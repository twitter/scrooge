package com.twitter.scrooge.linter

case class LintMessage(msg: String, level: LintLevel = Warning)
