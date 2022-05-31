#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

mkdir -p "$DIR/work"

cd work

# Update or checkout repositories
if [ ! -d "vscSnippetsConverter" ]; then
  git clone https://github.com/denofevil/vscSnippetsConverter.git vscSnippetsConverter
else
  cd vscSnippetsConverter
  git pull
  cd ..
fi

if [ ! -d "mpx-vscode-snippets" ]; then
  git clone https://github.com/sdras/mpx-vscode-snippets.git mpx-vscode-snippets
else
  cd mpx-vscode-snippets
  git reset --hard
  git pull
  cd ..
fi

rm -f "$DIR/work/mpx-vscode-snippets/snippets/nuxt-"*
rm -f "$DIR/work/mpx-vscode-snippets/snippets/ignore.json"
rm -f "$DIR/work/mpx-vscode-snippets/snippets/mpx-pug.json"

sed -i "" "s/3:styleObjectB]}/3:styleObjectB}]/g" "$DIR/work/mpx-vscode-snippets/snippets/mpx-template.json"

node vscSnippetsConverter/index.js "$DIR/work/mpx-vscode-snippets/snippets" "$DIR/liveTemplatesConfig.json" > "$DIR/gen/liveTemplates/Mpx.xml"
