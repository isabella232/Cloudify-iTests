Xvfb :114 -ac -noreset &
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:"/usr/lib/firefox-1.5.0.12"; 
export PATH="$PATH:/usr/lib/firefox-1.5.0.12/";
export DISPLAY=:114;
xterm &