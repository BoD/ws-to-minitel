sudo stty -F /dev/ttyUSB0 1200 istrip cs7 parenb -parodd brkint ignpar icrnl ixon ixany opost onlcr cread hupcl isig icanon -echo echoe echok raw
sudo chmod 777 /dev/ttyUSB0
echo OK > /dev/ttyUSB0
