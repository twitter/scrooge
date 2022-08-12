#!/bin/bash

set -euxo pipefail

# this script is for overwriting gold files when template changed, usage:
# $source ./scrooge/scrooge-generator-tests/src/test/resources/gen_gold_files.sh
javaTarget="//scrooge/scrooge-generator-tests/src/test/thrift/goldfile:thrift-java"
scalaTarget="//scrooge/scrooge-generator-tests/src/test/thrift/goldfile:thrift-scala"

scalaJar="bazel-bin/scrooge/scrooge-generator-tests/src/test/thrift/goldfile/thrift--thrift-root_scrooge_scala.srcjar"
javaJar="bazel-bin/scrooge/scrooge-generator-tests/src/test/thrift/goldfile/thrift--thrift-root_scrooge_java.srcjar"
scalaDest="scrooge/scrooge-generator-tests/src/test/resources/gold_file_output_scala"
javaDest="scrooge/scrooge-generator-tests/src/test/resources/gold_file_output_java"
rm -rf $scalaDest $javaDest

bazel build $javaTarget
bazel build $scalaTarget

unzip $scalaJar -d scala-gen/
unzip $javaJar -d java-gen/
rm -rf scala-gen/META-INF/ java-gen/META-INF/

cp -r scala-gen $scalaDest
cp -r java-gen $javaDest

rm -rf scala-gen/ java-gen/
