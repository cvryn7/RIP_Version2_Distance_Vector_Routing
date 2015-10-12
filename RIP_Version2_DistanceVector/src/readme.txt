RouterMain.java is the main router program

compile and run the program with config file as command line parameter


This application assumes the following format for configuration file.

ADDRESS: 000.000.008.002
NEIGHBOR: 000.000.008.000
NEIGHBOR: 000.000.008.001
NETWORK: 150.000.000.002/24
NETWORK: 150.000.001.002/24

Their should be one space between words such as "ADDRESS:" and their corresponding addresses and their should be no space after
any address.

All routers will run on localhost 127.0.0.1

ADDRESS: field is port number written in ip address format 000.000.008.002 translate to 2050 when converted
to decimal
NEIGBHOR: field is same as address field but is address of neigbhor
NETWORK: field is CIDR address reachable directly from this router. 

IF YOU HAVE ANY PROBLEM UNDERSTAND ANY BLOCK OR FOUND SOME BUG, PLEASE CONTACT AT cv.ryn7@gmail.com