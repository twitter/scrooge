package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.ScalaGeneratorFactory

class ScalaImmutableSequencesGoldFileTest extends GoldFileTest {

  override protected def language: String = ScalaGeneratorFactory.language
  override protected def languageFlags: Seq[String] = Seq("immutable-sequences")
  override protected def goldFilesRoot: String = s"gold_file_output_${language}_immutable_sequences"

}
