package com.twitter.scrooge.linter

sealed trait LintLevel

case object Warning extends LintLevel
case object Error extends LintLevel
