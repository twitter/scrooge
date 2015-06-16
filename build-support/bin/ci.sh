#!/bin/bash

function clone_repo {
  local name=$1
  local repo=$2
  local branch=$3
  git clone $repo || {
    echo "Unable to clone $name from $repo"
    #exit 1
  }
  cd $name || {
    echo "Unable to get $name from $repo"
    exit 2
  }
  git checkout $branch || {
    echo "Unable to checkout branch $branch for $name from $repo"
    exit 3
  }
  cd ..
}

function publish_local {
  local name=$1
  local target=$2
  cd $name || {
    echo "Unable to get $name for publish local $target"
    exit 4
  }
  ../../../sbt ++$TRAVIS_SCALA_VERSION $target || {
    echo "Unable to publish local $name with target $target"
    exit 5
  }
  cd ..
}

function bootstrap_scrooge {
  local current_branch=$(git rev-parse --abbrev-ref HEAD)
  if [ "$current_branch" != "master" ]; then
    mkdir -p build-support/tmp
    cd build-support/tmp
    # util
    clone_repo util https://github.com/twitter/util.git develop
    publish_local util publishLocal
    # ostrich
    clone_repo ostrich https://github.com/twitter/ostrich.git develop
    publish_local ostrich publishLocal
    # finagle
    clone_repo finagle https://github.com/twitter/finagle.git develop
    publish_local finagle finagle-core/publishLocal
    publish_local finagle finagle-mux/publishLocal
    publish_local finagle finagle-httpx/publishLocal
    publish_local finagle finagle-thrift/publishLocal
    publish_local finagle finagle-thriftmux/publishLocal
    publish_local finagle finagle-ostrich4/publishLocal
    cd ../..
  fi
}

bootstrap_scrooge

./sbt ++$TRAVIS_SCALA_VERSION test