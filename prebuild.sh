cd server
if [ ! -d "./spectre7" ]; then
  git clone https://github.com/spectreseven1138/python-spectre7
  mv ./python-spectre7/spectre7 ./
  rm -rf ./python-spectre7
fi
