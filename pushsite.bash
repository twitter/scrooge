#!/bin/bash

set -e

dir=/tmp/scrooge.$$
trap "rm -fr $dir" 0 1 2

echo 'making site...' 1>&2
./sbt scrooge-doc/make-site >/dev/null 2>&1

echo 'cloning...' 1>&2
git clone -b gh-pages git@github.com:twitter/scrooge.git $dir >/dev/null 2>&1

savedir=$(pwd)
cd $dir
git rm -fr .
touch .nojekyll
cp -r $savedir/doc/target/site/* .
git add -f .
git commit -am"site push by $(whoami)"
git push origin gh-pages:gh-pages
