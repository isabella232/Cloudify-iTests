#! /bin/sh

# shutdown the internal firewall.
sudo /usr/sbin/iptables -P INPUT ACCEPT
sudo /usr/sbin/iptables -P OUTPUT ACCEPT
sudo /usr/sbin/iptables -P FORWARD ACCEPT
sudo /usr/sbin/iptables -F