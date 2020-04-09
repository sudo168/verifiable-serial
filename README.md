# verifiable-serial
【百万级】100w/s 高性能, 推广码、兑换码、序列号生成器. 可验证，可追溯，不重复，不可推测.

电商系统必备工具.

对算法有问题欢迎咨询探讨，可定制开发！...2499325873

[demo] in main

int actId = 3;

int codeLength = 6;

String code = create(actId, codeLength);

System.out.println("code: " + code);// code: 6A396WW

System.out.println("verify: " + verify(code, actId > 0)); // verify: true

System.out.println("actId: " + getActId(code)); // actId: 3

